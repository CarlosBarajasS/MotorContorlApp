// ESP32 Motor Controller - SSR Version
// GPIO2 --> SSR Signal Pin

#include <WiFi.h>
#include <WebServer.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <EEPROM.h>
#include <ctype.h>
#if defined(CONFIG_BT_ENABLED) && defined(CONFIG_BLUEDROID_ENABLED)
#include "BluetoothSerial.h"
#include <esp_gap_bt_api.h>
#endif

#define AP_SSID "ESP32-MotorSetup"
#define AP_PASSWORD "12345678"

#define SSR_SIGNAL_PIN 2
#define CURRENT_SENSOR_PIN 36
#define VOLTAGE_SENSOR_PIN 39

// EEPROM Addresses
#define EEPROM_SIZE 512
#define WIFI_SSID_ADDR 0
#define WIFI_PASS_ADDR 64
#define MQTT_BROKER_ADDR 128
#define MQTT_PORT_ADDR 192
#define DEVICE_NAME_ADDR 256

// Defaults - Broker del profesor
String mqtt_broker = "177.247.175.4";
int mqtt_port = 1885;
String device_name = "MotorController";

String mqttTopicCommand;
String mqttTopicSpeed;
String mqttTopicSpeedCommand;
String mqttTopicState;
String mqttTopicCurrent;
String mqttTopicVoltage;
String mqttTopicRaw;
String mqttTopicType;

enum ControlChannel {
  CONTROL_WIFI = 0,
  CONTROL_BLUETOOTH = 1
};

ControlChannel activeControl = CONTROL_WIFI;

#if defined(CONFIG_BT_ENABLED) && defined(CONFIG_BLUEDROID_ENABLED)
void handleBluetooth();
#endif

// Tiempos de conexión WiFi
const uint32_t WIFI_CONNECT_TIMEOUT_MS = 30000;
const uint32_t WIFI_RETRY_INTERVAL_MS = 500;

// ======================================================================
// OBJETOS GLOBALES
// ======================================================================

WebServer server(80);
WiFiClient espClient;
PubSubClient mqtt(espClient);

// Estado del motor
struct MotorState {
  bool running = false;
  int speed = 0;
  float current = 0.0;
  float voltage = 0.0;
  String mode = "stopped";
  String lastCommand = "";
} motorState;

volatile bool rampInProgress = false;
volatile bool cancelRamp = false;

// Configuración WiFi
struct WiFiConfig {
  String ssid = "";
  String password = "";
  bool configured = false;
} wifiConfig;

#if defined(CONFIG_BT_ENABLED) && defined(CONFIG_BLUEDROID_ENABLED)
BluetoothSerial btSerial;
bool bluetoothReady = false;
bool bluetoothClientConnected = false;
String bluetoothBuffer = "";
unsigned long lastBluetoothActivity = 0;
const unsigned long BLUETOOTH_PRIORITY_TIMEOUT_MS = 60000;

void sendBluetoothResponse(const String& status, const String& command, const String& message = "") {
  if (!bluetoothReady) return;
  String payload = "{\"status\":\"" + status + "\"";
  if (command.length()) {
    payload += ",\"command\":\"" + command + "\"";
  }
  if (message.length()) {
    payload += ",\"message\":\"" + message + "\"";
  }
  payload += ",\"controller\":\"" + String(activeControl == CONTROL_BLUETOOTH ? "bluetooth" : "wifi") + "\"";
  payload += "}";
  btSerial.println(payload);
  Serial.println("Bluetooth ACK -> " + payload);
}
#endif

// ======================================================================
// SETUP
// ======================================================================

void buildMqttTopics() {
  if (device_name.length() == 0) {
    device_name = "MotorController";
  }
  device_name.trim();
  device_name.replace(" ", "-");
  String base = "motor/" + device_name;
  mqttTopicCommand = base + "/command";
  mqttTopicSpeed = base + "/speed";
  mqttTopicSpeedCommand = base + "/speed/set";
  mqttTopicState = base + "/state";
  mqttTopicCurrent = base + "/current";
  mqttTopicVoltage = base + "/voltage";
  mqttTopicRaw = base + "/raw";
  mqttTopicType = base + "/type";
  Serial.println("MQTT topics configured for device: " + base);
}

