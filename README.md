# JobTrail

## Architekturentscheidung: JWT

Für die Authentifizierung verwendet JobTrail JWT-Bearer-Tokens. Damit bleibt die REST-API zustandslos und kann ohne serverseitige Sessions genutzt und skaliert werden. Als Konsequenz müssen kurze Token-Laufzeiten und ein sicherer Umgang mit dem Signaturschlüssel berücksichtigt werden.

## Profil und KI-Job-Matching

Alle Endpunkte benötigen einen JWT-Bearer-Token:

- `POST /api/profiles` – Profil anlegen
- `GET /api/profiles` – eigenes Profil abrufen
- `PUT /api/profiles` – eigenes Profil vollständig aktualisieren
- `DELETE /api/profiles` – eigenes Profil löschen
- `POST /api/job-match/analyze` – Stellenbeschreibung mit dem eigenen Profil vergleichen

Ein Profil enthält Zielrolle, Standortwunsch, Verfügbarkeit, Erfahrungsniveau, Kurzprofil,
Skills, Sprachen, Projekte, Wunschrollen und Ausschlussbegriffe. Vor einem KI-Matching muss ein
Profil existieren. Das Ergebnis enthält einen Score von 0 bis 100, passende und fehlende Skills,
eine Empfehlung (`Apply`, `Maybe` oder `Do not apply`) und eine Kurzbegründung.

Für die KI-Funktion muss `OPENAI_API_KEY` gesetzt sein. Standardmäßig wird `gpt-4o-mini`
verwendet. Der Schlüssel gehört ausschließlich in die Umgebung und darf nicht committed werden.
Die Antwort wird über OpenAIs natives JSON-Schema validiert. Provider-Aufrufe haben ein Timeout
von 30 Sekunden, maximal drei Versuche und eine Ausgabegrenze von 700 Tokens. Pro Benutzer sind
standardmäßig fünf Analysen pro Minute erlaubt; der Wert kann mit
`JOB_MATCH_REQUESTS_PER_MINUTE` angepasst werden.

Der normale Testlauf verwendet keinen externen Dienst. Ein echter OpenAI-Integrationstest läuft
automatisch mit, sobald `OPENAI_API_KEY` in der Umgebung gesetzt ist:

```shell
./mvnw test
```

Beispiel:

```http
POST /api/job-match/analyze
Authorization: Bearer <token>
Content-Type: application/json

{
  "description": "Gesucht wird ein Java Backend Developer mit Spring Boot und Docker."
}
```
