# 📮 JobTrail Postman Collection & Environment

Diese Sammlung enthält alle Endpunkte der JobTrail REST-API mit vorbereiteten Request-Bodies, Query-Parametern, Authentifizierung, automatischen Test-Skripten und umfassenden **Fehlerfällen** (Validierungsfehler, 404 Nicht gefunden, 409 Konflikte, 401 Nicht autorisiert).

---

## 📁 Enthaltene Dateien

* [`JobTrail.postman_collection.json`](./JobTrail.postman_collection.json) – Die Hauptsammlung mit allen Standard- und Fehler-Requests, Collection-Variablen und Auto-Auth-Skripten.
* [`JobTrail.postman_environment.json`](./JobTrail.postman_environment.json) – Optionales Postman Environment für lokale Tests (`http://localhost:8080`).

---

## 🚀 Import in Postman (Schritt für Schritt)

1. Öffne **Postman**.
2. Klicke oben links auf **Import**.
3. Ziehe die Datei `JobTrail.postman_collection.json` (und optional `JobTrail.postman_environment.json`) per Drag & Drop in das Fenster oder wähle sie über **Files** aus.
4. Klicke auf **Import**.

---

## ⚡ Schnelleinstieg: In 10 Sekunden alle Endpunkte testen

1. **Backend starten**: Stelle sicher, dass die JobTrail-Anwendung läuft (z. B. auf Port `8080`).
2. **Demo-Login ausführen**:
   * Öffne den Ordner **`1. Authentication`**.
   * Führe den Request **`1-Klick Demo-Login (Empfohlen)`** aus (`POST /api/auth/demo?reset=true`).
3. **Automatischer JWT-Handshake**:
   * Das hinterlegte Postman Test-Skript extrahiert den JWT-Token automatisch aus der Serverantwort und speichert ihn in der Collection-Variable `{{token}}`.
4. **Sofort startklar**:
   * Alle weiteren Endpunkte (**Companies**, **Applications**, **Profiles**, **Job Match AI**) erben diese Authentifizierung (`Bearer {{token}}`) automatisch und können sofort ohne manuelles Copy & Paste ausgeführt werden!

---

## 🗂️ Übersicht der enthaltenen Endpunkte & Fehlerfälle

### 1. Authentication
* `POST /api/auth/demo?reset=true` – 1-Klick Demo-Login (setzt saubere Testdaten auf und speichert JWT)
* `POST /api/auth/login` – Normaler Login mit E-Mail & Passwort (speichert JWT)
* `POST /api/auth/register` – Neuen Benutzer registrieren
* `GET /api/auth/me` – Eigene Benutzerinformationen abrufen
* ⚠️ **Fehlerfälle:**
  * `POST /api/auth/login` (401 Unauthorized) – Login mit ungültigem Passwort
  * `POST /api/auth/register` (400 Bad Request) – Ungültige E-Mail & Passwort zu kurz (< 8 Zeichen)
  * `POST /api/auth/register` (409 Conflict) – Registrierung einer bereits vergebenen E-Mail (`demo@jobtrail.dev`)

### 2. Companies
* `GET /api/companies` – Alle Unternehmen des Benutzers abrufen (speichert erstes `companyId`)
* `POST /api/companies` – Neues Unternehmen anlegen (speichert generiertes `companyId`)
* `GET /api/companies/{{companyId}}` – Unternehmen nach ID abrufen
* `PUT /api/companies/{{companyId}}` – Unternehmen aktualisieren
* `DELETE /api/companies/{{companyId}}` – Unternehmen löschen
* ⚠️ **Fehlerfälle:**
  * `POST /api/companies` (400 Bad Request) – Fehlender/leerer Name und ungültige Website-URL
  * `GET /api/companies/999999` (404 Not Found) – Abfrage einer nicht-existierenden Firmen-ID
  * `DELETE /api/companies/{{companyId}}` (409 Conflict) – Löschversuch eines Unternehmens mit aktiven Bewerbungen