void setup() {
  Serial.begin(115200);
  Serial.println("\n=== ESP32 Motor Controller Starting ===");
  
  // Inicializar EEPROM
  EEPROM.begin(EEPROM_SIZE);
  
  // Configurar pines del motor
  setupMotorPins();
  
  // Cargar configuración guardada
  loadConfiguration();
  buildMqttTopics();
  
  // Intentar conectar a WiFi guardado
  if (wifiConfig.configured) {
    Serial.println("Attempting to connect to saved WiFi...");
    connectToWiFi();
  } else {
    Serial.println("No WiFi config found, starting setup mode...");
    startConfigurationMode();
  }
  
  // Configurar servidor web
  setupWebServer();

#if defined(CONFIG_BT_ENABLED) && defined(CONFIG_BLUEDROID_ENABLED)
  // Usar emparejamiento clásico con PIN fijo para evitar códigos aleatorios
  esp_bt_pin_code_t pinCode = {'1', '2', '3', '4'};
  esp_bt_gap_set_pin(ESP_BT_PIN_TYPE_FIXED, 4, pinCode);
  btSerial.setPin("1234", 4);
  if (btSerial.begin("ESP32-MotorController")) {
    bluetoothReady = true;
    Serial.println("Bluetooth SPP ready (device name: ESP32-MotorController)");
  } else {
    Serial.println("Failed to start Bluetooth SPP service");
  }
#else
  Serial.println("Bluetooth stack not enabled for this firmware build");
#endif
  
  Serial.println("=== Setup Complete ===");
}

// ======================================================================
// LOOP PRINCIPAL
// ======================================================================

void loop() {
  // DEBUG: Verificar cambios inesperados en el GPIO
  // Watchdog ya no es necesario con analogWrite() porque genera señal PWM continua

  server.handleClient();

#if defined(CONFIG_BT_ENABLED) && defined(CONFIG_BLUEDROID_ENABLED)
  if (bluetoothReady) {
    handleBluetooth();
  }
#endif

  if (WiFi.status() == WL_CONNECTED) {
    if (!mqtt.connected()) {
      reconnectMQTT();
    }
    mqtt.loop();
  }

  // Leer sensores
  readSensors();

  // Publicar telemetría cada 1 segundo
  static unsigned long lastTelemetry = 0;
  if (millis() - lastTelemetry > 1000) {
    publishTelemetry();
    lastTelemetry = millis();
  }

  delay(50);
}

// ======================================================================
// CONFIGURACIÓN DEL MOTOR
// ======================================================================

void setupMotorPins() {
  pinMode(SSR_SIGNAL_PIN, OUTPUT);
  analogWrite(SSR_SIGNAL_PIN, 0);
  Serial.println("SSR configured on GPIO2");
}

// ======================================================================
// CONFIGURACIÓN WIFI
// ======================================================================

void startConfigurationMode() {
  Serial.println("Starting WiFi configuration mode...");

  WiFi.mode(WIFI_AP_STA);
  WiFi.disconnect(true);
  delay(100);
  WiFi.softAP(AP_SSID, AP_PASSWORD);
  
  IPAddress IP = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(IP);
  
  Serial.println("Connect to WiFi: " + String(AP_SSID));
  Serial.println("Password: " + String(AP_PASSWORD));
  Serial.println("Then open: http://192.168.4.1");
}

void connectToWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.disconnect(true);
  delay(100);
  WiFi.setAutoReconnect(true);
  WiFi.begin(wifiConfig.ssid.c_str(), wifiConfig.password.c_str());
  
  Serial.print("Connecting to WiFi");
  unsigned long startAttemptTime = millis();
  while (WiFi.status() != WL_CONNECTED &&
         millis() - startAttemptTime < WIFI_CONNECT_TIMEOUT_MS) {
    delay(WIFI_RETRY_INTERVAL_MS);
    Serial.print(".");
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println();
    Serial.println("WiFi connected!");
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());
    buildMqttTopics();

    if (WiFi.getMode() != WIFI_STA) {
      WiFi.softAPdisconnect(true);
      WiFi.mode(WIFI_STA);
    }
    
    // Configurar MQTT
    mqtt.setServer(mqtt_broker.c_str(), mqtt_port);
    mqtt.setCallback(mqttCallback);
    
  } else {
    Serial.println();
    Serial.print("WiFi connection failed (status=");
    Serial.print(WiFi.status());
    Serial.println("), starting setup mode...");
    startConfigurationMode();
  }
}

// ======================================================================
// SERVIDOR WEB - ENDPOINTS
// ======================================================================

void setupWebServer() {
  // CORS headers para todas las respuestas
  server.onNotFound([](){
    server.sendHeader("Access-Control-Allow-Origin", "*");
    server.sendHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
    server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
    server.send(404, "text/plain", "Not Found");
  });
  
  // OPTIONS para CORS preflight
  server.on("/scan", HTTP_OPTIONS, sendCORS);
  server.on("/configure", HTTP_OPTIONS, sendCORS);
  server.on("/status", HTTP_OPTIONS, sendCORS);
  server.on("/restart", HTTP_OPTIONS, sendCORS);
  
  // Endpoints principales
  server.on("/scan", HTTP_GET, handleWiFiScan);
  server.on("/configure", HTTP_POST, handleWiFiConfig);
  server.on("/status", HTTP_GET, handleStatus);
  server.on("/restart", HTTP_POST, handleRestart);
  
  // Página web simple (opcional)
  server.on("/", HTTP_GET, handleRoot);
  
  server.begin();
  Serial.println("Web server started");
}

void sendCORS() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.sendHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
  server.sendHeader("Access-Control-Allow-Headers", "Content-Type");
  server.send(200, "text/plain", "");
}

void handleWiFiScan() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  
  Serial.println("Scanning WiFi networks...");
  int n = WiFi.scanNetworks();
  
  DynamicJsonDocument json(1024);
  JsonArray networks = json.createNestedArray("networks");
  
  for (int i = 0; i < n; i++) {
    JsonObject network = networks.createNestedObject();
    network["ssid"] = WiFi.SSID(i);
    network["rssi"] = WiFi.RSSI(i);
    network["security"] = (WiFi.encryptionType(i) == WIFI_AUTH_OPEN) ? "Open" : "WPA2";
  }
  
  String response;
  serializeJson(json, response);
  
  server.send(200, "application/json", response);
  Serial.println("WiFi scan complete, sent " + String(n) + " networks");
}

void handleWiFiConfig() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  
  if (server.hasArg("plain")) {
    String body = server.arg("plain");
    Serial.println("Received config: " + body);
    
    DynamicJsonDocument json(512);
    DeserializationError error = deserializeJson(json, body);
    if (error) {
      Serial.print("JSON parse error: ");
      Serial.println(error.f_str());
      server.send(400, "application/json",
                  "{\"status\":\"error\",\"message\":\"Invalid JSON payload\"}");
      return;
    }
    
    String ssid = json["ssid"] | "";
    String password = json["password"] | "";
    String mqtt_ip = json["mqtt_broker"] | mqtt_broker;
    int mqtt_p = json["mqtt_port"] | mqtt_port;
    String new_device_name = json["device_name"] | device_name;

    ssid.trim();
    password.trim();
    mqtt_ip.trim();
    new_device_name.trim();
    new_device_name.replace(" ", "-");
    
    if (ssid.length() > 0) {
      // Guardar configuración
      wifiConfig.ssid = ssid;
      wifiConfig.password = password;
      wifiConfig.configured = true;
      mqtt_broker = mqtt_ip;
      mqtt_port = mqtt_p;
      device_name = new_device_name;
      buildMqttTopics();
      
      saveConfiguration();
      
      server.send(200, "application/json", "{\"status\":\"success\",\"message\":\"Configuration saved\"}");
      
      Serial.println("Configuration saved, restarting in 2 seconds...");
      delay(2000);
      ESP.restart();
      
    } else {
      server.send(400, "application/json", "{\"status\":\"error\",\"message\":\"SSID required\"}");
    }
  } else {
    server.send(400, "application/json", "{\"status\":\"error\",\"message\":\"No data received\"}");
  }
}

