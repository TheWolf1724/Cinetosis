# Arquitectura técnica de Cinetosis

## Visión general

```
                       ┌──────────────────────────────┐
                       │        SENSORES (HW)          │
                       │  Acelerómetro + Giroscopio    │
                       └───────────────┬──────────────┘
                                       │ eventos
                              ┌────────▼─────────┐
                              │   MotionEngine   │  fusión + filtrado
                              │  (vector 2D)     │
                              └───┬──────────┬───┘
                                  │          │
                  estado/ajustes  │          │ vector de movimiento
        ┌─────────────────────────▼──┐   ┌───▼─────────────────────┐
        │     SettingsRepository      │   │      OverlayService     │
        │   (DataStore Preferences)   │   │  (Foreground Service)   │
        └───────────┬─────────────────┘   │   ┌──────────────────┐  │
                    │                      │   │     DotsView     │  │
        ┌───────────▼───────────┐          │   │ (Canvas, bordes) │  │
        │     MainActivity      │          │   └──────────────────┘  │
        │  (Compose: ajustes)   │          └──────────▲──────────────┘
        └───────────────────────┘                     │ start/stop
                    ▲                       ┌──────────┴───────────┐
                    │ abre                  │ CinetosisTileService │
                    └───────────────────────┤  (Quick Settings)    │
                                            └──────────────────────┘
```

## Componentes

### `MotionEngine` (`motion/`)
Núcleo de procesamiento. Se suscribe a `SensorManager`:
- `TYPE_LINEAR_ACCELERATION` → aceleración sin gravedad (empuje longitudinal/lateral).
- `TYPE_GYROSCOPE` → velocidad angular (giros).

Procesado de la señal:
1. **Filtro paso-bajo** (suavizado exponencial) para quitar vibración de alta frecuencia.
2. **Compensación de orientación con `RotationVector`** (ver abajo).
3. **Zona muerta** configurable para ignorar microaceleraciones.
4. **Escalado por sensibilidad** (ajuste del usuario).
5. Salida: un **vector 2D normalizado** `(x, y)` que representa hacia dónde deben desplazarse los
   puntos (en el sentido de la fuerza inercial sentida).

> **Compensación de orientación.** Se usa `TYPE_GAME_ROTATION_VECTOR` (sin magnetómetro, mejor en
> coches; con fallback a `TYPE_ROTATION_VECTOR`) para obtener la matriz de rotación device→world.
> Con ella se transforma la aceleración lineal al mundo, se toma su componente **horizontal** y se
> proyecta sobre los ejes de la **pantalla** (también proyectados al plano horizontal), teniendo en
> cuenta además la rotación del display. Así el mapeo acelerar/frenar/curvas es correcto sea cual sea
> la inclinación con que se sujete el teléfono. (Con el móvil casi vertical, el eje longitudinal de
> pantalla se vuelve casi vertical y su señal tiende a 0: limitación física inevitable.) La función
> de proyección vive en `MotionMath.screenComponents` y está cubierta por tests.

### `OverlayService` (`service/`)
**Foreground Service** (con notificación persistente, obligatoria en Android 8+). Responsable de:
- Crear una vista a pantalla completa mediante `WindowManager` con
  `TYPE_APPLICATION_OVERLAY`, **no táctil** (`FLAG_NOT_TOUCHABLE` | `FLAG_NOT_FOCUSABLE`) para no
  interferir con la app de debajo.
- Hospedar la `DotsView` y alimentarla con el vector de `MotionEngine`.
- Arrancar/parar el motor de sensores junto con el ciclo de vida del servicio.
- `foregroundServiceType="specialUse"` (requisito de Android 14+).

### `DotsView` (`overlay/`)
`View` personalizada que dibuja sobre un `Canvas`:
- Una rejilla de **puntos en los bordes** (configurable: qué bordes, cuántos, tamaño, color).
- Cada punto se **desplaza** respecto a su posición de reposo según el vector de movimiento,
  con interpolación para un movimiento fluido.
