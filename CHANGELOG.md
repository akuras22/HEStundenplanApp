# Änderungen

Hier steht in verständlicher Sprache, was sich von Version zu Version geändert hat — nicht nur für Entwickler.

## Version 1.5.0

- **Einstellungen neu aufgeteilt**: eigene Seiten für Studiengänge, Benachrichtigungen, Darstellung und "Über die App", statt einer langen Liste.
- **Neue Optionen**: Hell/Dunkel/System, dynamische Farben, Erinnerungszeit (15/20/30 Min.), manuelle Update-Prüfung, Zwischenspeicher leeren.
- **Pfeil-Symbol beim Studiengang-Wechsel**, wenn mehrere Favoriten gesetzt sind.
- **Update-Hinweis scrollt jetzt**, statt bei langen Änderungsprotokollen abgeschnitten zu werden.

## Version 1.0.24

- **Weniger doppelte Download-Dateien**: Jedes Update hieß bisher exakt gleich ("HEStundenplan.apk"), wodurch der Browser bei mehrfachem Herunterladen automatisch "(1)", "(2)" usw. anhängen musste. Der Dateiname enthält jetzt die Versionsnummer, wodurch das nicht mehr passiert. Die heruntergeladene Datei bleibt nach der Installation trotzdem in den Downloads liegen — die App kann sie aus denselben Gründen wie beim letzten Mal (Play Protect) nicht selbst löschen, das lässt sich aber jederzeit manuell in den Downloads erledigen.

## Version 1.0.23

- **Barrierefreiheit verbessert**: Die untere Navigationsleiste und die Kopfzeilen-Symbole (Heute, Datum wählen, Aktualisieren, Einstellungen) werden von Vorlesefunktionen wie TalkBack jetzt korrekt als ein einzelnes, klar benanntes Element erkannt statt als zwei getrennte.
- **Kleinere App**: Die App ist jetzt deutlich kleiner (App-interne Optimierung), ohne dass sich am Verhalten etwas ändert.

## Version 1.0.22

- **Homescreen-Widget**: Neues Widget, das die nächste anstehende Veranstaltung anzeigt (Titel, Uhrzeit, Raum), ohne die App öffnen zu müssen. Zum Startbildschirm hinzufügen wie jedes andere Widget auch. Aktualisiert sich automatisch, sobald die App im Hintergrund neue Daten lädt.

## Version 1.0.21

- **Vorlesungserinnerungen**: Neuer Schalter in den Einstellungen — einmal aktiviert, meldet sich die App etwa 15 Minuten bevor eine Veranstaltung beginnt, inklusive Raum. Ausgeblendete Veranstaltungen (siehe oben) werden dabei übersprungen. Beim ersten Aktivieren fragt Android einmalig nach der Benachrichtigungs-Erlaubnis.

## Version 1.0.20

- **Stundenplan als Kalenderdatei exportieren**: In den Einstellungen gibt es jetzt oben ein Kalender-Symbol, das den aktuell geladenen Stundenplan als .ics-Datei zum Import in eine beliebige Kalender-App (Google Kalender, Samsung Kalender, …) freigibt — inklusive aller Wochentermine für das ganze Semester, nicht nur die aktuell sichtbare Woche.

## Version 1.0.19

