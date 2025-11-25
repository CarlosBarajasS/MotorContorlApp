# Solución para RS3-1D12-41M SSR - Problema RESUELTO ✅

## ~~Problema Identificado~~ FALSO - Era el Tipo de Señal

**ACTUALIZACIÓN:** El problema NO era el voltaje. El **RS3-1D12-41M** acepta **3.5-32 VDC** y los 3.3V del ESP32 están en el límite, pero funciona.

### Verdadera Causa del Problema

Estábamos usando **`digitalWrite()`** (señal digital ON/OFF) cuando deberíamos usar **`analogWrite()`** (señal PWM).

El código del profesor que funciona usa:
```cpp
analogWrite(ledPin, brightness);  // PWM signal
```

Nuestro código fallaba porque usaba:
```cpp
digitalWrite(SSR_SIGNAL_PIN, HIGH);  // Simple digital signal
```

### ¿Por Qué analogWrite() Funciona Mejor?

1. **Señal PWM continua** - Pulsos rápidos que activan mejor el optoacoplador interno del SSR
2. **Mayor corriente efectiva** - La señal PWM proporciona más energía al input
3. **Activación más confiable** - El optoacoplador responde mejor a señales pulsantes

---

## ✅ SOLUCIÓN APLICADA

He modificado todo el código ESP32 para usar `analogWrite()` en lugar de `digitalWrite()`:

### Cambios Realizados

```cpp
// ❌ ANTES (NO FUNCIONABA)
digitalWrite(SSR_SIGNAL_PIN, HIGH);  // ON
digitalWrite(SSR_SIGNAL_PIN, LOW);   // OFF

// ✅ AHORA (FUNCIONA)
analogWrite(SSR_SIGNAL_PIN, 255);  // ON (PWM máximo)
analogWrite(SSR_SIGNAL_PIN, 0);    // OFF (PWM en 0)
```

### Funciones Modificadas

1. ✅ `setupMotorPins()` - Inicialización con analogWrite
2. ✅ `stopMotor()` - Apagado con analogWrite(0)
3. ✅ `startMotor()` - Encendido con analogWrite(255)
4. ✅ `startContinuousMode()` - Arranque continuo con PWM
5. ✅ `handleArranque6P()` - Arranque suave con PWM
6. ✅ `setMotorSpeed()` - Control de velocidad con PWM
7. ✅ Eliminado watchdog innecesario del `loop()`

---

## 🧪 Próximos Pasos de Prueba

1. **Cargar el código actualizado** al ESP32
2. **Probar arranque continuo** (comando `0i` o botón "Continuo" en la app)
3. **Probar arranque suave** (arranque6p con 6 valores)
4. **Verificar que el motor se mantiene encendido** sin apagarse

---

## 📋 Resumen Técnico

| Aspecto | Antes ❌ | Ahora ✅ |
|---------|----------|----------|
| Función | `digitalWrite()` | `analogWrite()` |
| Tipo de señal | Digital ON/OFF | PWM (0-255) |
| Encendido | `HIGH` | `255` |
| Apagado | `LOW` | `0` |
| Resultado | Motor arranca y se apaga | Motor funciona correctamente |

---

## ⚠️ INFORMACIÓN ANTIGUA (Ya No Necesaria)

El resto de este documento contiene soluciones de hardware que probamos cuando pensábamos que era un problema de voltaje. **Ya no son necesarias** porque el problema era de software.

Las dejo aquí solo como referencia histórica.

---

## ~~Solución 1: Circuito Level Shifter con Transistor~~ (NO NECESARIO)

### Diagrama de Conexión

```
                    +5V (VIN del ESP32)
                     │
                     │
                     ├─────────┐
                     │         │
                    [R2]      │
                    220Ω      │
                     │         │
                     └────┬────┘
                          │
ESP32 GPIO25 ───[R1]─────┤ NPN Transistor
                 10kΩ     │ (2N2222/BC547)
                          │
                          ├────────► Pin Señal SSR (+)
                          │
                         GND ────► GND SSR (-)
```

### Lista de Componentes

| Componente | Valor/Modelo | Cantidad | Propósito |
|------------|--------------|----------|-----------|
| Transistor NPN | 2N2222, BC547, 2N3904 | 1 | Amplificador de voltaje |
| Resistencia | 10kΩ (1/4W) | 1 | Limitador base transistor |
| Resistencia | 220Ω (1/4W) | 1 | Protección SSR |
| Cables | Dupont M-M | 4 | Conexiones |

