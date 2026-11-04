package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestAnkaraAuthRequired(t *testing.T) {
	old := taksisToken
	taksisToken = func() string { return "" }
	defer func() { taksisToken = old }()
	w := httptest.NewRecorder()
	ankaraStops(w, httptest.NewRequest("GET", "/?type=station", nil))
	if w.Code != 503 || !strings.Contains(w.Body.String(), "authorization_required") {
		t.Fatal(w.Code, w.Body.String())
	}
}
func TestAnkaraSafeProjection(t *testing.T) {
	s := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("Authorization") != "Bearer test-only" || r.URL.Path != "/api/app/istasyon" || r.URL.Query().Get("Ad") != "Kızılay" {
			t.Error("request mapping")
		}
		w.Write([]byte(`{"totalCount":1,"items":[{"id":"s1","ad":"Kızılay","no":"1","konum":"POINT(32.85 39.92)","creatorId":"private-audit","servisPersonelAdSoyad":"not-for-client"}]}`))
	}))
	defer s.Close()
	base, token := taksisBase, taksisToken
	taksisBase = s.URL
	taksisToken = func() string { return "test-only" }
	defer func() { taksisBase = base; taksisToken = token }()
	w := httptest.NewRecorder()
	ankaraStops(w, httptest.NewRequest("GET", "/?type=station&q=K%C4%B1z%C4%B1lay", nil))
	var d map[string]interface{}
	if w.Code != 200 || json.Unmarshal(w.Body.Bytes(), &d) != nil || strings.Contains(w.Body.String(), "private-audit") || strings.Contains(w.Body.String(), "test-only") {
		t.Fatal(w.Code, w.Body.String())
	}
}
func TestAnkaraRejectedToken(t *testing.T) {
	s := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { w.WriteHeader(401) }))
	defer s.Close()
	base, token := taksisBase, taksisToken
	taksisBase = s.URL
	taksisToken = func() string { return "test-only" }
	defer func() { taksisBase = base; taksisToken = token }()
	w := httptest.NewRecorder()
	ankaraStops(w, httptest.NewRequest("GET", "/", nil))
	if w.Code != 503 || !strings.Contains(w.Body.String(), "401") {
		t.Fatal(w.Code)
	}
}
