# AquaWatch — API Documentation

Base URL (local development): `http://localhost:8080/api/v1`

All responses follow this envelope:

```json
{
  "success": true,
  "message": "Human-readable description",
  "data": { "...": "..." },
  "timestamp": "2026-07-08T10:15:30"
}
```

On errors, `success` is `false`, `data` is `null`, and `message` explains what went wrong. HTTP status codes are used appropriately (`400` validation errors, `404` not found, `500` unexpected errors).

---

## 1. Submit a Sensor Reading

Used by the **ESP32** to push a new reading.

`POST /sensor-data`

### Request Body

```json
{
  "deviceId": "ESP32-01",
  "phValue": 7.10,
  "tdsValue": 210.5,
  "turbidityValue": 1.8,
  "temperatureValue": 24.5
}
```

| Field              | Type   | Required | Notes                          |
|--------------------|--------|----------|---------------------------------|
| deviceId           | string | yes      | Identifier of the ESP32 unit    |
| phValue            | number | yes      | 0–14                            |
| tdsValue           | number | yes      | ppm, 0–5000                     |
| turbidityValue     | number | yes      | NTU, 0–3000                     |
| temperatureValue   | number | yes      | °C, -10–100                     |

### Success Response — `201 Created`

```json
{
  "success": true,
  "message": "Reading recorded. Water quality is SAFE.",
  "data": {
    "id": 101,
    "deviceId": "ESP32-01",
    "phValue": 7.10,
    "tdsValue": 210.5,
    "turbidityValue": 1.8,
    "temperatureValue": 24.5,
    "status": "SAFE",
    "relayStatus": "OFF",
    "recordedAt": "2026-07-08T10:15:30",
    "remarks": "All parameters within safe range."
  },
  "timestamp": "2026-07-08T10:15:30"
}
```

> The ESP32 firmware reads `data.relayStatus` (`"ON"` or `"OFF"`) from this response to decide whether to energize the purification relay.

### Error Response — `400 Bad Request`

Returned when a value is outside the physically valid range (e.g. pH > 14) or a required field is missing.

---

## 2. Get Latest Reading

`GET /sensor-data/latest`

Returns the single most recent reading — used to populate the dashboard's live gauges.

### Response — `200 OK`

Same shape as the `data` object above.

---

## 3. Get Historical Readings (Paginated)

`GET /sensor-data/history?page=0&size=20`

| Query Param | Default | Notes                       |
|-------------|---------|------------------------------|
| page        | 0       | Zero-indexed page number     |
| size        | 20      | Number of records per page   |

### Response — `200 OK`

```json
{
  "success": true,
  "message": "History fetched.",
  "data": {
    "content": [ /* array of readings, most recent first */ ],
    "number": 0,
    "totalPages": 5,
    "totalElements": 93,
    "first": true,
    "last": false
  }
}
```

---

## 4. Get Trend Data

`GET /sensor-data/trend`

Returns the last 50 readings in chronological order (oldest → newest), ready to feed directly into the dashboard's line chart.

---

## 5. Get Dashboard Statistics

`GET /sensor-data/stats`

Returns aggregate figures computed over the last 24 hours.

```json
{
  "success": true,
  "message": "Stats fetched.",
  "data": {
    "totalReadings": 42,
    "safeReadingsCount": 38,
    "unsafeReadingsCount": 4,
    "safePercentage": 90.48,
    "avgPh": 7.02,
    "avgTds": 245.3,
    "avgTurbidity": 2.4,
    "avgTemperature": 25.1
  }
}
```

---

## 6. Get Current Relay Status

`GET /sensor-data/relay-status`

A lightweight endpoint the ESP32 (or any client) can poll independently of submitting new data.

```json
{
  "success": true,
  "message": "Relay status fetched.",
  "data": { "relayStatus": "OFF" }
}
```

---

## 7. Health Check

`GET /health`

```json
{ "success": true, "message": "Service is up.", "data": { "status": "UP" } }
```

---

## Business Rules

- **Safe thresholds:**
  - pH: 6.5 – 8.5
  - TDS: 50 – 500 ppm
  - Turbidity: ≤ 5 NTU
  - Temperature: 10 – 35 °C
- If **any** parameter falls outside its safe range, the overall `status` is `UNSAFE` and `relayStatus` becomes `ON` (purification simulated as active).
- Once **all** parameters return to their safe ranges on a subsequent reading, `status` becomes `SAFE` and `relayStatus` automatically reverts to `OFF`.
- Thresholds are centrally defined in `WaterQualityThresholds.java` and can be tuned to match local regulatory standards.