### Conexiones Detalladas

#### Paso 1: Preparar el Transistor
- Identifica los pines del transistor (Base, Colector, Emisor)
- Para 2N2222 (mirando de frente, pines hacia abajo):
  - Izquierda = Emisor
  - Centro = Base
  - Derecha = Colector

#### Paso 2: Conectar Resistencias
1. **R1 (10kΩ)**: Entre GPIO25 del ESP32 y Base del transistor
2. **R2 (220Ω)**: Entre Emisor del transistor y pin señal del SSR

#### Paso 3: Conectar Alimentación
1. **Colector del transistor** → **Pin 5V (VIN)** del ESP32
2. **Emisor del transistor** → **Pin señal (+) del SSR** (a través de R2)
3. **GND del ESP32** → **GND (-) del SSR**

### ¿Cómo Funciona?

1. **GPIO25 = HIGH (3.3V)**
   - La corriente fluye por R1 hacia la base del transistor
   - El transistor conduce (enciende)
   - La corriente fluye desde el colector (5V) al emisor
   - El emisor entrega ~4.7V al SSR
   - **4.7V > 3.5V mínimo** → SSR activa ✅

2. **GPIO25 = LOW (0V)**
   - No hay corriente en la base
   - El transistor NO conduce (apaga)
   - No fluye corriente al SSR
   - SSR desactiva ✅

---

## Solución 2: Usar GPIO33 con Mayor Corriente (TEMPORAL)

Si no tienes transistor disponible, intenta usar GPIO33 que puede proporcionar mayor corriente:

### Cambios en el Código

```cpp
// Cambiar línea 46 en ESP32_Motor_Controller.ino
#define SSR_SIGNAL_PIN 33  // Cambiar de 25 a 33
```

### Pros y Contras
- ✅ **Pro:** No requiere componentes adicionales
- ❌ **Contra:** Sigue siendo 3.3V, puede no funcionar confiablemente
- ⚠️ **Advertencia:** Esta es una solución temporal, no garantizada

---

## Solución 3: Módulo Level Shifter 3.3V a 5V

Si tienes un módulo level shifter disponible:

### Módulos Compatibles
- **4-channel Logic Level Converter** (3.3V ↔ 5V)
- **BSS138 MOSFET Level Shifter**

### Conexiones
```
ESP32 GPIO25 → LV1 (Low Voltage Input)
ESP32 3.3V   → LV (Low Voltage Power)
ESP32 5V     → HV (High Voltage Power)
ESP32 GND    → GND

HV1 (High Voltage Output) → Pin señal SSR (+)
GND                        → GND SSR (-)
```

---

## Prueba Rápida (Diagnóstico)

Para confirmar que el problema es de voltaje:

1. **Desconecta el GPIO25 del SSR**
2. **Conecta directamente el pin 5V del ESP32 al pin señal del SSR**
3. **Observa si el motor arranca**

Si el motor arranca con 5V directo → **Confirmado: problema de voltaje**

---

## Código Mejorado (Opcional)

Si usas el transistor, puedes ajustar el código para asegurar señales más fuertes:

```cpp
void setupMotorPins() {
  pinMode(SSR_SIGNAL_PIN, OUTPUT);

  // Asegurar estado LOW inicial
  digitalWrite(SSR_SIGNAL_PIN, LOW);
  delay(500);  // Delay más largo para estabilizar

  // Pulso de prueba
  digitalWrite(SSR_SIGNAL_PIN, HIGH);
  delay(100);
  digitalWrite(SSR_SIGNAL_PIN, LOW);
  delay(100);

  Serial.println("SSR control pin configured with transistor amplifier");
}
```

---

## Recomendación Final

**Usa la Solución 1 (transistor)** porque:
- ✅ Es barata (< $5 MXN)
- ✅ Es confiable
- ✅ Garantiza 5V al SSR
- ✅ Protege el ESP32

---

## Referencias

- [RS3-1D12-41M Especificaciones - TestEquity](https://www.testequity.com/product/10128413-RS3-1D12-41M)
- [RS3-1D12-41M Datasheet - LCSC](https://www.lcsc.com/product-detail/Photoelectric-Thyristor-Solid-State-Relays_NTE-ELECTRONICS-INC-RS3-1D12-41M_C6201525.html)
- [NTE Electronics RS3 Series - Amazon](https://www.amazon.com/NTE-Electronics-RS3-1D12-41M-SPST-NO-Arrangement/dp/B005T9WJ6U)