void handleStatus() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  
  DynamicJsonDocument json(512);
  
  json["wifi_connected"] = (WiFi.status() == WL_CONNECTED);
  json["ssid"] = WiFi.SSID();
  json["ip_address"] = WiFi.localIP().toString();
  json["mqtt_connected"] = mqtt.connected();
  json["mqtt_broker"] = mqtt_broker;
  json["device_name"] = device_name;
  json["control_channel"] = (activeControl == CONTROL_WIFI) ? "wifi" : "bluetooth";
#if defined(CONFIG_BT_ENABLED) && defined(CONFIG_BLUEDROID_ENABLED)
  json["bluetooth_ready"] = bluetoothReady;
  json["bluetooth_client"] = bluetoothClientConnected;
#else
  json["bluetooth_ready"] = false;
  json["bluetooth_client"] = false;
#endif
  json["uptime"] = millis();
  json["free_heap"] = ESP.getFreeHeap();
  
  // Estado del motor
  json["motor_running"] = motorState.running;
  json["motor_speed"] = motorState.speed;
  json["motor_current"] = motorState.current;
  json["motor_voltage"] = motorState.voltage;
  json["motor_mode"] = motorState.mode;
  
  String response;
  serializeJson(json, response);
  
  server.send(200, "application/json", response);
}

void handleRestart() {
  server.sendHeader("Access-Control-Allow-Origin", "*");
  server.send(200, "application/json", "{\"status\":\"restarting\"}");
  
  Serial.println("Restart requested, rebooting...");
  delay(1000);
  ESP.restart();
}

void handleRoot() {
  String html = "<html><body>";
  html += "<h1>ESP32 Motor Controller</h1>";
  html += "<p>Status: " + String(WiFi.status() == WL_CONNECTED ? "Connected" : "Disconnected") + "</p>";
  html += "<p>IP: " + WiFi.localIP().toString() + "</p>";
  html += "<p>MQTT: " + String(mqtt.connected() ? "Connected" : "Disconnected") + "</p>";
  html += "<p>Motor: " + motorState.mode + "</p>";
  html += "</body></html>";
  
  server.send(200, "text/html", html);
}

// ======================================================================
// MQTT
// ======================================================================

void reconnectMQTT() {
  if (WiFi.status() != WL_CONNECTED) return;
  
  static unsigned long lastAttempt = 0;
  if (millis() - lastAttempt < 5000) return; // Reintentar cada 5 segundos
  
  lastAttempt = millis();
  
  Serial.print("Attempting MQTT connection...");
  
  String clientId = "ESP32MotorController-" + String(random(0xffff), HEX);
  
  if (mqtt.connect(clientId.c_str())) {
    Serial.println("connected");
    
    // Suscribirse a topics de comandos
    mqtt.subscribe(mqttTopicCommand.c_str());
    mqtt.subscribe(mqttTopicSpeedCommand.c_str());
    mqtt.subscribe(mqttTopicType.c_str());
    
    // Publicar estado inicial
    publishTelemetry();
    
  } else {
    Serial.print("failed, rc=");
    Serial.println(mqtt.state());
  }
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
  if (activeControl != CONTROL_WIFI) {
    Serial.println("MQTT command ignored: Bluetooth control active");
    return;
  }

  Serial.print("MQTT received [" + String(topic) + "]: ");

  if (String(topic) == mqttTopicCommand) {
    for (int i = 0; i < length; i++) {
      char c = (char)payload[i];
      Serial.print(c);
      processCharacter(c);
    }
    Serial.println();
  } else if (String(topic) == mqttTopicSpeedCommand) {
    String message = "";
    for (int i = 0; i < length; i++) {
      message += (char)payload[i];
    }
    motorState.lastCommand = message;
    if (rampInProgress) {
      Serial.println("Speed command ignored during ramp");
      return;
    }
    int speed = message.toInt();
    setMotorSpeed(speed);
  } else if (String(topic) == mqttTopicType) {
    String typeMessage = "";
    for (int i = 0; i < length; i++) {
      typeMessage += (char)payload[i];
    }
    motorState.mode = typeMessage;
  }
}

