# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/)
y el proyecto sigue el [Versionado Semántico](https://semver.org/lang/es/).

## [No publicado]

### Añadido
- _En desarrollo en la rama `dev`._

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
