# Política de privacidad · Privacy Policy

> **Cinetosis** · Última actualización / Last updated: 2026-06-19

---

## 🇪🇸 Español

**Cinetosis no recopila, almacena ni comparte ningún dato personal.**

### Qué datos usa la app
- **Sensores de movimiento** (acelerómetro y giroscopio): se leen **en tiempo real** únicamente
  para animar los indicadores de movimiento en pantalla. **No se guardan, no se registran y no
  salen del dispositivo.**
- **Preferencias de la app** (sensibilidad, número y tamaño de los puntos, bordes activos,
  idioma): se guardan **solo en tu dispositivo** mediante el almacenamiento local de Android
  (DataStore). Nunca se envían a ningún servidor.
- **Detección de "voy en coche"** (opcional): usa **solo el acelerómetro** del dispositivo en
  comprobaciones cortas y periódicas, **en local**. No usa ubicación ni GPS.

### Qué datos NO usa la app
- ❌ No accede a Internet (la app no declara el permiso de red).
- ❌ No recopila datos de identificación, ubicación/GPS, contactos, cámara ni micrófono.
- ❌ No incluye analítica, publicidad, Google Play Services ni rastreadores de terceros.
- ❌ No comparte información con nadie.

### Permisos y su finalidad
- **Mostrar sobre otras apps (`SYSTEM_ALERT_WINDOW`)**: dibujar los puntos por encima de otras
  aplicaciones.
- **Servicio en primer plano (`FOREGROUND_SERVICE` / `..._SPECIAL_USE`)**: mantener el overlay y los
  sensores **solo mientras los indicadores están encendidos**.
- **Autoinicio (`RECEIVE_BOOT_COMPLETED`)**: programar la detección al encender el móvil, si lo activas.
- **Exención de batería (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)**: para que la detección periódica no
  la mate el sistema.
- **Notificaciones (`POST_NOTIFICATIONS`)**: mostrar la notificación del servicio en primer plano
  **solo mientras los indicadores están activos** (la detección en segundo plano no muestra ninguna).

### Niños
La app no está dirigida a menores y, al no recopilar datos, no trata información de ningún
usuario.

### Cambios
Cualquier cambio en esta política se reflejará en este archivo dentro del repositorio.

### Contacto
Para cualquier duda, abre un *issue* en el repositorio de GitHub:
<https://github.com/TheWolf1724/Cinetosis>

---

## 🇬🇧 English

**Cinetosis does not collect, store, or share any personal data.**

### Data the app uses
- **Motion sensors** (accelerometer and gyroscope): read **in real time** solely to animate the
  on-screen motion cues. **They are not saved, not logged, and never leave your device.**
- **App preferences** (sensitivity, dot count and size, active edges, language): stored **only on
  your device** using Android local storage (DataStore). They are never sent to any server.
- **"In-vehicle" detection** (optional): uses **only the accelerometer** in short periodic checks,
  **on-device**. It does not use location or GPS.

### Data the app does NOT use
- ❌ No Internet access (the app does not declare the network permission).
- ❌ No identifiers, location/GPS, contacts, camera, or microphone.
- ❌ No analytics, ads, Google Play Services, or third-party trackers.
- ❌ No data shared with anyone.

### Permissions and their purpose
- **Display over other apps (`SYSTEM_ALERT_WINDOW`)**: draw the dots on top of other apps.
- **Foreground service (`FOREGROUND_SERVICE` / `..._SPECIAL_USE`)**: keep the overlay and sensors
  running **only while the cues are on**.
- **Autostart (`RECEIVE_BOOT_COMPLETED`)**: schedule detection on boot, if you enable it.
- **Battery exemption (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)**: so the periodic detection isn't killed.
- **Notifications (`POST_NOTIFICATIONS`)**: show the foreground-service notification **only while the
  cues are active** (background detection shows none).

### Children
The app is not directed at children and, since it collects no data, it processes no user
information.

### Changes
Any change to this policy will be reflected in this file within the repository.

### Contact
For any questions, open an issue on the GitHub repository:
<https://github.com/TheWolf1724/Cinetosis>
