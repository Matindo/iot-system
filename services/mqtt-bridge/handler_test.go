package main

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/segmentio/kafka-go"
)

// ── mock writer ──────────────────────────────────────────────────────────────

type mockWriter struct {
	captured []kafka.Message
	err      error
}

func (m *mockWriter) WriteMessages(_ context.Context, msgs ...kafka.Message) error {
	if m.err != nil {
		return m.err
	}
	m.captured = append(m.captured, msgs...)
	return nil
}

// ── helpers ──────────────────────────────────────────────────────────────────

const testSecret = "test-secret"

func newTestServer(w *mockWriter) *server {
	return &server{secret: testSecret, writer: w}
}

func ingestReq(secret, projectID string, payload any) *http.Request {
	body, _ := json.Marshal(map[string]any{
		"project_id": projectID,
		"payload":    payload,
	})
	r := httptest.NewRequest(http.MethodPost, "/ingest", bytes.NewReader(body))
	r.Header.Set("Content-Type", "application/json")
	if secret != "" {
		r.Header.Set("X-EMQX-Secret", secret)
	}
	return r
}

// ── health ────────────────────────────────────────────────────────────────────

func TestHealth(t *testing.T) {
	rec := httptest.NewRecorder()
	handleHealth(rec, httptest.NewRequest(http.MethodGet, "/health", nil))
	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", rec.Code)
	}
}

// ── auth ──────────────────────────────────────────────────────────────────────

func TestIngest_wrongSecret_returns401(t *testing.T) {
	s := newTestServer(&mockWriter{})
	rec := httptest.NewRecorder()
	s.handleIngest(rec, ingestReq("wrong-secret", "proj-1", map[string]any{"temp": 25}))
	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("expected 401, got %d", rec.Code)
	}
}

func TestIngest_missingSecret_returns401(t *testing.T) {
	s := newTestServer(&mockWriter{})
	rec := httptest.NewRecorder()
	s.handleIngest(rec, ingestReq("", "proj-1", map[string]any{"temp": 25}))
	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("expected 401, got %d", rec.Code)
	}
}

// ── body validation ───────────────────────────────────────────────────────────

func TestIngest_missingProjectID_returns400(t *testing.T) {
	s := newTestServer(&mockWriter{})
	body, _ := json.Marshal(map[string]any{"payload": map[string]any{"temp": 25}})
	r := httptest.NewRequest(http.MethodPost, "/ingest", bytes.NewReader(body))
	r.Header.Set("X-EMQX-Secret", testSecret)
	rec := httptest.NewRecorder()
	s.handleIngest(rec, r)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected 400, got %d", rec.Code)
	}
}

func TestIngest_invalidPayloadJSON_returns400(t *testing.T) {
	s := newTestServer(&mockWriter{})
	body, _ := json.Marshal(map[string]any{
		"project_id": "proj-1",
		"payload":    "not-a-json-object",
	})
	r := httptest.NewRequest(http.MethodPost, "/ingest", bytes.NewReader(body))
	r.Header.Set("X-EMQX-Secret", testSecret)
	rec := httptest.NewRecorder()
	s.handleIngest(rec, r)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected 400, got %d", rec.Code)
	}
}

// ── successful ingest ────────────────────────────────────────────────────────

func TestIngest_validRequest_returns202(t *testing.T) {
	w := &mockWriter{}
	s := newTestServer(w)
	rec := httptest.NewRecorder()
	s.handleIngest(rec, ingestReq(testSecret, "proj-abc", map[string]any{"temperature": 24.5}))
	if rec.Code != http.StatusAccepted {
		t.Fatalf("expected 202, got %d", rec.Code)
	}
}

func TestIngest_validRequest_publishesOneMessage(t *testing.T) {
	w := &mockWriter{}
	s := newTestServer(w)
	s.handleIngest(httptest.NewRecorder(),
		ingestReq(testSecret, "proj-abc", map[string]any{"temperature": 24.5}))
	if len(w.captured) != 1 {
		t.Fatalf("expected 1 kafka message, got %d", len(w.captured))
	}
}

func TestIngest_validRequest_keyIsProjectID(t *testing.T) {
	w := &mockWriter{}
	s := newTestServer(w)
	s.handleIngest(httptest.NewRecorder(),
		ingestReq(testSecret, "proj-xyz", map[string]any{"humidity": 65}))
	if string(w.captured[0].Key) != "proj-xyz" {
		t.Fatalf("expected key 'proj-xyz', got %q", w.captured[0].Key)
	}
}

func TestIngest_validRequest_mergesProjectIDIntoPayload(t *testing.T) {
	w := &mockWriter{}
	s := newTestServer(w)
	s.handleIngest(httptest.NewRecorder(),
		ingestReq(testSecret, "proj-merge", map[string]any{"val": 1}))

	var out map[string]any
	if err := json.Unmarshal(w.captured[0].Value, &out); err != nil {
		t.Fatalf("payload is not valid JSON: %v", err)
	}
	if out["project_id"] != "proj-merge" {
		t.Fatalf("project_id not merged into payload, got %v", out["project_id"])
	}
}

func TestIngest_validRequest_preservesOriginalMetrics(t *testing.T) {
	w := &mockWriter{}
	s := newTestServer(w)
	s.handleIngest(httptest.NewRecorder(),
		ingestReq(testSecret, "proj-1", map[string]any{"temperature": 22.5, "humidity": 80.0}))

	var out map[string]any
	json.Unmarshal(w.captured[0].Value, &out) //nolint:errcheck
	if out["temperature"] != 22.5 {
		t.Fatalf("temperature not preserved, got %v", out["temperature"])
	}
}

// ── kafka error ───────────────────────────────────────────────────────────────

func TestIngest_kafkaError_returns503(t *testing.T) {
	w := &mockWriter{err: errors.New("broker unavailable")}
	s := newTestServer(w)
	rec := httptest.NewRecorder()
	s.handleIngest(rec, ingestReq(testSecret, "proj-1", map[string]any{"temp": 25}))
	if rec.Code != http.StatusServiceUnavailable {
		t.Fatalf("expected 503, got %d", rec.Code)
	}
}
