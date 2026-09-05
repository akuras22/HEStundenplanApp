# HEStundenplan

Inoffizieller Stundenplan der Hochschule Esslingen — live von der öffentlichen QIS/LSF-Seite
geladen, mit Wochen-/Tagesansicht, Suche, Vorlesungs-Erinnerungen und anpassbarem Design.

Es gibt zwei Apps in diesem Repository:

| | Plattform | Verzeichnis | Technologie |
|---|---|---|---|
| 📱 **Android** | Android 8.0+ | [`app/`](app) | Kotlin, Jetpack Compose |
| 🖥️ **Desktop** | Linux (optimiert für KDE Plasma, z. B. CachyOS) | [`desktop/`](desktop) | C++, Qt6/Kirigami |

Beide Apps sprechen dieselbe öffentliche QIS/LSF-Schnittstelle der Hochschule an und bieten den
gleichen Funktionsumfang (Wochen-/Tagesansicht, Studiengang-Auswahl mit Favoriten, Suche,
Vorlesungs-Erinnerungen, anpassbares Aussehen). Es gibt kein Backend und keinen Login — alle Daten
kommen live von der Hochschulseite.

## Android

Signierte Release-APKs gibt es auf der [Releases-Seite](https://github.com/akuras22/HEStundenplanApp/releases) —
neuestes `HEStundenplan-*.apk` herunterladen und installieren (Chrome übernimmt Download +
Installation, siehe [CHANGELOG.md](CHANGELOG.md) für Details zum Update-Mechanismus).

Quellcode und Build-Anleitung: [`app/`](app), Standard-Gradle-Projekt (`./gradlew assembleDebug`).

## Desktop (Linux / KDE Plasma)

Native Qt6/Kirigami-App, siehe [`desktop/`](desktop) für den vollständigen Quellcode. Jeder Push
auf `main` baut die App automatisch und veröffentlicht sie auf der
[Releases-Seite](https://github.com/akuras22/HEStundenplanApp/releases) zusammen mit der
Android-APK.

### Installation

**Arch Linux / CachyOS (empfohlen):** fertiges Paket von der [neuesten
Release](https://github.com/akuras22/HEStundenplanApp/releases/latest) herunterladen
(`hestundenplan-desktop-*-x86_64.pkg.tar.zst`) und installieren:

```bash
sudo pacman -U hestundenplan-desktop-*-x86_64.pkg.tar.zst
```

**Arch Linux / CachyOS über PKGBUILD:** `desktop/packaging/PKGBUILD` herunterladen und selbst bauen:

```bash
curl -LO https://raw.githubusercontent.com/akuras22/HEStundenplanApp/main/desktop/packaging/PKGBUILD
makepkg -si
```

**Andere Distributionen — aus dem Quellcode bauen:** benötigt Qt6 (Core, Gui, Qml, Quick,
QuickControls2, Network), KDE Frameworks 6 (Kirigami, KConfig, KCoreAddons, KI18n, KNotifications,
KDBusAddons), libxml2, sowie CMake, Ninja und extra-cmake-modules:

```bash
git clone https://github.com/akuras22/HEStundenplanApp.git
cd HEStundenplanApp
cmake -S desktop -B build -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build build
sudo cmake --install build
```

Die App erscheint danach im Anwendungsmenü als "Stundenplan" (`hestundenplan-desktop`).

### Bekannte Einschränkungen

- Vorlesungs-Erinnerungen laufen nur, solange die App geöffnet ist (kein Hintergrunddienst wie
  Androids WorkManager).
- Theme (Hell/Dunkel) und Akzentfarbe lassen sich für die selbstgezeichneten Teile der Oberfläche
  (Wochen-/Tagesumschalter, Heute-Markierung) anpassen; native KDE-Bedienelemente (Dialoge,
  Checkboxen, Fensterrahmen) folgen bewusst immer dem System-Theme von Plasma.
- Kein Pendant zum Android-Homescreen-Widget.

## Lizenz

GPL-3.0, siehe [LICENSE](LICENSE).
