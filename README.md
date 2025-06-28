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
| 2025-06-25  | Separación de `encodeStartRamp`; ajuste de botones UI; README.

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
│  │  └─ utils/Protocol.kt
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
- Agregar fechas y detalles de nuevas funcionalidades.
- Documentar pruebas en hardware real y lecturas.
- Registrar errores detectados y pasos de solución.

*Este documento se actualizará progresivamente con el avance del proyecto.*
