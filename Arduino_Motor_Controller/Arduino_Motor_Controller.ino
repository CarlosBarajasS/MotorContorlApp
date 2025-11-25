/*
 * ======================================================================
 * CÓDIGO ARDUINO PARA MOTOR CONTROL APP - VERSIÓN SSR + BLUETOOTH
 * ======================================================================
 * Este código debe cargarse en tu Arduino para que funcione con la app Android
 * Incluye: Bluetooth Serial (HC-05/HC-06), control de motor vía SSR
 *
 * CONEXIÓN DE HARDWARE:
 * ---------------------
 * ARDUINO PIN 2 (SSR_SIGNAL_PIN) --> Pin de señal del SSR
 * ARDUINO GND                     --> GND del SSR
 *
 * BLUETOOTH HC-05/HC-06:
 * ARDUINO TX (Pin 1)  --> RX del módulo Bluetooth
 * ARDUINO RX (Pin 0)  --> TX del módulo Bluetooth
 * VCC                 --> 5V del Arduino
 * GND                 --> GND del Arduino
 *
 * El SSR (Solid State Relay) se encarga de controlar el motor CD:
 * - Señal HIGH (5V) = SSR activado = Motor encendido
 * - Señal LOW (0V)  = SSR desactivado = Motor apagado
 *
 * NOTA: Este código está adaptado para control ON/OFF solamente.
 *       No hay control PWM de velocidad variable, solo encendido/apagado.
 * ======================================================================
 */

// ======================================================================
// CONFIGURACIÓN
// ======================================================================

// Motor Control Pin - Arduino
#define SSR_SIGNAL_PIN 2  // Pin de señal al SSR (Digital Pin 2)

// Estado del motor
struct MotorState {
  bool running = false;
  int speed = 0;
  String mode = "stopped";
} motorState;

volatile bool rampInProgress = false;
volatile bool cancelRamp = false;

String bluetoothBuffer = "";

// ======================================================================
// SETUP
// ======================================================================

void setup() {
  // Inicializar comunicación Serial (Bluetooth)
  Serial.begin(9600); // HC-05/HC-06 típicamente usa 9600 baud
  Serial.println("Arduino Motor Controller - Bluetooth Ready");
  Serial.println("Device: ESP32-MotorController"); // Nombre compatible con la app

  // Configurar pin del SSR
  setupMotorPins();

  Serial.println("=== Setup Complete ===");
}

// ======================================================================
// LOOP PRINCIPAL
// ======================================================================

void loop() {
  // Leer comandos Bluetooth
  handleBluetooth();

  delay(10);
}

// ======================================================================
// CONFIGURACIÓN DEL MOTOR
// ======================================================================

void setupMotorPins() {
  pinMode(SSR_SIGNAL_PIN, OUTPUT);
  digitalWrite(SSR_SIGNAL_PIN, LOW); // SSR apagado por defecto

  Serial.println("SSR control pin configured (Pin 2)");
}

// ======================================================================
// BLUETOOTH - RECEPCIÓN DE COMANDOS
// ======================================================================

void handleBluetooth() {
  while (Serial.available()) {
    char c = Serial.read();

    if (c == '\n' || c == '\r') {
      if (bluetoothBuffer.length() > 0) {
        processBluetoothCommand(bluetoothBuffer);
        bluetoothBuffer = "";
      }
    } else {
      bluetoothBuffer += c;
    }
  }
}

void processBluetoothCommand(String command) {
  command.trim();
  if (command.length() == 0) return;

  Serial.print("Received: ");
  Serial.println(command);

  String lower = command;
  lower.toLowerCase();

  // Procesar comandos
  if (lower == "0p" || lower == "p" || lower == "paro" || lower == "stop") {
    stopMotor();
    sendBluetoothResponse("ok", "paro", "Motor stopped");
  }
  else if (lower == "0i" || lower == "i" || lower == "continuo") {
    startContinuousMode();
    sendBluetoothResponse("ok", "continuo", "Continuous mode");
  }
  else if (lower.startsWith("arranque6p:") || lower.startsWith("arranque6p")) {
    handleArranque6P(command);
    sendBluetoothResponse("ok", "arranque6p", "Soft start initiated");
  }
  else if (lower.startsWith("speed=") || lower.startsWith("pwm=")) {
    int value = command.substring(command.indexOf('=') + 1).toInt();
    setMotorSpeed(value);
    sendBluetoothResponse("ok", "speed", String(motorState.speed));
  }
  else if (lower == "start") {
    startMotor();
    sendBluetoothResponse("ok", "start", "Motor started");
  }
  else {
    sendBluetoothResponse("error", command, "unknown_command");
    Serial.println("Unknown command");
  }
}

void sendBluetoothResponse(const String& status, const String& command, const String& message) {
  String payload = "{\"status\":\"" + status + "\"";
  if (command.length()) {
    payload += ",\"command\":\"" + command + "\"";
  }
  if (message.length()) {
    payload += ",\"message\":\"" + message + "\"";
  }
  payload += ",\"controller\":\"bluetooth\"";
  payload += "}";

  Serial.println(payload);
}