- **Veranstaltungen nach 19 Uhr werden nicht mehr abgeschnitten**: Das Zeitraster ging bisher immer von 8 bis 19 Uhr. Ein Studiengang mit einem Termin bis 21 Uhr wäre am unteren Rand einfach verschwunden. Das Raster passt sich jetzt automatisch an, wenn eine Veranstaltung früher beginnt oder später endet.
- **Update-Prüfung jetzt bei jedem Start, aber schonend**: Die App prüft jetzt bei jedem Start auf ein neues Update, fragt aber höchstens einmal pro Minute wirklich bei GitHub nach, falls die App mehrfach hintereinander geöffnet wird.
- **Offline-Ansicht**: Wenn keine Verbindung zur Hochschul-Seite besteht, zeigt die App jetzt den zuletzt erfolgreich geladenen Stand der aktuellen Woche mit einem "Offline"-Hinweis inklusive Zeitstempel, statt nur eine Fehlermeldung.
- **Verständlichere Fehlermeldungen**: Netzwerkfehler zeigen jetzt Klartext ("Keine Internetverbindung.") statt technischer Meldungen mit HTTP-Codes und URLs.
- **Einzelne Veranstaltungen ausblenden**: Wenn mehrere parallele Gruppen gleichzeitig stattfinden (z. B. drei Tutoriumsgruppen) und man nur in einer davon ist, lassen sich die anderen jetzt über den Detaildialog dauerhaft ausblenden. Rückgängig machen geht über die Einstellungen.
- **Mehrere Studiengänge als Favoriten**: In den Einstellungen lässt sich jeder Studiengang mit einem Stern markieren. Sind mehrere Favoriten gesetzt, kann oben durch Antippen des Titels schnell zwischen ihnen gewechselt werden, ohne den ganzen Studiengang neu zu suchen.

## Version 1.0.18

- **Überlappung jetzt in beide Richtungen sichtbar**: Drückt man beide Bereiche gleichzeitig, war die hellere Schnittfläche in der Mitte bisher nur zu sehen, wenn der linke Bereich aktiv war. Grund: Die Auswahlfläche war deckend gefüllt und hat den Druck-Effekt des Nachbarn überdeckt, sobald sie darüber gezeichnet wurde. Sie ist jetzt — wie bei Samsung — leicht durchscheinend, dadurch addieren sich die Flächen unabhängig von der Reihenfolge und es sieht in beide Richtungen gleich aus.
- **Abstand zum unteren Bildschirmrand korrigiert**: Die Leiste saß rund 3 mm zu weit oben und sitzt jetzt genauso tief wie die von Samsung.

## Version 1.0.17

- **Untere Leiste in der richtigen Größe**: Sie war rund 6,5 % zu groß, weil bei der letzten Messung von einer falschen Bildschirmdichte des Geräts ausgegangen wurde. Diesmal wurden App und Samsung Wallet in Screenshots vom selben Gerät direkt in Pixeln verglichen — dadurch entfällt die Umrechnung als Fehlerquelle komplett. Höhe, Breite, Innenabstände und Eckenrundung stimmen jetzt auf ein Zehntel Millimeter.
- **Auswahlflächen überlappen sich jetzt wie bei Samsung**: Die beiden Bereiche sind bei Samsung absichtlich etwas breiter als die Hälfte und überlappen sich in der Mitte. Sichtbar wird das nur, wenn man beide gleichzeitig drückt — genau daran ließ es sich nachmessen.
- **Auswahl kann nicht mehr aus der Leiste herausragen**: Die Höhe der Auswahlfläche ist jetzt fest vorgegeben, statt sich aus der Schriftgröße zu ergeben. Dadurch stand sie vorher je nach Schrift oben und unten über den Rand der Leiste hinaus.

## Version 1.0.16

- **Untere Leiste jetzt wirklich wie bei Samsung**: Statt weiter nach Augenmaß nachzubauen, wurden die Maße und Farben diesmal Pixel für Pixel aus einer Bildschirmaufnahme von Samsung Wallet ausgemessen. Dabei kam heraus, dass mehrere bisherige Annahmen schlicht falsch waren:
  - Der aktive Tab ist bei Samsung **grau**, nicht farbig — die blaue Hervorhebung der letzten Version gab es dort nie.
  - Die Symbole wechseln zwischen **ausgefüllt** (aktiv) und **nur umrandet** (inaktiv). Genau das macht bei Samsung den Unterschied aus, zusammen mit der fetteren Schrift — nicht die Farbe.
  - Beide Bereiche sind **exakt gleich breit**, unabhängig davon wie lang die Beschriftung ist.
  - Der Wechsel passiert **sofort**, ganz ohne Animation.
