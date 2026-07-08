package com.melina.jobtrail.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_arePublicAndDescribeJwtAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.info.title").value("JobTrail API"),
                        jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"),
                        jsonPath("$.paths['/api/applications']").exists()
                );
    }
}
