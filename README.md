# MyStuff

Android-App zum Sammeln von **0-Euro-Souvenirscheinen**.

- Schein fotografieren: Titel und Serie/Nummer werden per Texterkennung (ML Kit, direkt auf dem Handy) gelesen
- Ort selbst eintragen, alles vor dem Speichern korrigierbar
- Suchen nach Ort, Titel, Serie oder Nummer
- Sortieren nach **Ort** oder **Serie**; Antippen zeigt alle Scheine dieser Gruppe
- Lange drücken auf einen Schein = löschen
- Daten bleiben lokal auf dem Handy (`scheine.json`)

## Technik

Kotlin, minSdk 26, AGP 8.5.2, Gradle 8.7 (wird vom GitHub-Workflow bereitgestellt), Java 17.