void publishTelemetry() {
  if (!mqtt.connected()) return;
  
  mqtt.publish(mqttTopicState.c_str(), motorState.running ? "running" : "stopped");
  mqtt.publish(mqttTopicSpeed.c_str(), String(motorState.speed).c_str());
  mqtt.publish(mqttTopicCurrent.c_str(), String(motorState.current).c_str());
  mqtt.publish(mqttTopicVoltage.c_str(), String(motorState.voltage).c_str());
  
  // Raw telemetry
  String raw = "{";
  raw += "\"speed\":" + String(motorState.speed) + ",";
  raw += "\"current\":" + String(motorState.current) + ",";
  raw += "\"voltage\":" + String(motorState.voltage) + ",";
  raw += "\"running\":" + String(motorState.running ? "true" : "false") + ",";
  raw += "\"mode\":\"" + motorState.mode + "\"";
  raw += "}";
  
  mqtt.publish(mqttTopicRaw.c_str(), raw.c_str());
}

// ======================================================================
// CONTROL DEL MOTOR
// ======================================================================

bool isArranquePayload(const String& payload) {
  if (payload.indexOf(':') >= 0) return false;
  if (payload.length() < 3) return false;
  
  int start = 0;
  int tokens = 0;
  while (start < payload.length()) {
    int commaIndex = payload.indexOf(',', start);
    String token = (commaIndex == -1) ? payload.substring(start) : payload.substring(start, commaIndex);
    token.trim();
    if (token.length() < 2) return false;
    
    char suffix = token.charAt(token.length() - 1);
    suffix = tolower(suffix);
    if (suffix < 'a' || suffix > 'f') return false;
    
    String valueStr = token.substring(0, token.length() - 1);
    if (valueStr.length() == 0) return false;
    for (unsigned int i = 0; i < valueStr.length(); i++) {
      if (!isDigit(valueStr.charAt(i))) {
        return false;
      }
    }
    
    tokens++;
    if (commaIndex == -1) break;
    start = commaIndex + 1;
  }
  return tokens > 0;
}

bool extractStepValue(String token, int& outValue) {
  token.trim();
  if (token.length() == 0) return false;

  char last = token.charAt(token.length() - 1);
  if (!isDigit(last)) {
    char lower = tolower(last);
    if (lower >= 'a' && lower <= 'f') {
      token.remove(token.length() - 1);
    } else {
      return false;
    }
  }

  if (token.length() == 0) return false;
  for (unsigned int i = 0; i < token.length(); i++) {
    if (!isDigit(token.charAt(i))) {
      return false;
    }
  }

  outValue = token.toInt();
  return true;
}

