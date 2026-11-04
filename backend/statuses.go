package main

import (
	"encoding/json"
	"io"
	"net/http"
	"sync"
	"time"
)

var statusURL = "https://api.ibb.gov.tr/MetroIstanbul/api/MetroMobile/V2/GetServiceStatuses"
var statusMu sync.Mutex
var statusData []byte
var statusAt time.Time

func statuses(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store")
	if r.Method != "GET" {
		http.Error(w, `{"error":"method not allowed"}`, 405)
		return
	}
	statusMu.Lock()
	defer statusMu.Unlock()
	if len(statusData) > 0 && time.Since(statusAt) < time.Minute {
		w.Write(statusData)
		return
	}
	req, err := http.NewRequestWithContext(r.Context(), "GET", statusURL, nil)
	if err != nil {
		http.Error(w, `{"error":"invalid upstream configuration"}`, 500)
		return
	}
	req.Header.Set("User-Agent", "TransitTR/0.5")
	resp, err := client.Do(req)
	if err != nil {
		http.Error(w, `{"error":"official service unavailable"}`, 502)
		return
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		http.Error(w, `{"error":"official service error"}`, 502)
		return
	}
	var raw struct {
		Success bool
		Data    []struct {
			LineName    string
			Description string
			UpdateDate  string
			IsActive    bool
		}
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 524288)).Decode(&raw) != nil || !raw.Success {
		http.Error(w, `{"error":"invalid official response"}`, 502)
		return
	}
	data, err := json.Marshal(map[string]interface{}{"Success": true, "Data": raw.Data, "fetchedAt": time.Now().UTC().Format(time.RFC3339), "source": statusURL, "kind": "official_service_notices", "liveVehiclePositions": false})
	if err != nil {
		http.Error(w, `{"error":"encode failed"}`, 500)
		return
	}
	statusData = data
	statusAt = time.Now()
	w.Write(data)
}
