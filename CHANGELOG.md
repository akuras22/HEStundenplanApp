# Änderungen

Hier steht in verständlicher Sprache, was sich von Version zu Version geändert hat — nicht nur für Entwickler.

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
