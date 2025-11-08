# MotorControlApp

Aplicación Android (Jetpack Compose + MVVM) y firmware ESP32 para controlar un motor de inducción con arranque suave, paro seguro y telemetría básica. El sistema opera en tres modos: **Bluetooth clásico**, **WiFi local con broker MQTT del profesor** y **MQTT remoto**. La app puede poner al ESP32 en modo configuración, enviar las credenciales WiFi y reconectar automáticamente al broker `177.247.175.4:1885`.

---

## Características principales

- **Modos de control combinados**: selección directa en `MotorControlScreen` entre Bluetooth, WiFi local, MQTT remoto y asistente de configuración WiFi.
- **Arranque 6 pasos seguro**: sliders para los seis valores PWM; los comandos se serializan como `arranque6p:39a,114b,...` con verificación en firmware.
- **Continuo y paro de emergencia**: los botones sólo se habilitan cuando el modo del ESP32 lo permite, evitando estados inconsistentes.
- **Auto-configuración WiFi**: la app escanea, envía SSID/contraseña y detecta el ESP32 después del reinicio usando `ESP32IntegrationHelper` + `NetworkConfigManagerUpdated`.
- **Bluetooth con ACK explícitos**: el firmware responde cada comando con JSON (`{"status":"ok","command":"0p"}`) y la app sincroniza estado, velocidad y modo.
- **MQTT unificado**: tópicos dinámicos basados en `device_name` (`motor/<device>/command`, `/speed`, `/raw`, etc.) para controlar y monitorear el motor.
- **Documentación integrada**: guías para compilación rápida, plan de implementación y soluciones a errores comunes.

---

## Arquitectura

```
Android App (Compose)
 ├─ viewmodel/     → MotorViewModel, WiFiSetupViewModel
 ├─ controllers/   → BluetoothMotorController, MqttMotorController
 ├─ services/      → BluetoothService, MqttService
 ├─ network/       → ESP32ConfigService, ESP32IntegrationHelper
 └─ ui/screens/    → MotorControlScreen, BluetoothControlScreen, WiFiSetupScreenReal

ESP32 Firmware
 └─ ESP32_Motor_Controller.ino
    ├─ HTTP server (scan/configure/status)
    ├─ MQTT client (broker 177.247.175.4:1885)
    └─ Control de motor + telemetría
```

---

## Protocolo de comunicación

### Bluetooth
- Comandos ASCII terminados en `\n`.
- Arranque suave: `arranque6p:39a,114b,188c,205d,227e,254f`.
- Continuo: `0i`.
- Paro: `0p`.
- El ESP32 responde con JSON: `{"status":"ok","command":"arranque6p"}`. La app usa la respuesta para habilitar/deshabilitar botones y actualizar el panel.

### MQTT
- Broker: `177.247.175.4:1885` (configurable vía `/configure`).
- Tópicos generados por `buildMqttTopics()`:
  - Comandos: `motor/<device>/command`
  - Velocidad deseada: `motor/<device>/speed/set`
  - Telemetría: `motor/<device>/{speed,state,current,voltage,raw,type}`
- El ESP32 publica cada segundo y la app actualiza los paneles de estado.

### HTTP (modo configuración)
- `GET /scan` → redes WiFi disponibles.
- `POST /configure` → `{ssid,password,mqtt_broker,mqtt_port,device_name}`.
- `GET /status` → estado completo (WiFi, MQTT, motor, Bluetooth).
- `POST /restart` → reinicio seguro tras guardar configuración.

---

## Construcción y despliegue

### Prerrequisitos
- JDK 17 (configurar `JAVA_HOME`).
- Android Studio Hedgehog+ o `./gradlew assembleDebug` (asegúrate de tener `gradlew` con LF).
- Arduino IDE 2.x con soporte ESP32 y librerías `ArduinoJson`, `PubSubClient`.

### Pasos
1. **Firmware**
   - Abrir `ESP32_Motor_Controller.ino`.
   - Ajustar pines sólo si tu driver lo requiere (por defecto GPIO2/4/5, sensores 36/39).
   - Cargar en el ESP32 y abrir Serial Monitor (115200) para verificar logs.
2. **Aplicación Android**
   - En Windows/WSL: `./gradlew clean assembleDebug`.
   - Instalar el APK en tu dispositivo (adb o QR).
3. **Configuración inicial**
   - Con la app en modo *WiFi Setup*, conéctate al AP `ESP32-MotorSetup` (`12345678`).
   - Escanea redes, elige tu WiFi y define el nombre del dispositivo (se usará en los tópicos MQTT).
   - Tras el reinicio del ESP32, selecciona *WiFi Local* o *Bluetooth* para controlar el motor.

---

## Estado del proyecto

| Área | Estado | Detalles |
|------|--------|----------|
| Firmware ESP32 | ✅ Estable | Modo AP + STA, control motor, MQTT/Bluetooth simultáneos con prioridad BT. |
| Auto-configuración WiFi | ✅ Estable | `WiFiSetupScreenReal` guía al usuario, guarda IP/MQTT en `SharedPreferences`. |
| Control Bluetooth | ✅ Estable | ACKs JSON, reconexión segura, botón “Desconectar” funcional. |
| UI Arranque/Paro | ✅ Estable | Botones habilitados según modo reportado, velocidad reseteada tras `0p`. |
| Documentación | ✅ Actualizada | Guías en `*.md` cubren compilación, plan y troubleshooting. |
| Próximo paso | ℹ️ Opcional | Añadir gráficas de corriente/voltaje y registro histórico via MQTT. |

---

## Cambios recientes relevantes

- Reestablecido protocolo `arranque6p:<valor+letra>` y validador `extractStepValue()` en el firmware.
- Nuevo búfer ASCII en `BluetoothMotorController` para procesar ACKs multi-paquete y limpieza del socket en `BluetoothService.close()`.
- `MotorViewModel` sincroniza `motorRunning`/velocidad según el modo real y `MotorControlScreen` desbloquea botones inmediatamente después de un paro.
- `ConnectionPanel` y `BluetoothControlScreen` usan `viewModel.disconnectDevice()` para cerrar sesión y limpiar la UI.
- Documentación revisada (este README, guías y plan) para reflejar el estado actual del proyecto.

---

## Capturas

Coloca tus capturas en `docs/screenshots/` y referencia aquí:

![Motor Control](docs/screenshots/motor_control.png)
![Bluetooth Control](docs/screenshots/bluetooth_control.png)

---

## Support

- Logs Android: `BluetoothMotorCtrl`, `MqttService`, `ESP32Integration`.
- Logs ESP32: Monitor serie 115200.
- Problemas comunes y soluciones rápidas en `SOLUCION_COMPILACION.md`.

¡El sistema está listo para demo y pruebas finales! 🚀