void processCharacter(char data) {
  static String buffer = "";

  if (data == ',') {
    if (buffer.length() > 1) {
      Serial.println("Command: " + buffer);

      int velocidad = buffer.toInt();

      if (velocidad >= 0 && velocidad < 255) {

        if (buffer.indexOf('p') > 0) {
          velocidad = constrain(velocidad, 0, 255);
          analogWrite(SSR_SIGNAL_PIN, 0);
          Serial.print("Velocidad = ");
          Serial.println(0);
          delay(15);
          motorState.running = false;
          motorState.speed = 0;
          Serial.println("PARO DE EMERGENCIA");
          publishTelemetry();
        }

        if (buffer.indexOf('i') > 0) {
          Serial.println("INCREMENTO CONTINUO");
          Serial.print("Velocidad = ");
          for (int j = 1; j <= 25; j++) {
            if (velocidad >= 0 && velocidad < 254) {
              analogWrite(SSR_SIGNAL_PIN, velocidad);
              delay(120);
            }
            velocidad = velocidad + 10;
          }
          Serial.println(velocidad);
          motorState.running = true;
          motorState.speed = velocidad;
          publishTelemetry();
        }

        if (buffer.indexOf('a') > 0) {
          velocidad = constrain(velocidad, 0, 255);
          analogWrite(SSR_SIGNAL_PIN, velocidad);
          Serial.print("Velocidad = ");
          Serial.println(velocidad);
          delay(15);
          motorState.speed = velocidad;
          motorState.running = true;
          delay(200);
        }

        if (buffer.indexOf('b') > 0) {
          velocidad = constrain(velocidad, 0, 255);
          analogWrite(SSR_SIGNAL_PIN, velocidad);
          Serial.print("Velocidad = ");
          Serial.println(velocidad);
          delay(15);
          motorState.speed = velocidad;
          delay(150);
        }

        if (buffer.indexOf('c') > 0) {
          velocidad = constrain(velocidad, 0, 255);
          analogWrite(SSR_SIGNAL_PIN, velocidad);
          Serial.print("Velocidad = ");
          Serial.println(velocidad);
          delay(15);
          motorState.speed = velocidad;
          delay(120);
        }

        if (buffer.indexOf('d') > 0) {
          velocidad = constrain(velocidad, 0, 255);
          analogWrite(SSR_SIGNAL_PIN, velocidad);
          Serial.print("Velocidad = ");
          Serial.println(velocidad);
          delay(15);
          motorState.speed = velocidad;
          delay(80);
        }

        if (buffer.indexOf('e') > 0) {
          velocidad = constrain(velocidad, 0, 255);
          analogWrite(SSR_SIGNAL_PIN, velocidad);
          Serial.print("Velocidad = ");
          Serial.println(velocidad);
          delay(15);
          motorState.speed = velocidad;
          delay(70);
        }

        if (buffer.indexOf('f') > 0) {
          velocidad = constrain(velocidad, 0, 255);
          analogWrite(SSR_SIGNAL_PIN, velocidad);
          Serial.print("Velocidad = ");
          Serial.println(velocidad);
          delay(15);
          motorState.speed = velocidad;
          motorState.running = true;
          publishTelemetry();
          delay(60);
        }
      }
      buffer = "";
    }
  } else {
    buffer += data;
  }
}

void setMotorSpeed(int speed) {
  speed = constrain(speed, 0, 255);
  analogWrite(SSR_SIGNAL_PIN, speed);
  motorState.speed = speed;
  motorState.running = (speed > 0);
  publishTelemetry();
}

// ======================================================================
// SENSORES
// ======================================================================

void readSensors() {
  // Leer corriente (ejemplo con sensor ACS712)
  int currentRaw = analogRead(CURRENT_SENSOR_PIN);
  motorState.current = (currentRaw - 512) * 5.0 / 1024.0 * 1000.0 / 66.0; // mA
  
  // Leer voltaje (divisor de voltaje)
  int voltageRaw = analogRead(VOLTAGE_SENSOR_PIN);
  motorState.voltage = voltageRaw * 5.0 / 1024.0 * 5.0; // Asumiendo divisor 5:1
}

// ======================================================================
// EEPROM - GUARDAR/CARGAR CONFIGURACIÓN
// ======================================================================

