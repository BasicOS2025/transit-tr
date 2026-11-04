package main

import (
	"encoding/json"
	"io"
	"net/http"
	"net/url"
	"os"
	"regexp"
	"strings"
	"time"
)

var browserWorkerURL = func() string { return os.Getenv("EGO_BROWSER_URL") }
var browserHTTP = &http.Client{Timeout: 32 * time.Second}
var stopNumber = regexp.MustCompile(`^[0-9]{5}$`)

func egoArrivals(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store")
	if r.Method != "GET" {
		http.Error(w, `{"error":"method not allowed"}`, 405)
		return
	}
	for key := range r.URL.Query() {
		if key != "stop" {
			http.Error(w, `{"error":"invalid_parameter"}`, 400)
			return
		}
	}
	stop := r.URL.Query().Get("stop")
	if !stopNumber.MatchString(stop) {
		http.Error(w, `{"error":"invalid_stop","message":"5 haneli durak numarası girin."}`, 400)
		return
	}
	base := strings.TrimRight(browserWorkerURL(), "/")
	if base == "" {
		w.WriteHeader(503)
		json.NewEncoder(w).Encode(map[string]string{"error": "browser_worker_not_configured", "message": "EGO arka plan tarayıcısı bu sunucuda çalıştırılmamış."})
		return
	}
	req, err := http.NewRequestWithContext(r.Context(), "GET", base+"/query?stop="+url.QueryEscape(stop), nil)
	if err != nil {
		http.Error(w, `{"error":"worker configuration error"}`, 500)
		return
	}
	resp, err := browserHTTP.Do(req)
	if err != nil {
		http.Error(w, `{"error":"browser_worker_unavailable","message":"Arka plan tarayıcısına ulaşılamadı."}`, 502)
		return
	}
	defer resp.Body.Close()
	data, err := io.ReadAll(io.LimitReader(resp.Body, 65537))
	if err != nil || len(data) > 65536 || !json.Valid(data) {
		http.Error(w, `{"error":"invalid_browser_response"}`, 502)
		return
	}
	w.WriteHeader(resp.StatusCode)
	w.Write(data)
}
