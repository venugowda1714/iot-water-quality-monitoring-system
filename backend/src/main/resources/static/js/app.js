/* =========================================================
   AquaWatch Dashboard — Frontend logic
   Polls the Spring Boot backend REST API and renders live
   readings, water-quality status, relay state, trend chart,
   stats, and paginated history.
   ========================================================= */

// If the dashboard is served by the Spring Boot app itself
// (copied into src/main/resources/static), a relative path works.
// If opened as a standalone static file, point this at your backend.
const API_BASE_URL = window.location.origin.startsWith('http')
  ? `${window.location.origin}/api/v1`
  : 'http://localhost:8080/api/v1';

const REFRESH_INTERVAL_MS = 10000;
const HISTORY_PAGE_SIZE = 10;

let historyPage = 0;
let historyTotalPages = 1;
let trendChart = null;

// ---------------------- DOM references ----------------------
const connIndicator = document.getElementById('connIndicator');
const connText = document.getElementById('connText');
const clockEl = document.getElementById('clock');
const lastUpdatedEl = document.getElementById('lastUpdated');

const statusBanner = document.getElementById('statusBanner');
const statusWord = document.getElementById('statusWord');
const statusRemarks = document.getElementById('statusRemarks');
const relayIndicator = document.getElementById('relayIndicator');
const relayText = document.getElementById('relayText');
const relayCaption = document.getElementById('relayCaption');
const deviceIdLabel = document.getElementById('deviceIdLabel');

const GAUGES = {
  ph: { min: 6.5, max: 8.5, absMax: 14, valueEl: document.getElementById('phValue'), fillEl: document.getElementById('phFill') },
  tds: { min: 50, max: 500, absMax: 1000, valueEl: document.getElementById('tdsValue'), fillEl: document.getElementById('tdsFill') },
  turbidity: { min: 0, max: 5, absMax: 20, valueEl: document.getElementById('turbidityValue'), fillEl: document.getElementById('turbidityFill') },
  temperature: { min: 10, max: 35, absMax: 50, valueEl: document.getElementById('temperatureValue'), fillEl: document.getElementById('temperatureFill') }
};

// ---------------------- Clock ----------------------
function tickClock() {
  const now = new Date();
  clockEl.textContent = now.toLocaleTimeString();
}
setInterval(tickClock, 1000);
tickClock();

// ---------------------- Fetch helper ----------------------
async function apiGet(path) {
  const res = await fetch(`${API_BASE_URL}${path}`);
  if (!res.ok) throw new Error(`Request failed: ${res.status}`);
  const body = await res.json();
  if (!body.success) throw new Error(body.message || 'API returned failure');
  return body.data;
}

function setConnectionState(online) {
  connIndicator.classList.toggle('online', online);
  connIndicator.classList.toggle('offline', !online);
  connText.textContent = online ? 'LIVE' : 'OFFLINE';
}

// ---------------------- Gauge rendering ----------------------
function renderGauge(key, value) {
  const g = GAUGES[key];
  if (value === null || value === undefined) return;
  g.valueEl.textContent = Number(value).toFixed(2);

  const pct = Math.min(100, Math.max(4, (value / g.absMax) * 100));
  g.fillEl.style.width = `${pct}%`;

  const safe = key === 'turbidity'
    ? value <= g.max
    : (value >= g.min && value <= g.max);

  g.fillEl.classList.toggle('unsafe', !safe);
}

// ---------------------- Status banner + relay ----------------------
function renderLatest(latest) {
  deviceIdLabel.textContent = latest.deviceId;

  renderGauge('ph', latest.phValue);
  renderGauge('tds', latest.tdsValue);
  renderGauge('turbidity', latest.turbidityValue);
  renderGauge('temperature', latest.temperatureValue);

  const isSafe = latest.status === 'SAFE';
  statusBanner.dataset.state = isSafe ? 'safe' : 'unsafe';
  statusWord.textContent = isSafe ? 'SAFE' : 'UNSAFE';
  statusRemarks.textContent = latest.remarks || (isSafe
    ? 'All parameters are within safe drinking-water thresholds.'
    : 'One or more parameters have exceeded safe thresholds.');

  const relayOn = latest.relayStatus === 'ON';
  relayIndicator.dataset.relay = relayOn ? 'on' : 'off';
  relayText.textContent = relayOn ? 'ON' : 'OFF';
  relayCaption.textContent = relayOn
    ? 'Purification system engaged'
    : 'Standing by — water is safe';

  const recordedAt = new Date(latest.recordedAt);
  lastUpdatedEl.textContent = `Last updated: ${recordedAt.toLocaleString()}`;
}

