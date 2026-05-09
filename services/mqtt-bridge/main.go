package main

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"os"
	"strings"

	"github.com/segmentio/kafka-go"
)

// webhookBody is what EMQX's rule engine POSTs:
//
//	SELECT username as project_id, json_decode(payload) as payload FROM "ioteca/#"
type webhookBody struct {
	ProjectID string          `json:"project_id"`
	Payload   json.RawMessage `json:"payload"`
}

func main() {
	secret  := mustEnv("EMQX_INTERNAL_SECRET")
	brokers := getEnv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
	topic   := getEnv("KAFKA_TOPIC", "raw.ingest.af-ke-1")
	port    := getEnv("PORT", "8090")

	writer := &kafka.Writer{
		Addr:                   kafka.TCP(strings.Split(brokers, ",")...),
		Topic:                  topic,
		Balancer:               &kafka.LeastBytes{},
		RequiredAcks:           kafka.RequireOne,
		AllowAutoTopicCreation: false,
	}
	defer writer.Close()

	mux := http.NewServeMux()

	mux.HandleFunc("POST /ingest", func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("X-EMQX-Secret") != secret {
			w.WriteHeader(http.StatusUnauthorized)
			return
		}

		var body webhookBody
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.ProjectID == "" {
			w.WriteHeader(http.StatusBadRequest)
			return
		}

		// Merge project_id into the payload so ingestion-service can resolve it
		// from the message body (its primary resolution path).
		var merged map[string]any
		if err := json.Unmarshal(body.Payload, &merged); err != nil {
			w.WriteHeader(http.StatusBadRequest)
			return
		}
		merged["project_id"] = body.ProjectID

		value, err := json.Marshal(merged)
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}

		if err := writer.WriteMessages(context.Background(), kafka.Message{
			Key:   []byte(body.ProjectID),
			Value: value,
		}); err != nil {
			log.Printf("kafka write error: %v", err)
			w.WriteHeader(http.StatusServiceUnavailable)
			return
		}

		w.WriteHeader(http.StatusAccepted)
	})

	mux.HandleFunc("GET /health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})

	log.Printf("mqtt-bridge listening on :%s → topic %s", port, topic)
	log.Fatal(http.ListenAndServe(":"+port, mux))
}

func mustEnv(key string) string {
	v := os.Getenv(key)
	if v == "" {
		log.Fatalf("required env var %s is not set", key)
	}
	return v
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
