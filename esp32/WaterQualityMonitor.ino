/*
 * =========================================================
 * Automatic Water Quality Monitoring System - ESP32 Firmware
 * =========================================================
 * Reads pH, TDS, Turbidity, and Temperature sensors, sends the
 * readings to the Spring Boot backend via REST (HTTP POST as JSON),
 * and activates a relay (simulating a water purification system)
 * based on the "relayStatus" field in the backend's response.
 *
 * Hardware:
 *  - ESP32 Dev Board
 *  - Analog pH sensor       -> GPIO 34 (ADC1_CH6)
 *  - Analog TDS sensor      -> GPIO 35 (ADC1_CH7)
 *  - Analog Turbidity sensor-> GPIO 32 (ADC1_CH4)
 *  - DS18B20 Temperature    -> GPIO 4  (OneWire, digital)
 *  - Relay module IN pin    -> GPIO 26 (digital output)
 *
 * Libraries required (install via Arduino IDE Library Manager):
 *  - WiFi.h            (bundled with ESP32 board package)
 *  - HTTPClient.h      (bundled with ESP32 board package)
 *  - ArduinoJson        by Benoit Blanchon
 *  - OneWire            by Jim Studt / Paul Stoffregen
 *  - DallasTemperature   by Miles Burton
 * =========================================================
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <OneWire.h>
#include <DallasTemperature.h>

// ---------------------- USER CONFIGURATION ----------------------
const char* WIFI_SSID     = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

// Replace with your machine's LAN IP address running the Spring Boot backend.
// Example: "http://192.168.1.10:8080"
const char* SERVER_BASE_URL = "http://192.168.1.10:8080";
const char* DEVICE_ID = "ESP32-01";

// How often to read sensors and send data (milliseconds)
const unsigned long SEND_INTERVAL_MS = 10000; // every 10 seconds

// ---------------------- PIN DEFINITIONS ----------------------
#define PH_SENSOR_PIN         34
#define TDS_SENSOR_PIN        35
#define TURBIDITY_SENSOR_PIN  32
#define ONE_WIRE_BUS_PIN      4
#define RELAY_PIN             26

// ---------------------- CALIBRATION CONSTANTS ----------------------
// These calibration values are indicative; calibrate against
// buffer solutions / known standards for accurate results.
const float PH_CALIBRATION_OFFSET = 0.0;
const float VREF = 3.3;           // ESP32 ADC reference voltage
const int ADC_RESOLUTION = 4095;  // 12-bit ADC

OneWire oneWire(ONE_WIRE_BUS_PIN);
DallasTemperature temperatureSensor(&oneWire);

unsigned long lastSendTime = 0;

void setup() {
  Serial.begin(115200);
  delay(1000);

  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, LOW); // relay OFF initially

  temperatureSensor.begin();

  connectToWiFi();
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    connectToWiFi();
  }

  unsigned long now = millis();
  if (now - lastSendTime >= SEND_INTERVAL_MS) {
    lastSendTime = now;

    float phValue = readPhSensor();
    float tdsValue = readTdsSensor();
    float turbidityValue = readTurbiditySensor();
    float temperatureValue = readTemperatureSensor();

    Serial.println("---- Sensor Readings ----");
    Serial.printf("pH: %.2f | TDS: %.2f ppm | Turbidity: %.2f NTU | Temp: %.2f C\n",
                  phValue, tdsValue, turbidityValue, temperatureValue);

    sendReadingToServer(phValue, tdsValue, turbidityValue, temperatureValue);
  }
}

void connectToWiFi() {
  Serial.print("Connecting to WiFi");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 30) {
    delay(500);
    Serial.print(".");
    attempts++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\nWiFi connected. IP address: " + WiFi.localIP().toString());
  } else {
    Serial.println("\nFailed to connect to WiFi. Will retry in loop().");
  }
}

// ---------------------- SENSOR READING FUNCTIONS ----------------------
// Replace these formulas with your sensor's specific calibration curve.

float readPhSensor() {
  int raw = analogRead(PH_SENSOR_PIN);
  float voltage = raw * (VREF / ADC_RESOLUTION);
  // Example linear formula: pH = 7 + ((2.5 - voltage) / 0.18)
  float ph = 7.0 + ((2.5 - voltage) / 0.18) + PH_CALIBRATION_OFFSET;
  return constrain(ph, 0.0, 14.0);
}

float readTdsSensor() {
  int raw = analogRead(TDS_SENSOR_PIN);
  float voltage = raw * (VREF / ADC_RESOLUTION);
  // Example simplified TDS formula (temperature-compensated versions exist)
  float tds = (133.42 * voltage * voltage * voltage
               - 255.86 * voltage * voltage
               + 857.39 * voltage) * 0.5;
  return max(tds, 0.0f);
}

float readTurbiditySensor() {
  int raw = analogRead(TURBIDITY_SENSOR_PIN);
  float voltage = raw * (VREF / ADC_RESOLUTION);
  // Example formula: lower voltage => higher turbidity (sensor-specific)
  float turbidity = -1120.4 * voltage * voltage + 5742.3 * voltage - 4352.9;
  return max(turbidity, 0.0f);
}

float readTemperatureSensor() {
  temperatureSensor.requestTemperatures();
  float tempC = temperatureSensor.getTempCByIndex(0);
  if (tempC == DEVICE_DISCONNECTED_C) {
    Serial.println("Warning: Temperature sensor disconnected. Defaulting to 25.0 C");
    return 25.0;
  }
  return tempC;
}

// ---------------------- NETWORKING ----------------------

void sendReadingToServer(float ph, float tds, float turbidity, float temperature) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi not connected. Skipping send.");
    return;
  }

  HTTPClient http;
  String url = String(SERVER_BASE_URL) + "/api/v1/sensor-data";
  http.begin(url);
  http.addHeader("Content-Type", "application/json");

  StaticJsonDocument<256> doc;
  doc["deviceId"] = DEVICE_ID;
  doc["phValue"] = ph;
  doc["tdsValue"] = tds;
  doc["turbidityValue"] = turbidity;
  doc["temperatureValue"] = temperature;

  String requestBody;
  serializeJson(doc, requestBody);

  int httpResponseCode = http.POST(requestBody);

  if (httpResponseCode > 0) {
    String response = http.getString();
    Serial.printf("HTTP Response code: %d\n", httpResponseCode);
    Serial.println("Response: " + response);
    applyRelayStateFromResponse(response);
  } else {
    Serial.printf("HTTP POST failed. Error: %s\n", http.errorToString(httpResponseCode).c_str());
  }

  http.end();
}

void applyRelayStateFromResponse(const String& jsonResponse) {
  StaticJsonDocument<512> responseDoc;
  DeserializationError error = deserializeJson(responseDoc, jsonResponse);

  if (error) {
    Serial.print("Failed to parse server response: ");
    Serial.println(error.c_str());
    return;
  }

  const char* relayStatus = responseDoc["data"]["relayStatus"];
  if (relayStatus == nullptr) {
    Serial.println("relayStatus field missing in response.");
    return;
  }

  if (strcmp(relayStatus, "ON") == 0) {
    digitalWrite(RELAY_PIN, HIGH);
    Serial.println(">>> RELAY ACTIVATED (purification system ON) <<<");
  } else {
    digitalWrite(RELAY_PIN, LOW);
    Serial.println(">>> Relay OFF (water quality safe) <<<");
  }
}