- Größen, Abstände, Eckenrundung und Farbtöne stimmen jetzt auf ein Zehntel Millimeter genau mit Samsungs eigener Leiste überein.

## Version 1.0.15

- **Navigationsleiste nach echten Samsung-Vorlagen neu gebaut**: Anhand einer öffentlich einsehbaren Nachbildung von Samsungs eigener Oberflächen-Bibliothek (statt nur Screenshot-Vergleich) neu umgesetzt: fester, einfarbiger Hintergrund statt Weichzeichner-Effekt, und der aktive Tab ("Woche"/"Tag") wird jetzt vollflächig in der App-Akzentfarbe hervorgehoben, mit den exakten Text- und Hintergrundfarben aus Samsungs eigenem Design.

## Version 1.0.14

- **Absturz beim Update behoben**: Ein Tipp auf "Update herunterladen" hat die App zum Absturz gebracht, statt den Browser zu öffnen. Behoben.

## Version 1.0.13

- **Eigentliche Ursache für die Farbabweichung zu Samsung gefunden**: Die App hat bisher Androids "Material You"-Farbsystem genutzt, das Grautöne (u. a. die Hervorhebung in der unteren Navigationsleiste) nach dem Hintergrundbild des Handys einfärbt. Samsungs eigene Apps (z. B. Wallet) machen das nicht — die nutzen feste, neutrale Grautöne unabhängig vom Hintergrundbild. Die App tut das jetzt auch, wodurch Farben durchgehend näher an Samsungs eigenem Look liegen sollten, nicht nur in der Navigationsleiste.
- **Hervorhebung in der Navigationsleiste kontrastreicher**: Der helle Hintergrund hinter dem aktiven Tab ("Woche"/"Tag") hebt sich jetzt deutlicher ab, ähnlich wie in Samsung Wallet.

## Version 1.0.12

- **Update-Installation zuverlässiger gemacht**: Google Play Protect hat die App zuletzt als "möglicherweise schädlich" blockiert, weil sie selbst eine neue Version heruntergeladen und installiert hat — ein Verhalten, das Play Protect grundsätzlich misstrauisch macht, egal wie harmlos die App tatsächlich ist. "Update herunterladen" öffnet jetzt stattdessen den Browser, der die Installation übernimmt — von dort aus sollte es nicht mehr blockiert werden.
- **Untere Navigationsleiste noch genauer an Samsung angeglichen**: "Woche"/"Tag" sehen jetzt 1:1 wie die Leiste in Samsung Wallet aus, inklusive des dezenten hellen Hintergrunds hinter dem aktiven Tab.

## Version 1.0.11

- **Gitternetz im Untis-Stil**: Die Linien für Stunden und Tage sind jetzt deutlich kräftiger und wirken wie ein echtes Raster, ähnlich wie man es aus Untis kennt. Die Markierungslinie für Veranstaltungen, die nicht zur vollen Stunde beginnen, ist jetzt gestrichelt statt farbig — passt sich damit ins Raster ein, statt wie ein separater bunter Hinweis zu wirken.
- **Untere Navigationsleiste an Samsung angepasst**: "Woche"/"Tag" unten sehen jetzt genauso aus wie die Navigationsleisten in Samsungs eigenen Apps (z. B. Telefon, Galerie) — ohne auffälligen Hintergrund-Pfeil hinter dem aktiven Tab, nur durch hellere Farbe und Schrift hervorgehoben.

## Version 1.0.10

- **Gitternetz zurückgesetzt, dafür markiert wo eine Veranstaltung genau beginnt**: Die generell hellere Gitternetz-Optik aus Version 1.0.9 wurde wieder zurückgenommen (war anders gemeint) — stattdessen gibt es jetzt genau an den Stellen, wo eine Veranstaltung nicht zur vollen Stunde beginnt (z. B. 11:10 Uhr), eine eigene farbige Linie quer durch die Ansicht, passend zur Uhrzeit links.