- Bucle de animación con `Choreographer`/`postOnAnimation` para ~60 fps eficientes.

### `CinetosisTileService` (`tile/`)
**Quick Settings Tile** (acceso rápido de la barra de notificaciones):
- Estado `ACTIVE`/`INACTIVE` reflejando si el overlay está activo.
- Al pulsar: si falta el permiso de overlay, abre la app/ajustes; si está concedido, arranca o
  para `OverlayService`.

### `SettingsRepository` (`data/`)
Persistencia con **Jetpack DataStore (Preferences)**: sensibilidad, número de puntos, tamaño,
color/tema, bordes activos y estado activado/desactivado. Expone `Flow`s que observan tanto la
UI como el servicio.

### Detección "voy en coche" (`detection/`)
Detecta de forma **offline** si el usuario va en coche y, en ese caso, **enciende `OverlayService`
automáticamente** (el apagado es manual). Clave de diseño: se hace con **comprobaciones periódicas**
(`AlarmManager`), **sin servicio en primer plano y por tanto sin notificación permanente** (como las
apps que funcionan en segundo plano estando exentas de batería).
- `DetectionScheduler`: programa una alarma que se reprograma a sí misma. El **intervalo** depende
  del modo (`DetectionMode`): `BATTERY` ~3 min, `BALANCED` ~90 s, `EXTREME` ~45 s. En **ahorro de
  batería** del sistema se usa el intervalo más largo.
- `DetectionAlarmReceiver`: en cada disparo, con `goAsync()` (< 10 s) muestrea el **acelerómetro**
  unos 8 s y `VehicleClassifier` (lógica pura, con tests) decide "coche vs andar/quieto". Si es
  coche y hay permiso de overlay, enciende `OverlayService`. Solo usa el **acelerómetro** (sin GPS).
- `DetectionState`: guarda en `SharedPreferences` el instante del último apagado manual para un
  **cooldown** (no reactivar al instante tras apagarlo a mano).

### `BootReceiver` (`boot/`)
Receptor de `BOOT_COMPLETED`: si el usuario lo activó, **programa** la detección periódica al
encender el teléfono (sin servicio en primer plano ni notificación).

### `MainActivity` + `ui/` (Compose)
Pantalla de **ajustes e información**:
- Explica la función y la ciencia (resumen).
- Botón para **conceder el permiso** «Mostrar sobre otras apps».
- Controles (sliders/switches) enlazados a `SettingsRepository`.
- Botón para activar/desactivar el overlay (equivalente al Tile).

## Permisos

| Permiso | Motivo |
|---------|--------|
| `SYSTEM_ALERT_WINDOW` | Dibujar el overlay sobre otras apps. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Servicio en primer plano del overlay, solo mientras los indicadores están encendidos. |
| `RECEIVE_BOOT_COMPLETED` | Programar la detección al encender el móvil (si se activa). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Pedir exención para que la detección periódica no se mate. |
| `POST_NOTIFICATIONS` | Notificación del servicio en primer plano (solo con los indicadores activos). |

**No** se declara permiso de Internet **ni de ubicación**: la app es completamente offline (la
detección usa solo el acelerómetro y **no** usa Google Play Services). La detección en segundo plano
**no muestra notificación** (no es un servicio en primer plano).

## Decisiones de diseño

- **Overlay del sistema** (no solo intra-app) para que funcione mientras usas mapas, lees, etc.,
  igual que la solución de Apple.
- **Sin red ni telemetría** por privacidad y para mantener coste cero.
- **Compose + Material 3** para la UI de ajustes; **Canvas/View** para el overlay por eficiencia
  de dibujado en tiempo real.

## Hoja de ruta (futuro)

- Mejorar el clasificador de "en vehículo" (más características, distinguir tren/bici).
- Calibración guiada de sensibilidad.
- Selector de color manual de los puntos en la UI.
