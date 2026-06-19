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
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Servicio en primer plano para mantener el overlay y los sensores activos. |
| `POST_NOTIFICATIONS` | Notificación del servicio en primer plano (Android 13+). |

**No** se declara permiso de Internet: la app es completamente offline.

## Decisiones de diseño

- **Overlay del sistema** (no solo intra-app) para que funcione mientras usas mapas, lees, etc.,
  igual que la solución de Apple.
- **Sin red ni telemetría** por privacidad y para mantener coste cero.
- **Compose + Material 3** para la UI de ajustes; **Canvas/View** para el overlay por eficiencia
  de dibujado en tiempo real.

## Hoja de ruta (futuro)

- Perfiles automáticos (detección de «en vehículo» por patrón de aceleración).
- Calibración guiada de sensibilidad.
- Selector de color manual de los puntos en la UI.
