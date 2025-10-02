# Motor Control App - Versión Simplificada

## 📝 Cambios Realizados

Esta es la versión simplificada de la aplicación de Control de Motor, donde se ha eliminado completamente el sistema de autenticación (login y registro).

### ✅ Modificaciones Principales

1. **MainActivity.kt**
   - ✂️ Eliminado `AuthViewModel` y toda la lógica de autenticación
   - ✂️ Eliminadas las rutas de navegación "login" y "signup"
   - ✅ La app inicia directamente en la pantalla de control del motor
   - ✅ Manejo de botón back simplificado

2. **MotorControlScreen.kt**
   - ✂️ Eliminado parámetro `onLogout`
   - ✂️ Eliminado botón de "Logout" del TopAppBar
   - ✅ Funcionalidad del motor intacta

3. **BluetoothControlScreen.kt**
   - ✂️ Eliminado parámetro `onLogout`
   - ✂️ Eliminado botón de "Logout" del TopAppBar
   - ✅ Funcionalidad de Bluetooth intacta

4. **build.gradle.kts**
   - ✂️ Eliminados flavors "demo" y "prod"
   - ✂️ Eliminadas dependencias de autenticación (retrofit, gson, security-crypto, datastore)
   - ✅ Build más simple y rápido

### 📁 Archivos que Puedes Eliminar (Opcional)

Si deseas limpiar completamente el proyecto, puedes eliminar estos archivos ya que no se usan:

```
app/src/main/java/com/arranquesuave/motorcontrolapp/
├── ui/screens/
│   ├── LoginScreen.kt          ❌ (No se usa)
│   └── SignUpScreen.kt         ❌ (No se usa)
├── viewmodel/
│   └── AuthViewModel.kt        ❌ (No se usa)
├── data/
│   └── AuthRepository.kt       ❌ (No se usa)
├── network/
│   ├── AuthApi.kt              ❌ (No se usa)
│   ├── RetrofitClient.kt       ❌ (No se usa)
│   └── model/
│       ├── AuthRequest.kt      ❌ (No se usa)
│       ├── AuthResponse.kt     ❌ (No se usa)
│       └── User.kt             ❌ (No se usa)
└── utils/
    ├── SessionManager.kt       ❌ (No se usa)
    └── Security.kt             ❌ (No se usa)
```

### 🚀 Cómo Usar

1. **Sincronizar el proyecto:**
   ```bash
   # En Android Studio: File > Sync Project with Gradle Files
   ```

2. **Compilar y ejecutar:**
   - La app iniciará directamente en la pantalla de control del motor
   - No necesitas hacer login
   - Todos los controles del motor funcionan igual

3. **Navegación:**
   - Pantalla principal: Control del Motor
   - Pantalla secundaria: Configuración de Bluetooth

### ⚙️ Funcionalidades Mantenidas

✅ Control de 6 PWM para el motor
✅ Envío de valores al dispositivo Bluetooth
✅ Arranque continuo
✅ Paro de emergencia
✅ Búsqueda y conexión de dispositivos Bluetooth
✅ Gestión de permisos de Bluetooth
✅ Navegación entre pantallas

### 🔄 Volver a la Versión Original

Si necesitas volver a la versión con autenticación:

```bash
# Cambiar a la rama master
git checkout master

# O ver los cambios
git diff master version-simplificada
```

---

## 📌 Notas Importantes

- Esta versión NO requiere conexión a internet
- NO hay validación de usuarios
- La app es ideal para demostraciones o uso interno
- Todos los permisos de Bluetooth se mantienen

---

**Fecha de simplificación:** Octubre 2025
**Rama:** version-simplificada
