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

// messageWriter is satisfied by *kafka.Writer and by the test mock.
type messageWriter interface {
	WriteMessages(ctx context.Context, msgs ...kafka.Message) error
}

type server struct {
	secret string
	writer messageWriter
}

func (s *server) handleIngest(w http.ResponseWriter, r *http.Request) {
	if r.Header.Get("X-EMQX-Secret") != s.secret {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}

	var body webhookBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.ProjectID == "" {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

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

	if err := s.writer.WriteMessages(context.Background(), kafka.Message{
		Key:   []byte(body.ProjectID),
		Value: value,
	}); err != nil {
		log.Printf("kafka write error: %v", err)
		w.WriteHeader(http.StatusServiceUnavailable)
		return
	}

	w.WriteHeader(http.StatusAccepted)
}

func handleHealth(w http.ResponseWriter, _ *http.Request) {
	w.WriteHeader(http.StatusOK)
}

func newMux(s *server) *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("POST /ingest", s.handleIngest)
	mux.HandleFunc("GET /health", handleHealth)
	return mux
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

	s := &server{secret: secret, writer: writer}

	log.Printf("mqtt-bridge listening on :%s → topic %s", port, topic)
	log.Fatal(http.ListenAndServe(":"+port, newMux(s)))
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
