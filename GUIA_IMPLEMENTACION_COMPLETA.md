# 🚀 Guía completa de implementación ESP32 + Android

Este documento resume el flujo actual del proyecto: firmware `ESP32_Motor_Controller.ino`, aplicación Android `MotorControlApp` y la integración WiFi/MQTT/Bluetooth.

---

## 1. Requisitos

| Componente | Versión recomendada |
|------------|---------------------|
| Arduino IDE | 2.3+ con soporte ESP32 (core 3.x) |
| Librerías ESP32 | `WebServer`, `WiFi`, `EEPROM`, `PubSubClient`, `ArduinoJson 7.x` |
| Android Studio | Hedgehog/Koala con JDK 17 |
| Hardware | ESP32 DEVKIT, driver de motor, sensor de corriente/voltaje |
| Broker MQTT | `177.247.175.4:1885` (profesor) |

---

## 2. Firmware ESP32

1. Abre `ESP32_Motor_Controller.ino`.
2. Ajusta pines sólo si tu placa requiere cambios (por defecto GPIO2/4/5, ADC36/39).
3. Carga el sketch y observa en Serial Monitor:
   ```
   === ESP32 Motor Controller Starting ===
   AP SSID: ESP32-MotorSetup
   Web server started
   ```
4. El firmware habilita simultáneamente:
   - **Modo configuración** (AP + servidor HTTP) para `/scan`, `/configure`, `/status`.
   - **Modo operativo** (WiFi STA + MQTT) con tópicos `motor/<device>/...`.
   - **Bluetooth SPP** con PIN fijo `1234`; cada comando responde con JSON (`Bluetooth ACK -> {...}`).

### Comandos aceptados
- `arranque6p:<valora>,<valorb>,...` (hasta 6 pasos).
- `0i` → continuo.
- `0p` → paro.
- `speed=120` → PWM directo.

---

## 3. App Android

### Estructura clave

- `MotorViewModel.kt` coordina los modos y sincroniza el estado del motor.
- `WiFiSetupScreenReal.kt` guía al usuario durante la primera configuración.
- `BluetoothMotorController` / `MqttMotorController` implementan la interfaz `MotorController`.
- `ESP32ConfigService.kt` encapsula los endpoints HTTP.

### Flujo de conexión

```
MotorControlScreen
 ├─ Bluetooth: usa BluetoothService + ACKs JSON
 ├─ WiFi Local / MQTT Remote: usa MqttService (HiveMQ client)
 └─ WiFi Setup: lanza asistente que escanea redes y envía credenciales
```

### Reglas de UI
- Botones Arranque 6P y Continuo se habilitan cuando `motorMode` es `paro/stop/stopped`.
- Botón Paro sólo se habilita si el modo reporta ejecución.
- El panel “Conexión” muestra IP, modo, velocidad y permite desconectar/controlar según el modo seleccionado.

---

## 4. Configuración WiFi paso a paso

1. Enciende el ESP32 recién flasheado (modo AP `ESP32-MotorSetup`).
2. Abre la app → modo *WiFi Setup*.
3. Sigue el asistente:
   - Detecta si ya hay un ESP32 configurado en la red local.
   - Si no, se conecta automáticamente al AP y consulta `/status`.
   - Solicita SSID y contraseña de tu red doméstica (y opcionalmente `device_name`).
4. Tras enviar `/configure`, el ESP32 reinicia; la app espera 8–15 s y busca la nueva IP.
5. Si lo encuentra, guarda la IP/MQTT en `NetworkConfigManagerUpdated` y permite cambiar al modo *WiFi Local*.

---

## 5. Pruebas sugeridas

| Prueba | Herramienta | Resultado esperado |
|--------|-------------|--------------------|
| Arranque 6P por Bluetooth | App + Serial Monitor | `Processing motor command: arranque6p:...` y botones desactivados hasta `0p`. |
| Paro → nuevo arranque | App | Tras `Bluetooth ACK -> {"command":"0p"}` los botones de arranque vuelven a habilitarse. |
| MQTT Local | App (WiFi Local) + broker | Panel muestra IP/SSID, velocidad y modo actualizados cada segundo. |
| Desconectar Bluetooth | Panel o pantalla Bluetooth | Estado pasa a “Disconnected” y se liberan los botones. |
| WiFi Setup | WiFiSetupScreenReal | Flujo completo con escaneo, envío de credenciales y detección en red. |

---

## 6. Troubleshooting

| Problema | Causa probable | Solución |
|----------|----------------|----------|
| El APK no compila en WSL | `gradlew` con CRLF o sin `JAVA_HOME` | Ejecuta `dos2unix gradlew` y configura JDK 17 (`export JAVA_HOME=/usr/lib/jvm/...`). |
| No puedo desconectar BT | Socket seguía abierto | Actualiza a la versión actual (se cierra el socket antes de cancelar el lector). |
| Botones quedan bloqueados en “Paro” | ACK `0p` no llegaba a la app | Revisa que los comandos terminen en `\n` y que el log muestre `Bluetooth ACK -> ...`. |
| No aparece el ESP32 en WiFi Local | IP guardada desactualizada | Usa “Verificar ESP32” o ejecuta nuevamente el asistente WiFi. |
| MQTT sin conexión | Firewall o sin internet | Intenta con datos móviles o verifica que el puerto 1885 esté abierto. |

---

## 7. Próximos pasos opcionales

- Registrar corriente/voltaje en gráficos históricos utilizando los tópicos `motor/<device>/raw`.
- Añadir recordatorios de mantenimiento en la app (horas de uso, ciclos de arranque).
- Implementar OTA para el ESP32 aprovechando el servidor web ya levantado.

Con esta guía puedes desplegar todo el sistema desde cero y tener evidencia clara para tus avances académicos. Guarda logs y capturas en la carpeta `docs/` cuando hagas nuevas pruebas.
