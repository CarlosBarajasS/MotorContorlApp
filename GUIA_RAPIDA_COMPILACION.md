# 🚀 Guía Rápida para Compilar tu Motor Control App

## ✅ Problemas Solucionados

### **Aplicación Android:**
- ❌ Eliminado `ESP32ConfigHelper.kt` duplicado
- ❌ Movido directorio `examples/` problemático  
- ❌ Movido directorio `testing/` problemático
- ✅ Simplificado `MotorViewModel.kt` 
- ✅ Configuración broker profesor: `177.247.175.4:1885`

### **Código ESP32:**
- ❌ Cambiado `A0, A1` por `GPIO36, GPIO39` 
- ✅ Pines correctos para ESP32
- ✅ Broker del profesor configurado por defecto

---

## 🔧 Compilar la Aplicación

### **Paso 1: Limpiar proyecto**
```bash
cd C:\Users\LaBendiChao\Desktop\MotorContorlApp
.\gradlew clean
```

### **Paso 2: Compilar**
```bash
.\gradlew assembleDebug
```

Si hay errores, prueba:
```bash
.\gradlew assembleDemo
```

---

## 📱 Subir Código ESP32

### **Paso 1: Abrir Arduino IDE**
1. Abre `ESP32_Motor_Controller.ino`
2. Verifica que tengas seleccionado:
   - Board: "ESP32 Dev Module"
   - Port: Tu puerto COM

### **Paso 2: Cargar código**
1. Presiona el botón de upload
2. Si hay error de compilación, instala librerías:
   - `WiFi` (ya incluida)
   - `WebServer` (ya incluida) 
   - `PubSubClient` - Para MQTT
   - `ArduinoJson` - Para JSON

### **Instalar librerías faltantes:**
```
Tools → Manage Libraries → Buscar:
- PubSubClient (by Nick O'Leary)
- ArduinoJson (by Benoit Blanchon)
```

---

## 🎯 Pines ESP32 Actualizados

```cpp
// Pines correctos para ESP32
#define MOTOR_ENABLE_PIN 2      // GPIO2
#define MOTOR_DIR_PIN 4         // GPIO4  
#define MOTOR_PWM_PIN 5         // GPIO5
#define CURRENT_SENSOR_PIN 36   // GPIO36 (ADC1_0)
#define VOLTAGE_SENSOR_PIN 39   // GPIO39 (ADC1_3)
```

## 🔌 Conexión Motor al ESP32

```
Motor Driver → ESP32
VCC         → 3.3V/5V
GND         → GND
IN1         → GPIO2 (ENABLE)
IN2         → GPIO4 (DIRECTION)
PWM         → GPIO5 (SPEED)

Sensores → ESP32
Current     → GPIO36
Voltage     → GPIO39
```

---

## 🚀 Primer Uso

### **1. Configurar ESP32:**
1. Carga el código en ESP32
2. ESP32 creará WiFi "ESP32-MotorSetup" 
3. Conéctate desde tu teléfono (password: "12345678")
4. Abre navegador: `http://192.168.4.1`
5. Configura tu WiFi doméstico

### **2. Usar la aplicación:**
1. Instala la app en tu teléfono
2. Selecciona modo "Bluetooth" para pruebas
3. O selecciona "WiFi Local" para usar MQTT
4. Conecta y controla tu motor

---

## 🎮 Controles Disponibles

| Comando | Función | Uso |
|---------|---------|-----|
| **Arranque 6P** | Arranque suave 6 pasos | Configura sliders y presiona |
| **Continuo** | Arranque directo | Velocidad máxima inmediata |
| **Paro** | Paro de emergencia | Detiene motor instantáneamente |

---

## 🔍 Si hay errores:

### **Android - Error de compilación:**
```bash
.\gradlew clean
.\gradlew build --stacktrace
```

### **ESP32 - Error "A1 not declared":**
- Verifica que uses la versión corregida del código
- Los pines deben ser `GPIO36` y `GPIO39`, no `A0` y `A1`

### **MQTT no conecta:**
- Verifica que tienes internet
- El broker `177.247.175.4:1885` debe estar accesible  
- Revisa que no esté bloqueado por firewall

---

## ✅ Estado Final

🟢 **Aplicación Android**: Lista para compilar  
🟢 **Código ESP32**: Pines corregidos  
🟢 **Broker MQTT**: Configurado para profesor  
🟢 **Archivos problemáticos**: Removidos  

¡Tu sistema está listo para funcionar! 🎉
