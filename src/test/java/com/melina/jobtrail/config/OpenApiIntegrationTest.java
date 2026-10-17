package com.melina.jobtrail.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OpenApiIntegrationTest extends com.melina.jobtrail.AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_arePublicAndDescribeJwtAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.info.title").value("JobTrail API"),
                        jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"),
                        jsonPath("$.paths['/api/applications']").exists(),
                        jsonPath("$.paths['/api/auth/register'].post.security").isEmpty(),
                        jsonPath("$.paths['/api/auth/login'].post.security").isEmpty(),
                        jsonPath("$.paths['/api/auth/demo'].post.security").isEmpty()
                );
    }

    @Test
    void apiDocs_includeTagsSchemasAndStatusCodes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpectAll(
                        status().isOk(),
                        // Tags
                        jsonPath("$.tags[?(@.name == 'Authentication')]").exists(),
                        jsonPath("$.tags[?(@.name == 'Applications')]").exists(),
                        jsonPath("$.tags[?(@.name == 'Companies')]").exists(),
                        jsonPath("$.tags[?(@.name == 'Profiles')]").exists(),
                        jsonPath("$.tags[?(@.name == 'Job Match')]").exists(),

                        // Status Codes on Applications
                        jsonPath("$.paths['/api/applications'].post.responses['201']").exists(),
                        jsonPath("$.paths['/api/applications'].post.responses['400']").exists(),
                        jsonPath("$.paths['/api/applications'].post.responses['409']").exists(),
                        jsonPath("$.paths['/api/applications/{id}'].delete.responses['204']").exists(),

                        // Status Codes on Job Match (including AI-specific codes)
                        jsonPath("$.paths['/api/job-match/analyze'].post.responses['200']").exists(),
                        jsonPath("$.paths['/api/job-match/analyze'].post.responses['429']").exists(),
                        jsonPath("$.paths['/api/job-match/analyze'].post.responses['502']").exists(),
                        jsonPath("$.paths['/api/job-match/analyze'].post.responses['503']").exists(),

                        // Schema Properties and Types
                        jsonPath("$.components.schemas.ApplicationResponse.properties.positionTitle").exists(),
                        jsonPath("$.components.schemas.CompanyRequest.properties.name").exists(),
                        jsonPath("$.components.schemas.ErrorResponse.properties.status").exists(),
                        jsonPath("$.components.schemas.JobMatchResponse.properties.score").exists(),
                        jsonPath("$.components.schemas.ProfileRequest.properties.targetRole").exists()
                );
    }

    @Test
    void apiDocs_includeEndpointExamples() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpectAll(
                        status().isOk(),
                        // Auth examples
                        jsonPath("$.paths['/api/auth/register'].post.requestBody.content['application/json'].examples['Beispiel-Registrierung'].value.email").value("new.applicant@jobtrail.dev"),
                        jsonPath("$.paths['/api/auth/register'].post.responses['201'].content['application/json'].examples['RegistrierterBenutzer'].value.email").value("new.applicant@jobtrail.dev"),
                        jsonPath("$.paths['/api/auth/login'].post.requestBody.content['application/json'].examples['Beispiel-Login'].value.email").value("demo@jobtrail.dev"),
                        jsonPath("$.paths['/api/auth/demo'].post.responses['200'].content['application/json'].examples['DemoLoginErfolgreich'].value.email").value("demo@jobtrail.dev"),

                        // Company examples
                        jsonPath("$.paths['/api/companies'].post.requestBody.content['application/json'].examples['Beispiel-Unternehmen'].value.name").value("TechCorp GmbH"),
                        jsonPath("$.paths['/api/companies'].post.responses['201'].content['application/json'].examples['ErstelltesUnternehmen'].value.name").value("TechCorp GmbH"),

                        // Application examples
                        jsonPath("$.paths['/api/applications'].post.requestBody.content['application/json'].examples['Beispiel-Bewerbung'].value.positionTitle").value("Cloud DevOps Engineer"),
                        jsonPath("$.paths['/api/applications'].post.responses['201'].content['application/json'].examples['ErstellteBewerbung'].value.positionTitle").value("Senior Java Developer"),

                        // Job Match examples
                        jsonPath("$.paths['/api/job-match/analyze'].post.requestBody.content['application/json'].examples['Beispiel-Stellenausschreibung'].value.description").isNotEmpty(),
                        jsonPath("$.paths['/api/job-match/analyze'].post.responses['200'].content['application/json'].examples['AnalyseErgebnis'].value.score").value(85),

                        // Profile examples
                        jsonPath("$.paths['/api/profiles'].post.requestBody.content['application/json'].examples['Beispiel-Profil'].value.targetRole").value("Senior Backend Developer"),
                        jsonPath("$.paths['/api/profiles'].post.responses['201'].content['application/json'].examples['ErstelltesProfil'].value.targetRole").value("Senior Backend Developer")
                );
    }

    @Test
    void swaggerUi_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpectAll(
                        status().isOk(),
                        content().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                );
    }
}
