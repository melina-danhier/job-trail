package com.melina.jobtrail.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(properties = "spring.ai.openai.api-key=")
@ActiveProfiles("test")
class JobMatchOptionalConfigurationIntegrationTest {

    @Autowired
    private JobMatchAvailability availability;

    @Test
    void applicationStartsWithoutOpenAiApiKey() {
        assertFalse(availability.isConfigured());
    }
}