// ======================================================================
// CONTROL DEL MOTOR - SSR
// ======================================================================

void stopMotor() {
  digitalWrite(SSR_SIGNAL_PIN, LOW); // Apagar SSR

  motorState.running = false;
  motorState.speed = 0;
  motorState.mode = "stopped";

  Serial.println("Motor stopped (SSR OFF)");
}

void startMotor() {
  cancelRamp = false;
  digitalWrite(SSR_SIGNAL_PIN, HIGH); // Encender SSR

  motorState.running = true;
  motorState.mode = "running";

  Serial.println("Motor started (SSR ON)");
}

void startContinuousMode() {
  cancelRamp = false;
  rampInProgress = false;

  // Encender SSR directamente para asegurar que queda encendido
  digitalWrite(SSR_SIGNAL_PIN, HIGH);
  motorState.running = true;
  motorState.speed = 255; // Velocidad máxima (indicativo)
  motorState.mode = "continuo";

  Serial.println("Continuous mode activated (SSR ON at full speed)");
}

void setMotorSpeed(int speed) {
  speed = constrain(speed, 0, 255);
  motorState.speed = speed;

  // Con SSR: cualquier velocidad > 0 = motor ON, velocidad 0 = motor OFF
  if (speed > 0) {
    // Velocidad > 0: Encender SSR (siempre, aunque ya esté encendido)
    if (!motorState.running) {
      digitalWrite(SSR_SIGNAL_PIN, HIGH);
      motorState.running = true;
      Serial.print("SSR ON (speed request: ");
      Serial.print(speed);
      Serial.println(")");
    }
  } else if (speed == 0 && !rampInProgress) {
    // Velocidad = 0: Apagar SSR (solo si no hay rampa en progreso)
    if (motorState.running) {
      digitalWrite(SSR_SIGNAL_PIN, LOW);
      motorState.running = false;
      Serial.println("SSR OFF");
    }
  }
}

// ======================================================================
// ARRANQUE SUAVE 6 PASOS
// ======================================================================

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

void handleArranque6P(String command) {
  // Formato: "arranque6p:10a,20b,30c,40d,50e,60f" o "arranque6p:10,20,30,40,50,60"
  int colonIndex = command.indexOf(':');
  if (colonIndex == -1) {
    Serial.println("Invalid arranque6p format");
    return;
  }

  String values = command.substring(colonIndex + 1);

  cancelRamp = false;
  rampInProgress = true;
  motorState.mode = "arranque6p";

  Serial.print("Starting 6-step soft start with SSR: ");
  Serial.println(values);

  // Procesar los valores de arranque
  int stepSpeeds[6] = {0};
  int stepCount = 0;
  String remaining = values;

  while (remaining.length() > 0 && stepCount < 6) {
    int commaIndex = remaining.indexOf(',');
    String token = (commaIndex == -1) ? remaining : remaining.substring(0, commaIndex);
    token.trim();

    int parsedValue = 0;
    if (!extractStepValue(token, parsedValue)) {
      Serial.print("Invalid step token: ");
      Serial.println(token);
      rampInProgress = false;
      return;
    }

    stepSpeeds[stepCount++] = parsedValue;

    if (commaIndex == -1) break;
    remaining = remaining.substring(commaIndex + 1);
    remaining.trim();
  }

  if (stepCount == 0) {
    Serial.println("No valid speed steps provided");
    rampInProgress = false;
    return;
  }

  // IMPORTANTE: Con SSR y motor CD real, el motor se ENCIENDE y SE MANTIENE ENCENDIDO
  // Los "pasos" son solo delays para simular arranque progresivo
  // NO se debe hacer ON/OFF rápido porque daña el motor y el SSR

  Serial.println("Turning SSR ON - Motor starting...");
  digitalWrite(SSR_SIGNAL_PIN, HIGH);  // Encender SSR UNA VEZ
  motorState.running = true;

  // Ejecutar los pasos como delays progresivos
  for (int step = 0; step < stepCount; step++) {
    if (cancelRamp) {
      Serial.println("Arranque cancelado");
      rampInProgress = false;
      cancelRamp = false;
      stopMotor();
      return;
    }

    int stepSpeed = constrain(stepSpeeds[step], 0, 255);
    motorState.speed = stepSpeed;

    Serial.print("Step ");
    Serial.print(step + 1);
    Serial.print("/");
    Serial.print(stepCount);
    Serial.print(": PWM=");
    Serial.print(stepSpeed);
    Serial.println(" (SSR remains ON)");

    // Calcular tiempo de espera proporcional al paso (0.5-2 segundos por paso)
    int stepDelay = map(stepSpeed, 0, 255, 500, 2000);

    // Mantener el paso durante el tiempo calculado
    delay(stepDelay);
  }

  // Al final, asegurar que el motor queda encendido
  digitalWrite(SSR_SIGNAL_PIN, HIGH);
  motorState.running = true;
  motorState.speed = 255;

  rampInProgress = false;
  Serial.println("6-step soft start completed - Motor at full speed (SSR ON)");
}