void saveConfiguration() {
  // Guardar SSID
  for (int i = 0; i < 64; i++) {
    EEPROM.write(WIFI_SSID_ADDR + i, i < wifiConfig.ssid.length() ? wifiConfig.ssid[i] : 0);
  }
  
  // Guardar Password
  for (int i = 0; i < 64; i++) {
    EEPROM.write(WIFI_PASS_ADDR + i, i < wifiConfig.password.length() ? wifiConfig.password[i] : 0);
  }
  
  // Guardar MQTT Broker
  for (int i = 0; i < 64; i++) {
    EEPROM.write(MQTT_BROKER_ADDR + i, i < mqtt_broker.length() ? mqtt_broker[i] : 0);
  }
  
  // Guardar MQTT Port
  EEPROM.write(MQTT_PORT_ADDR, mqtt_port & 0xFF);
  EEPROM.write(MQTT_PORT_ADDR + 1, (mqtt_port >> 8) & 0xFF);
  
  // Guardar Device Name
  for (int i = 0; i < 64; i++) {
    EEPROM.write(DEVICE_NAME_ADDR + i, i < device_name.length() ? device_name[i] : 0);
  }
  
  // Marca de configuración válida
  EEPROM.write(511, 0xAA);
  
  EEPROM.commit();
  Serial.println("Configuration saved to EEPROM");
}

void loadConfiguration() {
  // Verificar si hay configuración válida
  if (EEPROM.read(511) != 0xAA) {
    Serial.println("No valid configuration found");
    return;
  }
  
  // Cargar SSID
  char ssidBuf[65] = {0};
  for (int i = 0; i < 64; i++) {
    ssidBuf[i] = EEPROM.read(WIFI_SSID_ADDR + i);
  }
  wifiConfig.ssid = String(ssidBuf);
  
  // Cargar Password
  char passBuf[65] = {0};
  for (int i = 0; i < 64; i++) {
    passBuf[i] = EEPROM.read(WIFI_PASS_ADDR + i);
  }
  wifiConfig.password = String(passBuf);
  
  // Cargar MQTT Broker
  char mqttBuf[65] = {0};
  for (int i = 0; i < 64; i++) {
    mqttBuf[i] = EEPROM.read(MQTT_BROKER_ADDR + i);
  }
  mqtt_broker = String(mqttBuf);
  
  // Cargar MQTT Port
  mqtt_port = EEPROM.read(MQTT_PORT_ADDR) | (EEPROM.read(MQTT_PORT_ADDR + 1) << 8);
  
  // Cargar Device Name
  char deviceBuf[65] = {0};
  for (int i = 0; i < 64; i++) {
    deviceBuf[i] = EEPROM.read(DEVICE_NAME_ADDR + i);
  }
  device_name = String(deviceBuf);
  device_name.trim();
  device_name.replace(" ", "-");
  
  if (wifiConfig.ssid.length() > 0) {
    wifiConfig.configured = true;
    Serial.println("Configuration loaded from EEPROM");
    Serial.println("SSID: " + wifiConfig.ssid);
    Serial.println("MQTT: " + mqtt_broker + ":" + String(mqtt_port));
    Serial.println("Device ID: " + device_name);
  }
}

#if defined(CONFIG_BT_ENABLED) && defined(CONFIG_BLUEDROID_ENABLED)

void handleBluetooth() {
  if (!bluetoothReady) return;

  bool currentClient = btSerial.hasClient();
  if (currentClient && !bluetoothClientConnected) {
    bluetoothClientConnected = true;
    activeControl = CONTROL_BLUETOOTH;
    Serial.println("Bluetooth client connected - MQTT commands paused");
  } else if (!currentClient && bluetoothClientConnected) {
    bluetoothClientConnected = false;
    Serial.println("Bluetooth client disconnected");
    lastBluetoothActivity = millis();
  }

  while (currentClient && btSerial.available()) {
    char c = btSerial.read();
    processCharacter(c);
    if (c == '\n') {
      bluetoothBuffer = "";
    } else if (c != '\r') {
      if (bluetoothBuffer.length() < 256) {
        bluetoothBuffer += c;
      }
    }
  }

  if (activeControl == CONTROL_BLUETOOTH) {
    if (currentClient) {
      lastBluetoothActivity = millis();
    } else if ((millis() - lastBluetoothActivity) > BLUETOOTH_PRIORITY_TIMEOUT_MS) {
      activeControl = CONTROL_WIFI;
      Serial.println("Bluetooth inactive, returning to WiFi control");
    }
  }
}
#endif
