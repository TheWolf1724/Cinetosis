# CLAUDE.md — Instrucciones maestras del proyecto Cinetosis

Este archivo guía a Claude Code (y a cualquier agente o colaborador) al trabajar en este
repositorio. Léelo antes de hacer cambios.

## Qué es este proyecto

App Android **libre y de código abierto** que reduce el mareo por movimiento (cinetosis)
mostrando **indicadores de movimiento** (puntos animados en los bordes de la pantalla) que
siguen en tiempo real las aceleraciones del vehículo, imitando la función de accesibilidad de
Apple. Detalle de la idea en [`docs/CIENCIA.md`](docs/CIENCIA.md) y de la técnica en
[`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md).

## Reglas innegociables

1. **Autoría de commits.** TODOS los commits los firma **TheWolf1724**:
   - `user.name = TheWolf1724`
   - `user.email = 145658463+TheWolf1724@users.noreply.github.com`
   - **NUNCA** añadas trailers `Co-Authored-By:` (ni de Claude, ni de cba1724, ni de nadie).
   - Que **no** aparezcan otros colaboradores en el historial.
2. **Coste cero.** Nada que genere coste en el repositorio: **sin GitHub Actions** ni workflows,
   sin servicios externos de pago, sin runners.
3. **Privacidad.** La app es **offline**: sin permiso de internet, sin telemetría, sin analítica.
   No añadas dependencias que rompan esto.
4. **El código de la app vive en `Código/`.** Mantén el `.gitignore` al día para no subir
   artefactos (`build/`, `.gradle/`, `local.properties`, keystores, APK/AAB…).

## Estructura

```
Cinetosis/
├── Código/          ← Proyecto Android Studio (Gradle Kotlin DSL)
│   └── app/src/main/java/com/thewolf1724/cinetosis/
│       ├── MainActivity.kt           Pantalla de ajustes (Compose)
│       ├── ui/                        Tema y componentes Compose
│       ├── service/OverlayService.kt  Servicio en primer plano + overlay
│       ├── overlay/DotsView.kt        Vista Canvas que dibuja los puntos
│       ├── tile/CinetosisTileService.kt  Quick Settings Tile
│       ├── motion/MotionEngine.kt     Fusión y filtrado de sensores
│       └── data/SettingsRepository.kt DataStore de preferencias
├── docs/
└── (archivos maestros del repo)
```

## Flujo de ramas

- `main`: estable. `dev`: desarrollo. Funcionalidades en `feature/*` que salen de `dev`.
- Trabaja en `dev` salvo que se indique lo contrario.

## Comandos útiles (dentro de `Código/`)

```bash
./gradlew assembleDebug   # compilar APK debug
./gradlew installDebug    # instalar en dispositivo conectado
./gradlew lint            # análisis estático
```

## Stack técnico

- Kotlin 2.0 · Jetpack Compose + Material 3 · DataStore
- `compileSdk`/`targetSdk` 35 · `minSdk` 26 (Android 8.0)
- Overlay con `WindowManager` (`TYPE_APPLICATION_OVERLAY`); Tile con `TileService`.

## Al terminar una tarea

- Actualiza `CHANGELOG.md` (sección «No publicado»).
- Verifica que compila y que el `.gitignore` sigue cubriendo los residuos.
- Commits con Conventional Commits y la autoría correcta (regla 1).