## Version 1.0.9

- **Gitternetz deutlich sichtbarer**: Die Linien für Stunden und Tage in der Wochen- und Tagesansicht waren kaum zu erkennen. Jetzt ist auf einen Blick klar, wo eine Stunde beginnt und wo ein Tag endet.
- **Überlappende Termine besser lesbar**: Wenn drei Veranstaltungen gleichzeitig stattfinden (z. B. drei Tutoriumsgruppen), wurde der Text vorher in einzelne Buchstaben zerhackt. Der Text passt sich jetzt an die verfügbare Breite an, und bei sehr engen Spalten wird der Raum ausgeblendet, damit der Titel lesbar bleibt (den Raum sieht man weiterhin beim Antippen).
- **Tagesansicht optisch an die Wochenansicht angeglichen**: Die Tagesauswahl oben (Mo, Di, Mi, …) ist jetzt exakt so ausgerichtet wie in der Wochenansicht, mit denselben Abständen und Spaltenbreiten.

## Version 1.0.8

- **Update-Hinweis besser lesbar**: Die Formatierung in der "Update verfügbar"-Meldung (fett gedruckte Begriffe, Aufzählungspunkte) wird jetzt wirklich als Formatierung angezeigt, statt die Sternchen (**) wörtlich mit auszugeben.
- **Aufgeräumter Start-Bildschirm**: Solange noch kein Studiengang ausgewählt ist, zeigt die App oben nur noch das Zahnrad (Einstellungen) an — die anderen Knöpfe (Heute, Datum wählen, Aktualisieren) hätten sowieso nichts zu tun, ohne dass ein Stundenplan geladen ist.

## Version 1.0.7

- **Aktiver Tag in der Tagesansicht repariert**: Beim schnellen Durchwischen mehrerer Tage konnte die Markierung oben (welcher Tag gerade ausgewählt ist) hängen bleiben und nicht mehr zum tatsächlich angezeigten Tag passen. Das ist jetzt behoben.
- **Genaue Startzeiten in der Zeitleiste**: Links werden weiterhin die vollen Stunden angezeigt (8:00, 9:00, …), zusätzlich aber jetzt auch die genaue Uhrzeit, wenn eine Veranstaltung nicht zur vollen Stunde beginnt (z. B. 11:10 Uhr) — auf einen Blick erkennbar, ohne nachzählen zu müssen.
- **Titel oben repariert**: Wenn noch kein Studiengang ausgewählt war, konnte der Titel "Stundenplan" abgeschnitten aussehen oder die Symbole rechts (Einstellungen usw.) verdrängen. Der Titel passt sich jetzt automatisch in der Größe an, statt abgeschnitten zu werden.

## Version 1.0 – Erste Version

- **Stundenplan direkt von der Hochschule Esslingen**, immer aktuell für den gewählten Studiengang.
- **Wochen- und Tagesansicht** mit Wischen zum Blättern zwischen Wochen bzw. Tagen.
- **Tutorien werden jetzt zuverlässig angezeigt** — vorher fehlten sie in bestimmten Wochen, weil sie in einem anderen Layout auf der Hochschul-Seite standen.
- **Überlappende Veranstaltungen** (z. B. Vorlesung und Tutorium zur gleichen Zeit) werden übersichtlich nebeneinander statt übereinander dargestellt.
- **Deutlich flüssigeres Wischen** zwischen den Wochen.
- **Automatische Updates**: Die App informiert direkt, wenn eine neue Version verfügbar ist, und installiert sie mit einem Tipp.
- **Detailansicht** für jede Veranstaltung (Raum, Dozent, Zeitraum, Turnus) per Antippen.
