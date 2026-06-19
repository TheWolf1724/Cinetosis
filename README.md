<div align="center">

# 🌀 Cinetosis

### Reduce el mareo por movimiento en el coche con indicadores visuales inteligentes

*Una alternativa libre y de código abierto a la función «Indicadores de movimiento del vehículo» de Apple, para Android.*

[![Licencia: MIT](https://img.shields.io/badge/Licencia-MIT-blue.svg)](LICENSE)
[![Plataforma](https://img.shields.io/badge/Plataforma-Android%208.0%2B-3DDC84?logo=android&logoColor=white)](#requisitos)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](#tecnología)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](#tecnología)

</div>

---

## ¿Qué es el mareo por movimiento (cinetosis)?

La cinetosis aparece por un **conflicto sensorial**: tu **oído interno** (sistema vestibular)
detecta que el vehículo acelera, frena o gira, pero tus **ojos**, fijos en una pantalla
aparentemente estática, le dicen al cerebro que estás quieto. Esa contradicción entre lo que
*sientes* y lo que *ves* es lo que provoca las náuseas.

## ¿Cómo ayuda Cinetosis?

Cinetosis dibuja **puntos animados en los bordes de la pantalla** que se mueven en **tiempo real**
siguiendo las aceleraciones y los cambios de dirección reales del vehículo, medidos con los
sensores del teléfono. Así la información **visual coincide con la que percibe tu cuerpo**,
reduciendo el conflicto sensorial y, con ello, el mareo.

- El coche **acelera** → los puntos se desplazan hacia **atrás**.
- El coche **frena** → los puntos se desplazan hacia **delante**.
- El coche **gira** → los puntos se desplazan **lateralmente**.

> Inspirado en el estudio y la función de accesibilidad de Apple, reimplementado de forma
> independiente y abierta para Android.

## Características

- 🎯 **Indicadores de movimiento en tiempo real** a partir de acelerómetro + giroscopio, con
  fusión y filtrado de señal para que respondan al movimiento real y no al ruido del sensor.
- 🪟 **Overlay del sistema:** los puntos se muestran **por encima de cualquier app** (mapas,
  lector de libros, mensajería…), igual que la solución de Apple.
- ⚡ **Acceso rápido (Quick Settings Tile):** activa o desactiva los indicadores desde la
  **barra de notificaciones**, sin abrir la app.
- ⚙️ **Pantalla de ajustes** para personalizar sensibilidad, número y tamaño de los puntos,
  color/tema y bordes activos.
- 🔒 **100 % offline y sin telemetría:** no pide internet, no recopila datos, no envía nada a
  ningún servidor. Solo usa los sensores de movimiento del dispositivo. Ver
  [política de privacidad](PRIVACY.md).
- 🌍 **Bilingüe (español / inglés)** con selector de idioma integrado en los ajustes.
- 🚗 **Auto-encendido en coche (opcional):** detecta que vas en un vehículo y enciende los
  indicadores solo. Tres modos **offline**: *Batería* (sin GPS, mínimo consumo), *Equilibrado* y
  *Máxima precisión* (GPS). Se adapta al **ahorro de batería** del sistema y puede **arrancar al
  encender el móvil** (de forma invisible hasta detectar coche). El apagado es manual.
- 🆓 **Libre y gratuita** (MIT).

## Requisitos

- **Android 8.0 (API 26)** o superior.
- Sensores de **acelerómetro** y **giroscopio** (presentes en la mayoría de teléfonos).
- Permiso **«Mostrar sobre otras aplicaciones»** (necesario para el overlay del sistema).

## Tecnología

| Área            | Elección                                            |
|-----------------|-----------------------------------------------------|
| Lenguaje        | Kotlin 2.0                                           |
| UI              | Jetpack Compose + Material 3                         |
| Overlay         | `WindowManager` (`TYPE_APPLICATION_OVERLAY`)         |
| Acceso rápido   | `TileService` (Quick Settings Tile)                  |
| Sensores        | `SensorManager` (acelerómetro lineal + giroscopio)   |
| Persistencia    | Jetpack DataStore (Preferences)                      |
| Build           | Gradle (Kotlin DSL) + Android Gradle Plugin          |

## Estructura del repositorio

```
Cinetosis/
├── Código/          ← Proyecto de Android Studio (la app)
├── docs/            ← Documentación (la ciencia y la arquitectura)
├── CHANGELOG.md     ← Historial de cambios
├── CONTRIBUTING.md  ← Cómo contribuir
├── CLAUDE.md        ← Instrucciones maestras del proyecto
└── LICENSE          ← Licencia MIT
```

## Cómo compilar

1. Instala **Android Studio** (ver guía de entorno más abajo).
2. Abre la carpeta **`Código/`** como proyecto en Android Studio.
3. Deja que Gradle sincronice y descargue el SDK/dependencias.
4. Conecta un dispositivo o emulador con Android 8.0+ y pulsa **Run ▶**.

Desde línea de comandos (dentro de `Código/`):

```bash
./gradlew assembleDebug      # genera el APK de depuración
./gradlew installDebug       # lo instala en el dispositivo conectado
```

## Uso

1. Abre la app y concede el permiso **«Mostrar sobre otras apps»**.
2. Ajusta sensibilidad y aspecto a tu gusto.
3. Activa los indicadores desde la app **o** desde el **acceso rápido** de la barra de
   notificaciones.
4. Coloca el teléfono donde lo vayas a mirar durante el trayecto. Los puntos seguirán el
   movimiento del vehículo.

## Aviso

Cinetosis es una **ayuda**, no un dispositivo médico. La eficacia varía entre personas. Si los
síntomas persisten, descansa la vista y mira al horizonte. No la uses mientras conduces.

## Contribuir

¡Las contribuciones son bienvenidas! Lee [CONTRIBUTING.md](CONTRIBUTING.md) y revisa el
[CHANGELOG.md](CHANGELOG.md). Trabaja siempre sobre la rama **`dev`**.

## Licencia

Distribuido bajo licencia **MIT**. Consulta [LICENSE](LICENSE).

<div align="center">
<sub>Hecho con ❤️ para viajar sin mareos · © 2026 TheWolf1724</sub>
</div>
