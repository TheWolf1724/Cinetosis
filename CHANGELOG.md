# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/)
y el proyecto sigue el [Versionado Semántico](https://semver.org/lang/es/).

## [No publicado]

### Añadido
- **Página de onboarding «Batería y autoinicio»** y **botón en ajustes** para pedir la exención de
  optimización de batería, de modo que el auto-encendido en coche no lo mate el sistema (con guía
  para el «autoinicio» de Xiaomi/Huawei/Samsung, etc.).
- **Selector de color manual** de los puntos en ajustes: «Por defecto» (adaptativo claro/oscuro) o
  «Personalizado», con paleta de colores y ajuste fino RGB. Se modifica solo el **relleno**; el
  **halo/contorno** se mantiene automático para que el punto siga viéndose sobre cualquier fondo.
  El cálculo de relleno+halo se centraliza en `DotColors` (usado por el overlay y la vista previa).
- **Detección automática de "voy en coche"** (`DetectionService` + `VehicleClassifier`): enciende
  los indicadores solo al detectar un vehículo (apagado manual). **Selector de 3 modos offline**:
  *Batería* (movimiento significativo + acelerómetro, mínimo consumo, por defecto), *Equilibrado* y
  *Máxima precisión* (con GPS de `LocationManager`, sin Google). Se **adapta al ahorro de batería**
  del sistema (fuerza el modo Batería) y la lógica de clasificación es pura y testeada.
- **Autoinicio al encender el teléfono** (`BootReceiver`): arranca la detección de forma invisible
  (sin overlay) y solo lo enciende al detectar coche.
- **Modo de prueba** en la pantalla principal (`MotionPreview`): vista previa con maniobras
  simuladas (acelerar, frenar, curvas) sobre fondo claro/oscuro, para ver y afinar el movimiento
  sin coche ni sensores.
- Ajuste de **amplitud del movimiento** (recorrido de los puntos) con slider, y conmutador de
  **color automático** claro/oscuro.
- **Tour de bienvenida en primera ejecución** (`OnboardingScreen`) con `HorizontalPager`:
  explicación breve → una página por permiso (overlay y notificaciones) con botón directo para
  concederlo → página para añadir el acceso rápido → página final de bienvenida. El estado se
  guarda en `onboardingDone` (DataStore) para no repetirlo.
- **Pantalla principal** rediseñada (`MainScreen`): tarjeta de estado ON/OFF con botón de
  activar/desactivar, aviso y botón para **añadir el acceso rápido** a los Ajustes rápidos
  (`StatusBarManager.requestAddTileService`, Android 13+) y los ajustes de aspecto, bordes e idioma.
- Utilidades de permisos centralizadas (`ui/Permissions.kt`): overlay, notificaciones y alta del tile.

- **Política de privacidad** bilingüe ([`PRIVACY.md`](PRIVACY.md)): la app no recopila datos.
- **Internacionalización (ES/EN)** con recursos `values/` (español) y `values-en/` (inglés).
- **Selector de idioma** en caliente desde los ajustes (Sistema / Español / English) mediante
  `AppCompatDelegate.setApplicationLocales` y `locales_config.xml`.
- **Tests unitarios** de la lógica de movimiento (`MotionMathTest`) sobre la nueva clase pura
  `MotionMath` (extraída de `MotionEngine`).
- **Icono de la app rediseñado**: icono adaptativo vectorial con fondo en degradado, pantalla con
  puntos de movimiento en los bordes y variante monocroma para iconos temáticos.

### Cambiado
- **Overlay a la máxima capa posible y edge-to-edge:** el overlay usa `TYPE_APPLICATION_OVERLAY`
  (la capa más alta disponible para una app de terceros), con aceleración por hardware y cobertura
  de la zona del notch/cutout. Se blinda el cálculo de *insets* para que el borde superior nunca
  quede oculto tras la barra de estado (en overlays el sistema a veces reporta inset 0).
  Nota: por seguridad de Android, ninguna app de terceros puede dibujar por encima de la barra de
  estado/navegación ni del panel de notificaciones (eso solo lo pueden las apps de sistema).
- **Compensación de orientación con el vector de rotación** (`TYPE_GAME_ROTATION_VECTOR`, con
  fallback a `TYPE_ROTATION_VECTOR`): `MotionEngine` proyecta la aceleración horizontal real sobre
  los ejes de la pantalla teniendo en cuenta la inclinación del teléfono y la rotación del display,
  así acelerar/frenar/curvas se mapean bien se sujete como se sujete. Lógica pura en
  `MotionMath.screenComponents`, con tests. (Sustituye al heurístico anterior basado en el
  giroscopio.)
- `MotionEngine` ahora reutiliza `MotionMath` (filtro paso-bajo y mapeo de ejes), eliminando la
  duplicación y haciendo la lógica testeable.
- Sincronizado el proyecto con Android Studio: wrapper a **Gradle 9.4.1**, catálogo de versiones
  ampliado (Material, AndroidX Test) y `testInstrumentationRunner` configurado.
- El icono usa solo el formato adaptativo vectorial (eliminados los `webp` por densidad y el tema
  noche autogenerado, redundante con `Theme.AppCompat.DayNight`).
- La notificación del servicio en primer plano pasa a un canal **IMPORTANCE_MIN** (silenciosa, sin
  icono en la barra de estado y minimizada). Android exige una notificación mientras el servicio
  está activo, pero ahora es prácticamente invisible.

### Corregido
- **Ahorro de batería:** el overlay **pausa los sensores y el dibujado cuando la pantalla se apaga**
  y los reanuda al encenderla (antes seguían activos sin sentido).
- **Auto-cero del sensor:** `MotionEngine` resta una línea base lenta (sesgo/deriva del acelerómetro)
  para que los puntos no queden desplazados de forma permanente; conserva las aceleraciones reales.
- Los puntos ahora tienen **mayor recorrido** (configurable) y un **contorno/halo** contrastado
  para verse sobre cualquier fondo (un overlay no puede leer los píxeles del fondo real).
- Los puntos respetan los **insets de las barras del sistema**, de modo que la barra de estado o de
  navegación ya no los oculta.
- El **acceso rápido (tile)** ahora refleja correctamente su estado al pulsarlo: antes se quedaba
  encendido visualmente porque se leía el flag `isRunning` del servicio (asíncrono) demasiado
  pronto. Se actualiza de inmediato y el servicio refresca el tile (`requestListeningState`) al
  cambiar de estado, manteniéndolo sincronizado también cuando se activa desde la app.

## [0.1.0] - 2026-06-19

### Añadido
- Estructura inicial del repositorio y archivos maestros (`.gitignore`, `LICENSE` MIT,
  `README.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `CLAUDE.md`, `.editorconfig`,
  `.gitattributes`).
- Documentación del proyecto en `docs/` (la ciencia de la cinetosis y la arquitectura técnica).
- Esqueleto del proyecto Android en `Código/` con Gradle (Kotlin DSL) y catálogo de versiones.
- Pantalla de ajustes con Jetpack Compose + Material 3 (`MainActivity`).
- Servicio de **overlay del sistema** que dibuja los indicadores de movimiento sobre otras apps
  (`OverlayService` + `DotsView`).
- **Motor de movimiento** que fusiona acelerómetro lineal y giroscopio con filtrado de señal
  (`MotionEngine`).
- **Quick Settings Tile** para activar/desactivar desde la barra de notificaciones
  (`CinetosisTileService`).
- Persistencia de preferencias con Jetpack DataStore (`SettingsRepository`).

[No publicado]: https://github.com/TheWolf1724/Cinetosis/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/TheWolf1724/Cinetosis/releases/tag/v0.1.0
