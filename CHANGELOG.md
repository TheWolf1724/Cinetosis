# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/)
y el proyecto sigue el [Versionado Semántico](https://semver.org/lang/es/).

## [No publicado]

### Añadido
- **Política de privacidad** bilingüe ([`PRIVACY.md`](PRIVACY.md)): la app no recopila datos.
- **Internacionalización (ES/EN)** con recursos `values/` (español) y `values-en/` (inglés).
- **Selector de idioma** en caliente desde los ajustes (Sistema / Español / English) mediante
  `AppCompatDelegate.setApplicationLocales` y `locales_config.xml`.
- **Tests unitarios** de la lógica de movimiento (`MotionMathTest`) sobre la nueva clase pura
  `MotionMath` (extraída de `MotionEngine`).
- **Icono de la app rediseñado**: icono adaptativo vectorial con fondo en degradado, pantalla con
  puntos de movimiento en los bordes y variante monocroma para iconos temáticos.

### Cambiado
- `MotionEngine` ahora reutiliza `MotionMath` (filtro paso-bajo y mapeo de ejes), eliminando la
  duplicación y haciendo la lógica testeable.

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
