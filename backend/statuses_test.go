package main

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

func TestStatusesProxyCache(t *testing.T) {
	calls := 0
	s := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		calls++
		w.Write([]byte(`{"Success":true,"Data":[{"LineName":"M7","Description":"Test notice","UpdateDate":"2026-11-04","IsActive":true}]}`))
	}))
	defer s.Close()
	old := statusURL
	statusURL = s.URL
	defer func() { statusURL = old; statusData = nil }()
	statusData = nil
	statusAt = time.Time{}
	for i := 0; i < 2; i++ {
		w := httptest.NewRecorder()
		statuses(w, httptest.NewRequest("GET", "/api/v1/statuses", nil))
		if w.Code != 200 || !strings.Contains(w.Body.String(), "Test notice") || w.Header().Get("Cache-Control") != "no-store" {
			t.Fatal(w.Code, w.Body.String())
		}
	}
	if calls != 1 {
		t.Fatal(calls)
	}
}
func TestStatusesRejectsInvalid(t *testing.T) {
	s := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { w.Write([]byte(`{"Success":false}`)) }))
	defer s.Close()
	old := statusURL
	statusURL = s.URL
	defer func() { statusURL = old }()
	statusData = nil
	w := httptest.NewRecorder()
	statuses(w, httptest.NewRequest("GET", "/api/v1/statuses", nil))
	if w.Code != 502 {
		t.Fatal(w.Code)
	}
}
