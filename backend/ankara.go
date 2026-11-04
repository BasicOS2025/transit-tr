package main

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"
)

var taksisBase = "https://taksisapi.ego.gov.tr"
var taksisToken = func() string { return os.Getenv("TAKSIS_BEARER_TOKEN") }

// Only the two read-only stop/station endpoints. Never expose staff/audit fields.
func ankaraStops(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store")
	if r.Method != "GET" {
		http.Error(w, `{"error":"method not allowed"}`, 405)
		return
	}
	kind := r.URL.Query().Get("type")
	if kind == "" {
		kind = "station"
	}
	if kind != "station" && kind != "stop" {
		http.Error(w, `{"error":"invalid type"}`, 400)
		return
	}
	page := 0
	var err error
	if value := r.URL.Query().Get("page"); value != "" {
		page, err = strconv.Atoi(value)
		if err != nil || page < 0 || page > 2000 {
			http.Error(w, `{"error":"invalid page"}`, 400)
			return
		}
	}
	q := strings.TrimSpace(r.URL.Query().Get("q"))
	if len(q) > 128 {
		http.Error(w, `{"error":"query too long"}`, 400)
		return
	}
	token := strings.TrimSpace(taksisToken())
	if token == "" {
		w.WriteHeader(503)
		json.NewEncoder(w).Encode(map[string]interface{}{"error": "authorization_required", "message": "TAKSİS OAuth erişimi gerekiyor. Sunucu yöneticisi EGO tarafından verilen yetkili erişim belirtecini yapılandırmalı.", "liveVehiclePositions": false})
		return
	}
	path := "/api/app/istasyon"
	if kind == "stop" {
		path = "/api/app/durak"
	}
	params := url.Values{"MaxResultCount": {"50"}, "SkipCount": {strconv.Itoa(page * 50)}}
	if q != "" {
		if kind == "station" {
			params.Set("Ad", q)
		} else {
			params.Set("No", q)
		}
	}
	req, err := http.NewRequestWithContext(r.Context(), "GET", taksisBase+path+"?"+params.Encode(), nil)
	if err != nil {
		http.Error(w, `{"error":"configuration error"}`, 500)
		return
	}
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("Accept", "application/json")
	resp, err := client.Do(req)
	if err != nil {
		http.Error(w, `{"error":"taksis unavailable"}`, 502)
		return
	}
	defer resp.Body.Close()
	if resp.StatusCode == 401 || resp.StatusCode == 403 {
		w.WriteHeader(503)
		json.NewEncoder(w).Encode(map[string]interface{}{"error": "authorization_required", "upstream_status": resp.StatusCode, "message": "TAKSİS erişimi reddetti; yetki veya belirteç süresini kontrol edin."})
		return
	}
	if resp.StatusCode != 200 {
		http.Error(w, fmt.Sprintf(`{"error":"taksis upstream error","upstream_status":%d}`, resp.StatusCode), 502)
		return
	}
	var raw struct {
		TotalCount int `json:"totalCount"`
		Items      []struct {
			ID       string `json:"id"`
			Name     string `json:"ad"`
			No       string `json:"no"`
			Address  string `json:"adres"`
			Location string `json:"konum"`
		} `json:"items"`
	}
	if json.NewDecoder(io.LimitReader(resp.Body, 2<<20)).Decode(&raw) != nil {
		http.Error(w, `{"error":"invalid taksis response"}`, 502)
		return
	}
	if len(raw.Items) > 50 {
		raw.Items = raw.Items[:50]
	}
	items := make([]map[string]string, 0, len(raw.Items))
	for _, x := range raw.Items {
		name := x.Name
		if name == "" {
			name = "Durak " + x.No
		}
		items = append(items, map[string]string{"id": x.ID, "name": name, "code": x.No, "address": x.Address, "locationText": x.Location})
	}
	json.NewEncoder(w).Encode(map[string]interface{}{"items": items, "total": raw.TotalCount, "page": page, "source": "EGO TAKSİS", "kind": kind, "liveVehiclePositions": false, "liveArrivals": false})
}
