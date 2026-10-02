# HEStundenplan

Inoffizieller Stundenplan der Hochschule Esslingen — live von der öffentlichen QIS/LSF-Seite
geladen, mit Wochen-/Tagesansicht, Suche, Vorlesungs-Erinnerungen und anpassbarem Design. Dazu
der Mensa-Speiseplan des Studierendenwerks Stuttgart mit Fotos, Preisen und Allergenen.

Es gibt zwei Apps in diesem Repository:

| | Plattform | Verzeichnis | Technologie |
|---|---|---|---|
| 📱 **Android** | Android 8.0+ | [`app/`](app) | Kotlin, Jetpack Compose |
| 🖥️ **Desktop** | Linux (GNOME-Look als Standard, KDE-Look unter KDE Plasma) | [`desktop/`](desktop) | C++, Qt6/Kirigami |

Beide Apps sprechen dieselbe öffentliche QIS/LSF-Schnittstelle der Hochschule an und bieten den
gleichen Funktionsumfang (Wochen-/Tagesansicht, Studiengang-Auswahl mit Favoriten, Suche,
Vorlesungs-Erinnerungen, anpassbares Aussehen, Mensa-Speiseplan). Es gibt kein Backend und keinen
Login — alle Daten kommen live von der Hochschulseite bzw. vom
[Speiseplan des Studierendenwerks Stuttgart](https://www.studierendenwerk-stuttgart.de/essen/speiseplan)
(Standort wählbar: die drei Mensen der HS Esslingen und die übrigen des Studierendenwerks).

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

**Andere Distributionen — aus dem Quellcode bauen:** benötigt Qt6 ab 6.7 (Core, Gui, Qml, Quick,
QuickControls2, Network, DBus, Svg), KDE Frameworks 6 (Kirigami, KConfig, KCoreAddons, KI18n,
KNotifications, KDBusAddons), libxml2, sowie CMake, Ninja und extra-cmake-modules. Dazu je nach
Desktop (siehe [Zwei Looks](#zwei-looks-gnome-und-kde)): für den GNOME-Look das
**Adwaita-Icon-Theme**, für den KDE-Look **qqc2-desktop-style** und das **Breeze-Icon-Theme**
(`kirigami` zieht keines davon automatisch mit — ohne `qqc2-desktop-style` startet die App unter
Plasma nicht, ohne das jeweilige Icon-Theme bleiben Symbole leer):

```bash
git clone https://github.com/akuras22/HEStundenplanApp.git
cd HEStundenplanApp
cmake -S desktop -B build -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build build
sudo cmake --install build
```

Die App erscheint danach im Anwendungsmenü als "Stundenplan" (`hestundenplan-desktop`).

### Zwei Looks: GNOME und KDE

Die App erkennt beim Start, auf welchem Desktop sie läuft, und sieht entsprechend aus:

- **GNOME-Look (Standard)** — unter GNOME und jedem anderen Desktop außer Plasma. Eigene
  Kopfleiste mit Fensterknöpfen statt Titelleiste, abgerundete Fensterecken, Bedienelemente,
  Farben und Maße nach libadwaita-Vorbild, Adwaita-Symbole, Einstellungen als "Boxed Lists".
  Hell/Dunkel, Akzentfarbe und die Anordnung der Fensterknöpfe folgen live den
  GNOME-Einstellungen. Technisch ist das kein GTK: ein eigener Qt-Quick-Controls-Stil
  ([`desktop/qml/adwaita`](desktop/qml/adwaita)) zeichnet die Adwaita-Formen nach.
- **KDE-Look** — unter KDE Plasma. Breeze-Bedienelemente über den `org.kde.desktop`-Stil
  (`qqc2-desktop-style`), Kirigami-Werkzeugleiste, normale KWin-Titelleiste, Farbschema und
  Akzentfarbe von Plasma, Breeze-Symbole.

Zum Ausprobieren lässt sich die Erkennung übersteuern: `HESTUNDENPLAN_LOOK=kde` bzw.
`HESTUNDENPLAN_LOOK=gnome` erzwingt einen Look, `HESTUNDENPLAN_CSD=0` behält im GNOME-Look die
Titelleiste des Fenstermanagers statt der eigenen Fensterknöpfe.

### Bekannte Einschränkungen

- Vorlesungs-Erinnerungen laufen nur, solange die App geöffnet ist (kein Hintergrunddienst wie
  Androids WorkManager, kein Systemtray-Icon zum Weiterlaufen im Hintergrund).
- Theme (Hell/Dunkel) und Akzentfarbe folgen immer dem System — es gibt bewusst keine
  App-eigene Override-Einstellung dafür (in früheren Versionen versucht, aber nie zuverlässig
  über alle Bedienelemente hinweg wirksam).
- Im GNOME-Look hat das Fenster keinen Schlagschatten: den zeichnen GTK-Apps selbst in einen
  unsichtbaren Rand um das Fenster, wofür Qt keine Schnittstelle anbietet. Stattdessen umgibt
  das Fenster eine feine Randlinie.
- Kein Pendant zum Android-Homescreen-Widget.

## Lizenz

GPL-3.0, siehe [LICENSE](LICENSE).
