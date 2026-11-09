# MyStuff

Android-App zum Sammeln von **0-Euro-Souvenirscheinen**.

- Schein fotografieren: Titel und Serie/Nummer werden per Texterkennung (ML Kit, direkt auf dem Handy) gelesen
- Ort selbst eintragen, alles vor dem Speichern korrigierbar
- Suchen nach Ort, Titel, Serie oder Nummer
- Sortieren nach **Ort** oder **Serie**; Antippen zeigt alle Scheine dieser Gruppe
- Lange drücken auf einen Schein = löschen
- Daten bleiben lokal auf dem Handy (`scheine.json`)

## APK bekommen (ohne Android Studio)

1. Projekt auf GitHub hochladen (siehe unten).
2. Im Repository oben auf **Actions** klicken.
3. Den Lauf **APK bauen** öffnen (dauert ca. 3–5 Minuten).
4. Unten bei **Artifacts** `MyStuff-debug-apk` herunterladen (ZIP), entpacken, `app-debug.apk` aufs Handy kopieren und installieren.
   (Android fragt eventuell nach "Installation aus unbekannten Quellen" – erlauben.)

## Mit Android Studio öffnen

**File → Open** → diesen Ordner wählen → warten bis Sync fertig ist → Play-Knopf.

## Technik

Kotlin, minSdk 26, AGP 8.5.2, Gradle 8.7 (wird vom GitHub-Workflow bereitgestellt), Java 17.
