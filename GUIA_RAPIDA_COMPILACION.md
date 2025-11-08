# ⚡ Guía rápida de compilación y despliegue

Esta guía resume los pasos esenciales para volver a compilar la app, flashear el ESP32 y dejar todo funcionando con el broker del profesor.

---

## 1. Preparar el entorno

1. **Windows/WSL**
   ```bash
   cd /mnt/c/Users/LaBendiChao/Desktop/MotorContorlApp
   dos2unix gradlew        # (una vez) evita el error 'sh\r'
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
   export PATH="$JAVA_HOME/bin:$PATH"
   ```
2. **Requisitos Android**
   - Android Studio con SDK 34
   - Dispositivo físico con Android 10+ (Bluetooth clásico)
3. **Arduino IDE**
   - Paquete ESP32 3.x
   - Librerías: `PubSubClient`, `ArduinoJson`

---

## 2. Compilar la aplicación Android

```bash
./gradlew clean assembleDebug        # empaqueta app completa
# o solo demo sin auth
./gradlew assembleDemo
```
El APK queda en `app/build/outputs/apk/<flavor>/`. Instálalo con `adb install -r ...apk`.

**Errores comunes**
- `JAVA_HOME is not set` → define la variable antes de ejecutar Gradle.
- `Permission denied` → asegúrate de que `gradlew` tenga permisos (`chmod +x gradlew`).
- Kotlin “Unresolved reference” → sincroniza el proyecto (los archivos `ESP32Models.kt`, `NetworkConfigManagerUpdated.kt`, etc. ya están incluidos).

---

## 3. Flashear el ESP32

1. Conecta la tarjeta y selecciona el puerto COM correcto.
2. Abre `ESP32_Motor_Controller.ino` y pulsa *Upload*.
3. Verifica que en Serial Monitor aparezca la IP del AP y los logs de MQTT/Bluetooth.

Si la compilación falla por pines analógicos (`A0/A1`), asegúrate de estar usando la versión actual donde se definen como `GPIO36` y `GPIO39`.

---

## 4. Primer uso

1. **Modo Bluetooth**
   - Empareja el ESP32 (PIN `1234`).
   - En la app, ve a *Bluetooth Control* y pulsa “Buscar dispositivos”.
2. **Modo WiFi Setup**
   - Conéctate al AP `ESP32-MotorSetup` (`12345678`).
   - Sigue el asistente para enviar tu SSID/contraseña.
3. **Modo WiFi Local / MQTT Remote**
   - Después de configurar WiFi, simplemente pulsa “Connect MQTT”.

---

## 5. Validaciones rápidas

| Acción | Herramienta | Confirmación |
|--------|-------------|--------------|
| Arranque 6P | App + Serial Monitor | `Bluetooth ACK -> {"command":"arranque6p"}` |
| Paro | App | Botones de arranque se vuelven a habilitar. |
| MQTT | Logs app (`MqttService`) | `MQTT connection established successfully`. |
| HTTP | Navegador `http://192.168.4.1/status` | JSON con estado WiFi/Bluetooth. |

---

## 6. Problemas resueltos recientemente

- Eliminado conflicto de líneas CRLF en `gradlew` que impedía ejecutar comandos en WSL.
- Se añadieron los modelos (`ESP32Status`, `WiFiCredentials`) faltantes para compilar el módulo de integración.
- El botón “Desconectar” ahora cierra correctamente el socket Bluetooth y limpia el estado de la UI.
- La UI bloquea/desbloquea los botones de arranque según el modo reportado por el ESP32, evitando comandos duplicados.

---

## 7. Checklist antes de una demo

- [ ] ESP32 flasheado y conectado al broker del profesor.
- [ ] App instalada en el teléfono con permisos Bluetooth y ubicación concedidos.
- [ ] Prueba rápida: Arranque 6P → Paro → Arranque Continuo (en ambos modos, BT y WiFi).
- [ ] Capturas actualizadas en `docs/screenshots/` si hubo cambios visuales.

Con esto tienes todo listo para compilar y mostrar el proyecto sin contratiempos. 💪
