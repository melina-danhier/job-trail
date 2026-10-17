package com.melina.jobtrail.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Configuration
public class OpenApiConfig {
    private static final String BEARER_AUTH = "bearerAuth";

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(AuthenticationPrincipal.class);
    }

    @Bean
    public OpenAPI jobTrailOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("JobTrail API")
                        .version("1.0")
                        .description("""
                                ### 🚀 Willkommen zur interaktiven JobTrail API-Dokumentation!
                                
                                **JobTrail** ist ein modernes REST-Backend zur Verwaltung von Bewerbungsprozessen mit integriertem, KI-gestütztem Stellenabgleich (*Spring AI*).
                                
                                ---
                                
                                #### ⚡ Quickstart für Recruiter & Tester (In 30 Sekunden startklar)
                                
                                Du möchtest alle Endpunkte sofort mit realistischen Daten testen, ohne dich manuell zu registrieren?
                                
                                **1. Demo-Token abrufen**
                                
                                * Führe unten unter **Authentication** den Endpunkt **`POST /api/auth/demo`** aus (*Try it out* ➔ *Execute*).
                                
                                * Der Demo-Account wird dabei automatisch frisch mit sauberen Beispieldaten initialisiert.
                                
                                **2. Im Swagger UI autorisieren**
                                
                                * Kopiere den `token` aus der Response.
                                
                                * Klicke oben rechts auf den grünen **Authorize**-Button (Schloss-Symbol).
                                
                                * Füge den Token in das Eingabefeld ein (z. B. `Bearer <token>` oder nur `<token>`) und klicke auf *Authorize*.
                                
                                **3. Direkt testen**
                                
                                Der Demo-Account (`demo@jobtrail.dev`) ist bereits voll ausgestattet:
                                
                                * **`GET /api/companies`** – Vorbereitete Unternehmen (Spotify, SAP, Zalando)
                                
                                * **`GET /api/applications`** – Bewerbungen mit Statusverlauf
                                
                                * **`GET /api/profiles`** – Vollständiges Bewerberprofil mit Skills, Sprachen & Projekten
                                
                                * **`POST /api/job-match/analyze`** – KI-gestütztes Matching einer echten Stellenanzeige mit dem Profil
                                
                                > 💡 **Tipp:** Der Demo-Account wird bei jedem Aufruf von `POST /api/auth/demo` sowie bei jeder Neuregistrierung automatisch auf den Ausgangszustand zurückgesetzt.
                                """)
                        .contact(new Contact().name("JobTrail Support").email("support@jobtrail.local"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
