# AquaWatch — Automatic Water Quality Monitoring System

An end-to-end IoT + Java full-stack project. An **ESP32** reads pH, TDS,
Turbidity, and Temperature sensors and posts readings to a **Spring Boot**
REST backend, which validates the data, stores it in **MySQL**, evaluates
water-quality thresholds, and instructs the ESP32 to switch a **relay**
(simulating a purification system) ON or OFF. A live **HTML/CSS/JS
dashboard** displays real-time readings, historical data, and trend charts.

---

## Tech Stack

| Layer        | Technology                                |
|--------------|--------------------------------------------|
| Firmware     | ESP32 (Arduino C++), analog + OneWire sensors |
| Backend      | Java 17, Spring Boot 3, Spring Data JPA, REST |
| Database     | MySQL 8                                     |
| Frontend     | HTML5, CSS3, vanilla JavaScript, Chart.js   |
| Build Tool   | Maven                                       |

---

## Project Structure

```
water-quality-monitoring-system/
├── backend/                         # Spring Boot application (open in IntelliJ IDEA)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/wqms/watermonitor/
│       │   ├── WaterMonitorApplication.java
│       │   ├── controller/          # REST controllers
│       │   ├── service/             # Business logic interfaces + impl
│       │   ├── repository/          # Spring Data JPA repositories
│       │   ├── entity/              # JPA entities
│       │   ├── dto/                 # Request/response DTOs
│       │   ├── exception/           # Custom exceptions + global handler
│       │   ├── enums/                # WaterQualityStatus, RelayStatus
│       │   ├── util/                # Threshold constants
│       │   └── config/              # CORS configuration
│       ├── main/resources/
│       │   ├── application.properties
│       │   └── static/              # Copy of the frontend (served at /)
│       └── test/java/...            # Unit tests
├── database/
│   ├── schema.sql                   # Table creation script
│   └── sample_data.sql              # Sample rows for quick testing
├── esp32/
│   └── WaterQualityMonitor.ino      # Arduino sketch (open in Arduino IDE)
├── frontend/                        # Standalone copy of the dashboard
│   ├── index.html
│   ├── css/style.css
│   └── js/app.js
├── docs/
│   └── API_DOCUMENTATION.md
├── .gitignore
└── README.md
```

---

## How It Works

1. The ESP32 reads all four sensors every 10 seconds and sends a JSON
   payload via `POST /api/v1/sensor-data`.
2. The Spring Boot backend validates the payload, compares each value
   against safe thresholds, computes an overall `SAFE`/`UNSAFE` status,
   and persists the reading to MySQL.
3. If the reading is `UNSAFE`, the backend responds with `relayStatus: "ON"`.
   The ESP32 reads this field and energizes the relay pin (simulating
   activation of a purification system). Once subsequent readings return
   to the safe range, the response switches back to `relayStatus: "OFF"`
   and the ESP32 de-energizes the relay automatically.
4. The web dashboard polls `/latest`, `/stats`, `/trend`, and `/history`
   every 10 seconds and updates live gauges, the SAFE/UNSAFE banner, the
   relay indicator, a trend chart, and a paginated history table.

---

## Prerequisites

- **Java 17** (JDK)
- **Maven 3.8+** (or use IntelliJ's bundled Maven)
- **MySQL 8** running locally
- **IntelliJ IDEA** (Community or Ultimate) for the backend
- **Arduino IDE 2.x** for the ESP32 firmware
- An ESP32 dev board + pH / TDS / Turbidity / DS18B20 temperature sensors + relay module (optional — sample data lets you test the dashboard without hardware)

---

## Running the Backend Locally

### 1. Create the database

Open MySQL and run:

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/sample_data.sql   # optional: adds sample rows
```

(Alternatively, `spring.jpa.hibernate.ddl-auto=update` will auto-create the
table on first run if you skip `schema.sql` — but the database itself,
`water_quality_db`, must exist, which `schema.sql` also handles via
`CREATE DATABASE IF NOT EXISTS`.)

### 2. Configure credentials

Edit `backend/src/main/resources/application.properties` and set your
MySQL username/password:

```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. Open in IntelliJ IDEA

- `File → Open` → select the `backend/` folder.
- Let IntelliJ import the Maven project (it will download dependencies automatically).
- Run `WaterMonitorApplication.java` (right-click → Run).
- The backend starts on `http://localhost:8080`.

Or from the command line:

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 4. Open the dashboard

Because the frontend is also copied into `src/main/resources/static`,
simply visit:

```
http://localhost:8080/
```

Or, to run the frontend independently (e.g. from the standalone
`frontend/` folder), open `frontend/index.html` directly in a browser —
`js/app.js` will automatically target `http://localhost:8080/api/v1`.

---

## Flashing the ESP32

1. Open `esp32/WaterQualityMonitor.ino` in **Arduino IDE 2.x**.
2. Install the **ESP32 board package** (Boards Manager) if not already installed.
3. Install these libraries via **Library Manager**:
   - `ArduinoJson` (Benoit Blanchon)
   - `OneWire`
   - `DallasTemperature`
4. Update the configuration section at the top of the sketch:
   ```cpp
   const char* WIFI_SSID     = "YOUR_WIFI_SSID";
   const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";
   const char* SERVER_BASE_URL = "http://<your-computer-LAN-IP>:8080";
   ```
5. Wire the sensors/relay per the pin comments at the top of the file.
6. Select your ESP32 board + COM port, then **Upload**.
7. Open the Serial Monitor (115200 baud) to watch readings and relay state.

> Your computer and the ESP32 must be on the **same Wi-Fi network**, and
> your firewall must allow inbound connections to port 8080.

---

## API Reference

See [`docs/API_DOCUMENTATION.md`](docs/API_DOCUMENTATION.md) for full
endpoint documentation, request/response examples, and the business
rules governing SAFE/UNSAFE classification and relay control.

Quick reference:

| Method | Endpoint                          | Purpose                          |
|--------|------------------------------------|-----------------------------------|
| POST   | `/api/v1/sensor-data`              | ESP32 submits a new reading        |
| GET    | `/api/v1/sensor-data/latest`       | Latest reading (dashboard)         |
| GET    | `/api/v1/sensor-data/history`      | Paginated history                  |
| GET    | `/api/v1/sensor-data/trend`        | Last 50 readings for charting      |
| GET    | `/api/v1/sensor-data/stats`        | 24h aggregate statistics           |
| GET    | `/api/v1/sensor-data/relay-status` | Current relay state                |
| GET    | `/api/v1/health`                   | Health check                       |

---

## Safe Thresholds (default)

| Parameter    | Safe Range        |
|--------------|-------------------|
| pH           | 6.5 – 8.5         |
| TDS          | 50 – 500 ppm      |
| Turbidity    | ≤ 5 NTU           |
| Temperature  | 10 – 35 °C        |

Adjust these in `backend/src/main/java/com/wqms/watermonitor/util/WaterQualityThresholds.java`.

---

## Running Tests

```bash
cd backend
mvn test
```

Includes unit tests validating the SAFE/UNSAFE classification and
relay ON/OFF business logic (`SensorDataServiceTest`).

---

## Git

This project is already initialized as a Git repository with clean,
incremental commits. To push it to your own GitHub repository:

```bash
git remote add origin https://github.com/<your-username>/<your-repo>.git
git branch -M main
git push -u origin main
```

---

## License

This project is provided as-is for educational and portfolio purposes.