// ---------------------- Stats strip ----------------------
function renderStats(stats) {
  document.getElementById('statTotal').textContent = stats.totalReadings;
  document.getElementById('statSafePct').textContent = `${stats.safePercentage}%`;
  document.getElementById('statAvgPh').textContent = stats.avgPh != null ? stats.avgPh.toFixed(2) : '--';
  document.getElementById('statAvgTds').textContent = stats.avgTds != null ? stats.avgTds.toFixed(1) : '--';
  document.getElementById('statAvgTurbidity').textContent = stats.avgTurbidity != null ? stats.avgTurbidity.toFixed(2) : '--';
  document.getElementById('statAvgTemp').textContent = stats.avgTemperature != null ? stats.avgTemperature.toFixed(1) : '--';
}

// ---------------------- Trend chart ----------------------
function renderTrendChart(readings) {
  const labels = readings.map(r => new Date(r.recordedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
  const ph = readings.map(r => r.phValue);
  const tds = readings.map(r => r.tdsValue);
  const turbidity = readings.map(r => r.turbidityValue);
  const temperature = readings.map(r => r.temperatureValue);

  const ctx = document.getElementById('trendChart').getContext('2d');

  const datasetStyle = (label, data, color, yAxisID) => ({
    label, data, borderColor: color, backgroundColor: color,
    borderWidth: 2, pointRadius: 0, tension: 0.35, yAxisID
  });

  const config = {
    type: 'line',
    data: {
      labels,
      datasets: [
        datasetStyle('pH', ph, '#35c7c0', 'y'),
        datasetStyle('TDS (ppm)', tds, '#f2a94a', 'y1'),
        datasetStyle('Turbidity (NTU)', turbidity, '#e4573d', 'y'),
        datasetStyle('Temp (°C)', temperature, '#7fa3a8', 'y')
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { labels: { color: '#e7f3f2', font: { family: 'JetBrains Mono', size: 11 } } }
      },
      scales: {
        x: { ticks: { color: '#7fa3a8', font: { family: 'JetBrains Mono', size: 10 } }, grid: { color: 'rgba(30,66,78,0.4)' } },
        y: { position: 'left', ticks: { color: '#7fa3a8', font: { size: 10 } }, grid: { color: 'rgba(30,66,78,0.4)' } },
        y1: { position: 'right', ticks: { color: '#7fa3a8', font: { size: 10 } }, grid: { drawOnChartArea: false } }
      }
    }
  };

  if (trendChart) {
    trendChart.data = config.data;
    trendChart.update();
  } else {
    trendChart = new Chart(ctx, config);
  }
}

// ---------------------- History table ----------------------
function renderHistoryTable(page) {
  const rows = page.content || [];
  const tbody = document.getElementById('historyBody');

  if (rows.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" class="empty-row">No readings recorded yet.</td></tr>';
    return;
  }

  tbody.innerHTML = rows.map(r => `
    <tr>
      <td>${new Date(r.recordedAt).toLocaleString()}</td>
      <td>${r.phValue.toFixed(2)}</td>
      <td>${r.tdsValue.toFixed(1)}</td>
      <td>${r.turbidityValue.toFixed(2)}</td>
      <td>${r.temperatureValue.toFixed(1)}</td>
      <td><span class="status-pill ${r.status === 'SAFE' ? 'safe' : 'unsafe'}">${r.status}</span></td>
      <td><span class="relay-pill ${r.relayStatus === 'ON' ? 'on' : ''}">${r.relayStatus}</span></td>
    </tr>
  `).join('');

  historyTotalPages = page.totalPages || 1;
  document.getElementById('pageIndicator').textContent = `Page ${page.number + 1} of ${historyTotalPages}`;
  document.getElementById('historyRange').textContent = `${page.totalElements} total readings`;
  document.getElementById('prevPage').disabled = page.first;
  document.getElementById('nextPage').disabled = page.last;
}

// ---------------------- Pagination controls ----------------------
document.getElementById('prevPage').addEventListener('click', () => {
  if (historyPage > 0) {
    historyPage--;
    loadHistory();
  }
});
document.getElementById('nextPage').addEventListener('click', () => {
  if (historyPage < historyTotalPages - 1) {
    historyPage++;
    loadHistory();
  }
});

async function loadHistory() {
  try {
    const page = await apiGet(`/sensor-data/history?page=${historyPage}&size=${HISTORY_PAGE_SIZE}`);
    renderHistoryTable(page);
  } catch (err) {
    console.error('Failed to load history:', err);
  }
}

// ---------------------- Main polling loop ----------------------
async function refreshDashboard() {
  try {
    const [latest, stats, trend] = await Promise.all([
      apiGet('/sensor-data/latest'),
      apiGet('/sensor-data/stats'),
      apiGet('/sensor-data/trend')
    ]);

    renderLatest(latest);
    renderStats(stats);
    renderTrendChart(trend);
    setConnectionState(true);
  } catch (err) {
    console.error('Dashboard refresh failed:', err);
    setConnectionState(false);
  }
}

async function init() {
  await refreshDashboard();
  await loadHistory();
  setInterval(refreshDashboard, REFRESH_INTERVAL_MS);
  setInterval(loadHistory, REFRESH_INTERVAL_MS);
}

init();
