# HEStundenplan

Inoffizieller Stundenplan der Hochschule Esslingen — live von der öffentlichen QIS/LSF-Seite
geladen, mit Wochen-/Tagesansicht, Suche, Vorlesungs-Erinnerungen und anpassbarem Design.

Es gibt zwei Apps in diesem Repository:

| | Plattform | Verzeichnis | Technologie |
|---|---|---|---|
| 📱 **Android** | Android 8.0+ | [`app/`](app) | Kotlin, Jetpack Compose |
| 🖥️ **Desktop** | Linux (für KDE Plasma optimiert, z. B. CachyOS; läuft auch unter GNOME) | [`desktop/`](desktop) | C++, Qt6/Kirigami |

Beide Apps sprechen dieselbe öffentliche QIS/LSF-Schnittstelle der Hochschule an und bieten den
gleichen Funktionsumfang (Wochen-/Tagesansicht, Studiengang-Auswahl mit Favoriten, Suche,
Vorlesungs-Erinnerungen, anpassbares Aussehen). Es gibt kein Backend und keinen Login — alle Daten
kommen live von der Hochschulseite.

## Android

Signierte Release-APKs gibt es auf der [Releases-Seite](https://github.com/akuras22/HEStundenplanApp/releases) —
neuestes `HEStundenplan-*.apk` herunterladen und installieren (Chrome übernimmt Download +
Installation, siehe [CHANGELOG.md](CHANGELOG.md) für Details zum Update-Mechanismus).

Quellcode und Build-Anleitung: [`app/`](app), Standard-Gradle-Projekt (`./gradlew assembleDebug`).

## Desktop (Linux)

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

**Andere Distributionen (auch GNOME) — aus dem Quellcode bauen:** benötigt Qt6 (Core, Gui, Qml,
Quick, QuickControls2, Network), KDE Frameworks 6 (Kirigami, KConfig, KCoreAddons, KI18n,
KNotifications, KDBusAddons), zusätzlich **qqc2-desktop-style** und das **Breeze-Icon-Theme**
(beide zieht `kirigami` selbst nicht automatisch mit — ohne `qqc2-desktop-style` startet die App
auf einem reinen GNOME-System gar nicht erst, da der erzwungene `org.kde.desktop`-Stil fehlt; ohne
Breeze-Icons bleiben einige Symbolleisten-Icons unter Adwaita leer), libxml2, sowie CMake, Ninja
und extra-cmake-modules:

```bash
git clone https://github.com/akuras22/HEStundenplanApp.git
cd HEStundenplanApp
cmake -S desktop -B build -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build build
sudo cmake --install build
```

Die App erscheint danach im Anwendungsmenü als "Stundenplan" (`hestundenplan-desktop`).

### GNOME und andere Desktops

Die App ist mit [Kirigami](https://develop.kde.org/frameworks/kirigami/) gebaut, das bewusst
"konvergent" ist und außerhalb von KDE Plasma genauso läuft. Damit sie dabei nicht auf den kargen,
generischen Qt-Standardstil zurückfällt, erzwingt sie explizit den `org.kde.desktop`-Stil (aus
`qqc2-desktop-style`) — der ist trotz des Namens nicht Plasma-exklusiv, sondern für jeden
Linux-Desktop gedacht. Zusammen mit dem Breeze-Icon-Theme sorgt das für ein konsistentes,
KDE-artiges Erscheinungsbild sowohl unter Plasma als auch unter GNOME. Pacman zieht beide
Pakete automatisch mit; bei anderen Paketmanagern ggf. manuell nachinstallieren
(`qqc2-desktop-style` und `breeze-icons`/`breeze-icon-theme`).

### Bekannte Einschränkungen

- Vorlesungs-Erinnerungen laufen nur, solange die App geöffnet ist (kein Hintergrunddienst wie
  Androids WorkManager, kein Systemtray-Icon zum Weiterlaufen im Hintergrund).
- Theme (Hell/Dunkel) und Akzentfarbe folgen immer dem System — es gibt bewusst keine
  App-eigene Override-Einstellung dafür (in früheren Versionen versucht, aber nie zuverlässig
  über alle Bedienelemente hinweg wirksam).
- Kein Pendant zum Android-Homescreen-Widget.

## Lizenz

GPL-3.0, siehe [LICENSE](LICENSE).
