# 🛠️ Motor Control App – Solución a problemas de compilación

Este documento concentra los errores que aparecieron durante las últimas sesiones y cómo se solucionaron. Úsalo como referencia rápida antes de reportar un bug.

---

## 1. Gradle / WSL

| Error | Causa | Solución |
|-------|-------|----------|
| `/usr/bin/env: 'sh\r': No such file or directory` | `gradlew` con CRLF | Ejecuta `dos2unix gradlew` o `python3 - <<'PY' ...` para reemplazar `\r\n` por `\n`. |
| `JAVA_HOME is not set` | Falta JDK en WSL | Instala `openjdk-17-jdk` y exporta `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`. |
| `Permission denied` | gradlew sin permisos | `chmod +x gradlew`. |

---

## 2. Kotlin / Compose

| Error | Motivo | Solución |
|-------|--------|----------|
| `Unresolved reference 'ESP32Status' / 'WiFiCredentials'` | Se eliminaron modelos al limpiar el proyecto | Reinstaurados en `app/src/main/java/com/arranquesuave/motorcontrolapp/network/ESP32Models.kt`. |
| `Cannot infer type for parameter` en `collectLatest` o `MutableStateFlow` | Falta tipo explícito tras cambios en ViewModels | Se revisó `MotorViewModel` y `WiFiSetupViewModel` agregando tipos y retornos claros. |
| `Expression 'weight' cannot be invoked` en `WiFiSetupScreenReal` | Uso de `RowScope.weight` fuera del scope | La función `StepDivider` ahora recibe el `Modifier.weight(1f)` desde el padre. |

---

## 3. Firmware / Bluetooth

| Problema | Síntoma | Solución |
|----------|---------|----------|
| Arranque 6P enviaba payload sin sufijos | Serial mostraba `unknown_command` | `Protocol.encodeArranqueSuave()` volvió a generar `a..f` y el firmware agrega `extractStepValue()` para validar cada token. |
| UI quedaba bloqueada en “Paro” | Botones Arranque/Continuo deshabilitados | `MotorViewModel` sincroniza `motorRunning` según `motorMode` y la UI habilita botones cuando llega el ACK `0p`. |
| Botón “Desconectar” no cerraba BT | Estado seguía “conectado” | `BluetoothMotorController.disconnect()` cierra el socket antes de cancelar el `readerJob`, y `BluetoothService.close()` pone `socket = null`. |

---

## 4. Pasos recomendados cuando reaparezca un error

1. **Limpiar el proyecto**
   ```bash
   ./gradlew clean
   ```
2. **Revisar dependencias** en `build.gradle(.kts)` (no se requieren libs externas adicionales).
3. **Sincronizar** en Android Studio para regenerar `BuildConfig` y recursos.
4. **Verifica versiones del SDK** (compile/target 34) y habilita `kotlinOptions { jvmTarget = "17" }` si agregas nuevos módulos.
5. **Firmware**: re-flashea cuando cambies la lógica de comandos para evitar estados residuales en EEPROM.

---

## 5. Reportar nuevos errores

Incluye siempre:
- Comando o acción ejecutada.
- Log completo (`gradlew ... --stacktrace` o captura del Monitor Serie / Logcat).
- Archivos modificados justo antes del fallo.

Así podremos documentar nuevas soluciones en este archivo. 💪
