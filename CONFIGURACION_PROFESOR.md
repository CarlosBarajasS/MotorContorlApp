# ⚙️ Configuración para el broker del profesor

El proyecto ya está cableado para operar contra el broker público del profesor **`177.247.175.4:1885`** en todos los modos. Usa este checklist para verificar la instalación completa.

---

## 1. Firmware ESP32

1. Abre `ESP32_Motor_Controller.ino` en Arduino IDE 2.x.
2. Selecciona *ESP32 Dev Module* y carga el sketch.
3. En el monitor serie (115200) deberías ver:
   ```
   === ESP32 Motor Controller Starting ===
   AP SSID: ESP32-MotorSetup
   MQTT broker: 177.247.175.4:1885
   Web server started
   ```
4. Si necesitas cambiar el broker en campo, edita `/configure` desde la app o la página `http://192.168.4.1`.

### Pines configurados

| Función | GPIO |
|---------|------|
| ENABLE  | 2    |
| DIR     | 4    |
| PWM     | 5    |
| Corriente | 36 |
| Voltaje  | 39 |

Los sensores y el driver deben alimentarse externamente (12/24 V según tu motor).

---

## 2. Modos disponibles en la app

| Modo | Uso | Requisitos |
|------|-----|------------|
| **Bluetooth** | Control directo del ESP32 vía SPP | Emparejar ESP32 desde ajustes del teléfono (PIN `1234`). |
| **WiFi Local** | Control vía MQTT cuando teléfono y ESP32 están en la misma red | Broker `177.247.175.4`, puerto `1885`. |
| **MQTT Remote** | Controlar desde cualquier red con internet | Igual que WiFi Local, sólo requiere datos móviles. |
| **WiFi Setup** | Asistente para enviar SSID/contraseña al ESP32 | Acércate al AP `ESP32-MotorSetup` (clave `12345678`). |

---

## 3. Flujo recomendado

1. **Bluetooth** (primeras pruebas)
   - Abre *Bluetooth Control* → *Buscar dispositivos* → selecciona tu ESP32.
   - Envía Arranque 6P, Continuo y Paro; confirma los `Bluetooth ACK -> {"status":...}` en Serial Monitor.
2. **Configurar WiFi**
   - Desde la app, modo *WiFi Setup* → escanea redes → envía credenciales.
   - El ESP32 se reinicia y se conecta al broker del profesor automáticamente.
3. **WiFi Local / MQTT Remote**
   - En la app selecciona el modo deseado → *Connect MQTT*.
   - Verifica en el panel “Conexión” que el dispositivo muestra IP, modo y velocidad.

---

## 4. Verificación rápida

| Paso | Comando | Resultado esperado |
|------|---------|--------------------|
| Ping HTTP | `curl http://192.168.4.1/status` | JSON con `wifi_connected=false` (modo AP). |
| Configurar WiFi | POST `/configure` con SSID/clave | ESP32 responde `{"status":"success"}` y reinicia. |
| MQTT | Monitor serie | `MQTT topics configured for device: motor/<device>` y `connected`. |
| Bluetooth | Serial Monitor | `[Bluetooth] Received: 0p` + `Bluetooth ACK -> {...}`. |

---

## 5. Resolución de problemas

- **No puedo desconectar Bluetooth** → usa el botón “Desconectar” (ahora cierra el socket y libera el recurso). Si queda colgado, apaga y enciende Bluetooth en el celular y vuelve a emparejar.
- **Botones de arranque siguen bloqueados** → asegúrate de recibir `command":"0p"` en el log. La UI se desbloquea apenas llega ese ACK; si no aparece, revisa que haya `\n` al final de cada comando enviado.
- **No conecta a MQTT** → confirma que el teléfono tenga internet y que la red permita salir al puerto 1885. Si estás en una red del campus con proxy, usa datos móviles.
- **Broker diferente** → En modo AP, `POST /configure` con tu IP/puerto personalizados y un `device_name` sin espacios.

---

## 6. Estado actual

- ✅ Firmware con WiFi + Bluetooth simultáneos.
- ✅ Broker del profesor configurado por defecto.
- ✅ App verifica estado, modo y velocidad vía MQTT y Bluetooth.
- ✅ Botón “Desconectar” funcional en todas las pantallas.
- 📌 Próximo paso (opcional): añadir logs de corriente/voltaje al panel para diagnóstico de campo.

Listo, el sistema está preparado para las prácticas y para presentar avances al profesor. Si detectas un comportamiento nuevo, documenta el log serie y adjúntalo en `SOLUCION_COMPILACION.md`.