### 3. Applications
* `GET /api/applications` – Alle Bewerbungen paginiert & sortiert abrufen (inkl. optionaler Status- & Datumsfilter)
* `POST /api/applications` – Neue Bewerbung erstellen (speichert generiertes `applicationId`)
* `GET /api/applications/{{applicationId}}` – Bewerbungsdetails abrufen
* `PUT /api/applications/{{applicationId}}` – Bewerbungsdaten aktualisieren
* `PATCH /api/applications/{{applicationId}}/status` – Status ändern (APPLIED, INTERVIEW_SCHEDULED, OFFER_RECEIVED, etc.)
* `GET /api/applications/{{applicationId}}/status-history` – Vollständige Statushistorie einsehen
* `DELETE /api/applications/{{applicationId}}` – Bewerbung löschen
* ⚠️ **Fehlerfälle:**
  * `POST /api/applications` (400 Bad Request) – Fehlende Pflichtfelder (`positionTitle`, `companyId`) & ungültige `jobUrl`
  * `POST /api/applications` (409 Conflict) – Duplikat: Bewerbung für selbe Position & Firma existiert bereits
  * `GET /api/applications/999999` (404 Not Found) – Abfrage einer nicht-existierenden Bewerbung
  * `GET /api/applications` (400 Bad Request) – Ungültige Paginierung/Sortierung (`page < 0`, `size > 100`, unzulässiges `sortBy`)
  * `GET /api/applications` (400 Bad Request) – Widersprüchliche Datumsfilter (`applicationDateFrom > applicationDateTo`)
  * `PATCH /api/applications/{{applicationId}}/status` (400 Bad Request) – Status-Update ohne Pflichtfeld `status`

### 4. Profiles
* `GET /api/profiles` – Eigenes Bewerberprofil mit Skills, Sprachen & Projekten abrufen
* `POST /api/profiles` – Neues Profil anlegen
* `PUT /api/profiles` – Bestehendes Profil aktualisieren
* `DELETE /api/profiles` – Eigenes Profil entfernen
* ⚠️ **Fehlerfälle:**
  * `POST /api/profiles` (400 Bad Request) – Fehlende Pflichtfelder (`targetRole`, `locationPreference`, `availability`, etc.)
  * `POST /api/profiles` (409 Conflict) – Versuch, ein zweites Profil für denselben Account anzulegen

### 5. Job Match AI
* `POST /api/job-match/analyze` – KI-gestütztes Matching einer Stellenanzeige mit dem Bewerberprofil (benötigt konfigurierten KI-Provider)
* ⚠️ **Fehlerfälle:**
  * `POST /api/job-match/analyze` (400 Bad Request) – Leere oder nur Whitespace enthaltende Stellenbeschreibung

### 6. Dokumentation & System
* `GET /v3/api-docs` – OpenAPI 3.0 Spezifikation als JSON
* `GET /swagger-ui.html` – Weiterleitung zur interaktiven Swagger UI

---

## 🛡️ Einheitliche ErrorResponse-Struktur

Alle Fehlerfälle der API liefern eine einheitliche JSON-Struktur (`ErrorResponse`):

```json
{
  "timestamp": "2026-10-27T19:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "positionTitle": "Position title cannot be null or blank",
    "companyId": "Company ID cannot be null"
  }
}
```

Die Postman-Tests in dieser Sammlung prüfen automatisch auf diese Felder (`timestamp`, `status`, `error`, `message` und bei 400 `fieldErrors`).

---

## 🛠️ Enthaltene Variablen

| Variable | Standardwert | Beschreibung |
| :--- | :--- | :--- |
| `baseUrl` | `http://localhost:8080` | URL des Spring Boot Backends |
| `token` | *(dynamisch)* | JWT Bearer Token, wird automatisch gesetzt |
| `companyId` | `1` *(dynamisch)* | Aktuelle Firmen-ID für Tests |
| `applicationId` | `1` *(dynamisch)* | Aktuelle Bewerbungs-ID für Tests |
