# JobTrail

## Architekturentscheidung: JWT

Für die Authentifizierung verwendet JobTrail JWT-Bearer-Tokens. Damit bleibt die REST-API zustandslos und kann ohne serverseitige Sessions genutzt und skaliert werden. Als Konsequenz müssen kurze Token-Laufzeiten und ein sicherer Umgang mit dem Signaturschlüssel berücksichtigt werden.
