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

- **Ubicación (opcional, solo modos GPS):** si activas la detección automática en modo
  «Equilibrado» o «Máxima precisión», la app usa la **velocidad del GPS** para reconocer que vas en
  coche. Se procesa **en el dispositivo** y **nunca se transmite**. El modo por defecto («Batería»)
  **no** usa la ubicación.

### Qué datos NO usa la app
- ❌ No accede a Internet (la app no declara el permiso de red).
- ❌ No recopila datos de identificación, contactos, cámara ni micrófono.
- ❌ No usa la ubicación salvo en los modos de detección GPS, y siempre **en local**.
- ❌ No incluye analítica, publicidad, Google Play Services ni rastreadores de terceros.
- ❌ No comparte información con nadie.

### Permisos y su finalidad
- **Mostrar sobre otras apps (`SYSTEM_ALERT_WINDOW`)**: dibujar los puntos por encima de otras
  aplicaciones.
- **Servicio en primer plano (`FOREGROUND_SERVICE` / `..._SPECIAL_USE` / `..._LOCATION`)**: mantener
  activos el overlay/los sensores y la detección de "voy en coche".
- **Ubicación (`ACCESS_FINE_LOCATION`, opcional)**: solo para los modos de detección con GPS; en
  local, sin transmitir.
- **Autoinicio (`RECEIVE_BOOT_COMPLETED`)**: arrancar la detección (invisible) al encender el móvil,
  si lo activas.
- **Notificaciones (`POST_NOTIFICATIONS`)**: mostrar la notificación obligatoria del servicio en
  primer plano.

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
- **Location (optional, GPS modes only):** if you enable automatic detection in “Balanced” or
  “Maximum accuracy” mode, the app uses **GPS speed** to recognize you are in a vehicle. It is
  processed **on the device** and **never transmitted**. The default “Battery” mode does **not** use
  location.

### Data the app does NOT use
- ❌ No Internet access (the app does not declare the network permission).
- ❌ No identifiers, contacts, camera, or microphone.
- ❌ No location except in the GPS detection modes, always **on-device**.
- ❌ No analytics, ads, Google Play Services, or third-party trackers.
- ❌ No data shared with anyone.

### Permissions and their purpose
- **Display over other apps (`SYSTEM_ALERT_WINDOW`)**: draw the dots on top of other apps.
- **Foreground service (`FOREGROUND_SERVICE` / `..._SPECIAL_USE` / `..._LOCATION`)**: keep the
  overlay/sensors and the "in-vehicle" detection running.
- **Location (`ACCESS_FINE_LOCATION`, optional)**: only for the GPS detection modes; on-device,
  never transmitted.
- **Autostart (`RECEIVE_BOOT_COMPLETED`)**: start the (invisible) detection on boot, if you enable it.
- **Notifications (`POST_NOTIFICATIONS`)**: show the mandatory foreground-service notification.

### Children
The app is not directed at children and, since it collects no data, it processes no user
information.

### Changes
Any change to this policy will be reflected in this file within the repository.

### Contact
For any questions, open an issue on the GitHub repository:
<https://github.com/TheWolf1724/Cinetosis>
