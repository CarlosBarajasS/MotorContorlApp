# 🚀 Motor Control App - Configuración para Broker del Profesor

## ✅ Configuración Actualizada

Tu aplicación ahora está configurada para usar el **broker MQTT del profesor**:
- **IP**: `177.247.175.4`
- **Puerto**: `1885`

No necesitas configurar ningún broker local en tu laptop.

---

## 📱 Modos de Conexión Simplificados

### **1. Bluetooth (Recomendado para desarrollo)**
- Conecta ESP32 directamente por Bluetooth
- No requiere WiFi ni internet
- Ideal para pruebas y debugging

### **2. WiFi Local + MQTT**
- ESP32 y teléfono en la misma red WiFi
- Comunican vía MQTT al broker del profesor
- Requiere internet para llegar al broker `177.247.175.4:1885`

### **3. MQTT Remoto**
- Mismo broker que WiFi Local
- Control desde cualquier lugar con internet
- Ambos dispositivos se conectan al broker del profesor

---

## 🔧 Configuración del ESP32

### **Paso 1: Cargar el código**
1. Abre `ESP32_Motor_Controller.ino` en Arduino IDE
2. El código ya está configurado con la IP del profesor: `177.247.175.4:1885`
3. Carga el código en tu ESP32

### **Paso 2: Configurar WiFi**
1. ESP32 creará un WiFi llamado `"ESP32-MotorSetup"`
2. Conéctate desde tu teléfono con password: `"12345678"`
3. Abre navegador en: `http://192.168.4.1`
4. O usa la app Android en modo "WiFi Setup"

### **Paso 3: Conectar a tu WiFi**
1. Escanea redes WiFi disponibles
2. Selecciona tu red WiFi
3. Introduce la contraseña
4. El ESP32 se reiniciará automáticamente

---

## 📱 Uso de la Aplicación Android

### **Compilar la app**
```bash
cd /path/to/MotorControlApp
./gradlew assembleDebug
```

### **Conectar por Bluetooth**
1. Abre la app
2. Selecciona modo "Bluetooth"
3. Buscar dispositivos → Seleccionar ESP32
4. ¡Listo para controlar el motor!

### **Conectar por WiFi/MQTT**
1. Asegúrate que ESP32 esté conectado a WiFi
2. Selecciona modo "WiFi Local" o "MQTT Remote"
3. Presiona "Connect MQTT"
4. ¡Controla tu motor desde cualquier lugar!

---

## 🎮 Controles del Motor

### **Arranque Suave (6 Pasos)**
- Configura los 6 sliders con valores 0-255
- Presiona "Arranque 6P"
- El motor acelera gradualmente según tus valores

### **Arranque Continuo**
- Presiona "Continuo"
- Motor arranca a velocidad máxima inmediatamente

### **Paro de Emergencia**
- Presiona "Paro"
- Motor se detiene instantáneamente

---

## 🔍 Verificar Conexiones

### **Topics MQTT que usa tu app:**
- `motor/command` - Comandos (arranque6p, continuo, paro)
- `motor/speed` - Velocidad actual del motor
- `motor/state` - Estado (running/stopped)
- `motor/current` - Corriente del motor
- `motor/voltage` - Voltaje del motor
- `motor/type` - Tipo de arranque actual

### **Pruebas de conexión:**
1. **ESP32**: Abre Serial Monitor, verifica que se conecte al broker
2. **App Android**: En logs verifica conexión MQTT exitosa
3. **Broker**: Ambos dispositivos deben aparecer conectados

---

## 🚨 Resolución de Problemas

### **"MQTT Connection Failed"**
- Verifica que tanto ESP32 como teléfono tengan internet
- El broker `177.247.175.4:1885` debe estar accesible
- Revisa que el puerto 1885 no esté bloqueado

### **"Bluetooth Connection Failed"**
- Verifica que ESP32 tenga Bluetooth habilitado
- Empareja el dispositivo desde Configuración Android primero
- Asegúrate que no esté conectado a otra app

### **"Motor no responde"**
- Verifica conexiones físicas del motor al ESP32
- Revisa que los pines estén correctamente definidos
- Comprueba alimentación del motor

---

## 📊 Estado Actual

✅ **Configuración MQTT**: Broker del profesor `177.247.175.4:1885`  
✅ **Código ESP32**: Actualizado con IP correcta  
✅ **App Android**: Compilación corregida  
✅ **Modos de conexión**: Bluetooth + MQTT funcionales  

---

## 🎯 Pasos para Empezar

1. **Compila** la app Android
2. **Carga** el código ESP32
3. **Configura** WiFi del ESP32 
4. **Prueba** conexión Bluetooth primero
5. **Conecta** vía MQTT cuando WiFi esté listo
6. **Controla** tu motor desde la app

¡Tu sistema está listo para funcionar con el broker del profesor! 🎉
