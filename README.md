# MotorControlApp

Proyecto de aplicación Android para control de motor vía Bluetooth Classic SPP.

## Contexto Académico
Este proyecto forma parte de:
- **Residencias Profesionales** (Ingeniería en Sistemas).
- **Proyecto de Investigación** para titulación.

## Descripción
MotorControlApp permite:
- Arranque suave de 6 pasos (5 aceleraciones) con slider para cada paso (_ARRANQUE 6P_).
- Arranque continuo rápido (_CONTINUO_).
- Paro de emergencia (_PARO_).
- Monitor de velocidad en tiempo real.

Desarrollada en Kotlin con Jetpack Compose y corutinas, utilizando arquitectura MVVM y `BluetoothService` para comunicación.

## Protocolo de Comunicación
- Comandos ASCII individuales para cada paso: `<valor><letra>,` (letras de `a` a `f`).
- Paro de emergencia: `0p,`.
- Arranque continuo: `0i,`.
- Decodificación de velocidad: trama `D0+velocidad`.

## Cronograma de Desarrollo
| Fecha       | Hito                                                       |
|-------------|------------------------------------------------------------|
| 2025-06-20  | Inicio del proyecto: UI básica, permisos Bluetooth.        |
| 2025-06-21  | Implementación de lectura de velocidad y control continuo. |
| 2025-06-22  | Primer arranque suave con CSV combinado (encodeArranqueSuave). |
| 2025-06-23  | Depuración: envío individual de pasos (`encodeStep`) y retardo. |
| 2025-06-24  | Corrección de paro de emergencia a ASCII (`encodeParo`).   |
| 2025-06-25  | Separación de `encodeStartRamp`; ajuste de botones UI; README. |
| 2025-07-02  | Agregado flavor `demo`/`prod` con flag `BuildConfig.NO_AUTH` para demo sin autenticación. |
| 2025-07-03  | Configurado splash screen con `motor_control_background` para logo desvanecido al iniciar. |
| 2025-07-12  | Restricción de botones de arranque según estado del motor; manejo de BackHandler para navegación y confirmación de salida; actualización de icono de la app. |
|

## Complicaciones y Soluciones
- **Protocolo incorrecto**: uso inicial de comandos binarios vs ASCII, causando falta de respuesta del MCU. _Solución_: migración a ASCII con sufijos y comas.
- **Envío combinado CSV**: última etapa no procesada por MCU. _Solución_: enviar cada comando por separado con `delay(100)`.
- **Orden de comandos**: `0i,` se enviaba en arranque 6P. _Solución_: separar lógica de `sendContinuo()` y `sendArranque6P()`.
- **Interfaz**: confusión en botones y sliders. _Solución_: eliminar slider de continuo y simplificar botón _CONTINUO_.

## Estructura de Archivos
```
MotorControlApp/
├─ app/
│  ├─ src/main/java/com/arranquesuave/motorcontrolapp/
│  │  ├─ ui/screens/*.kt  
│  │  ├─ viewmodel/*.kt
│  │  ├─ services/BluetoothService.kt
│  │  ├─ utils/Protocol.kt
│  │  ├─ utils/SessionManager.kt
│  │  ├─ utils/AuthRepository.kt
│  └─ AndroidManifest.xml
└─ README.md
```

## Uso
1. Conceder permisos Bluetooth en primer inicio.
2. Conectar a dispositivo SPP Bluetooth.
3. Pulsar **CONTINUO** para arranque rápido.
4. Ajustar sliders y pulsar **ARRANQUE 6P** para rampa de 6 pasos.
5. Pulsar **PARO** para detener inmediatamente.

## Notas de Desarrollo

### Cambios Recientes
- **UI Refinements**: En `MotorControlScreen`, la paleta de colores se ajustó para coincidir con el diseño Figma (bots, sliders y contenedores suaves). Se eliminó el texto de velocidad en tiempo real para simplificar la interfaz.
- **Decodificación de Velocidad**: En `Protocol.kt`, `decodeSpeed` se revirtió al decodificado binario original (first byte - VEL_B) para evitar interferencia de datos ASCII.
- **Persistencia de Sesión**: Creación de `SessionManager` (`SharedPreferences`) que guarda el token de autenticación.
- **MainActivity**:
  - Inicializa `SessionManager` y determina `startDestination` según la sesión.
  - Guarda `token` al iniciar sesión (Login) y lo limpia al hacer logout.
- **Estructura y Navegación**: El flujo de pantallas ahora respeta la sesión activa al relanzar la app.
- **Demo Flavor**: Agregados flavors `demo` y `prod` con flag `BuildConfig.NO_AUTH` para demo sin autenticación.
- **Splash Screen**: Implementado windowBackground con `motor_control_background` para mostrar logo desvanecido durante el arranque.
- **Reversión de Splash**: Eliminado splash separado; unificado fondo en tema principal.
- **Pantalla de Control Bluetooth** (`BluetoothControlScreen.kt`):
    - Muestra estado de conexión y errores en tiempo real.
    - Añade indicador de progreso (`CircularProgressIndicator`) y botones "Buscar dispositivos" / "Detener búsqueda".
    - Incluye botón "Buscar nuevamente" para reescanear sin reiniciar la app.
    - Gestión de cuatro estados: conectado, escaneando, sin dispositivos, listado de dispositivos.
    - Botón "Desconectar" para cerrar la conexión y limpiar estado.

- **Restricciones de Arranque**: Deshabilita botones de arranque suave/continuo si el motor ya está en ejecución; habilita botón de paro solo cuando el motor esté corriendo.
- **Manejo de Botón Atrás**: Implementa `BackHandler` en `MainActivity` para navegación entre pantallas Bluetooth, Control, Login y Signup; confirmación de salida con doble pulsación.
- **Icono de la App**: Cambia el icono de la aplicación por un recurso adaptativo personalizado usando Image Asset Studio.

## Capturas

<!-- Inserta aquí capturas de pantalla de la app -->

Añade tus capturas en `docs/screenshots/` y referencia aquí:

![Pantalla de Motor Control](docs/screenshots/motor_control.png)
![Pantalla de Splash](docs/screenshots/splash.png)
<!-- Otras capturas -->

## Ejemplos de Errores y Soluciones

<!-- Inserta aquí capturas de errores detectados y descripción de la solución -->
- Agregar fechas y detalles de nuevas funcionalidades.
- Documentar pruebas en hardware real y lecturas.
- Registrar errores detectados y pasos de solución.

*Este documento se actualizará progresivamente con el avance del proyecto.*
