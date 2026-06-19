# Guía de contribución

¡Gracias por tu interés en mejorar **Cinetosis**! Este documento explica cómo colaborar.

## Flujo de ramas

- **`main`** — rama estable. Solo recibe versiones probadas. No se hace push directo.
- **`dev`** — rama de desarrollo. Todo el trabajo nuevo parte de aquí.
- **`feature/<nombre>`** — ramas de trabajo que salen de `dev` y vuelven a `dev` por Pull Request.

```bash
git checkout dev
git pull
git checkout -b feature/mi-mejora
# ... trabajas y haces commits ...
git push -u origin feature/mi-mejora
# Abres un Pull Request contra dev
```

## Estilo de commits

Usamos **Conventional Commits**:

```
<tipo>(<ámbito opcional>): <descripción breve en imperativo>
```

Tipos habituales: `feat`, `fix`, `docs`, `refactor`, `perf`, `test`, `build`, `chore`.

Ejemplos:
- `feat(overlay): añade selección de bordes activos`
- `fix(motion): corrige deriva del giroscopio en reposo`
- `docs(readme): aclara los requisitos de permisos`

## Estilo de código

- **Kotlin** siguiendo las convenciones oficiales y el `.editorconfig` del repo.
- UI con **Jetpack Compose** + **Material 3**.
- Nombres y comentarios en español o inglés, pero **consistentes** dentro de cada archivo.
- Ejecuta el linter antes de subir: `./gradlew lint` (dentro de `Código/`).

## Antes de abrir un Pull Request

1. Que **compile**: `./gradlew assembleDebug`.
2. Que **pase el lint**: `./gradlew lint`.
3. Actualiza el **`CHANGELOG.md`** en la sección «No publicado».
4. Describe **qué** cambia y **por qué** en el PR.

## Buenas prácticas del proyecto

- **Nunca** subas `local.properties`, keystores, `*.jks`, ni la carpeta `build/`
  (ya están en `.gitignore`).
- La app es **offline y sin telemetría**: no añadas dependencias que requieran red ni que
  recopilen datos sin discusión previa.
- Nada que genere coste en el repositorio (sin GitHub Actions de pago, sin servicios externos).

## Reportar errores o proponer ideas

Abre un **Issue** describiendo el problema o la propuesta. Para errores, incluye modelo de
dispositivo, versión de Android y pasos para reproducirlo.

¡Gracias por contribuir! 🌀
