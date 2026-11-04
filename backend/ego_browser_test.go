package main

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestEgoBrowserValidation(t *testing.T) {
	old := browserWorkerURL
	browserWorkerURL = func() string { return "" }
	defer func() { browserWorkerURL = old }()
	for _, q := range []string{"", "123", "../secret", "10001&url=evil"} {
		w := httptest.NewRecorder()
		egoArrivals(w, httptest.NewRequest("GET", "/?stop="+q, nil))
		if w.Code != 400 {
			t.Fatal(q, w.Code)
		}
	}
	w := httptest.NewRecorder()
	egoArrivals(w, httptest.NewRequest("GET", "/?stop=10001", nil))
	if w.Code != 503 {
		t.Fatal(w.Code)
	}
}
func TestEgoBrowserProxy(t *testing.T) {
	s := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/query" || r.URL.Query().Get("stop") != "10001" {
			t.Error("mapping")
		}
		w.Write([]byte(`{"stop":"10001","arrivals":[],"sourceTime":"04.11.2026 19:20:00"}`))
	}))
	defer s.Close()
	old := browserWorkerURL
	browserWorkerURL = func() string { return s.URL }
	defer func() { browserWorkerURL = old }()
	w := httptest.NewRecorder()
	egoArrivals(w, httptest.NewRequest("GET", "/?stop=10001", nil))
	if w.Code != 200 || w.Header().Get("Cache-Control") != "no-store" || !strings.Contains(w.Body.String(), "10001") {
		t.Fatal(w.Code, w.Body.String())
	}
}
