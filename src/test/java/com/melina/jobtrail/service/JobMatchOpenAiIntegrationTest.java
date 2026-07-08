package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.JobMatchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_OPENAI_INTEGRATION_TESTS", matches = "(?i)true")
class JobMatchOpenAiIntegrationTest {

    @Autowired
    private ChatClient chatClient;

    @Test
    void openAiReturnsNativeStructuredJobMatch() {
        JobMatchResponse response = chatClient.prompt()
                .system("You compare candidate profiles with job descriptions. Do not invent skills.")
                .user("Candidate: Java and Spring Boot. Job: Java, Spring Boot and Docker. " +
                        "Recommendation must be Apply, Maybe, or Do not apply.")
                .call()
                .entity(JobMatchResponse.class, spec -> spec
                        .useProviderStructuredOutput()
                        .validateSchema());

        assertNotNull(response);
        assertTrue(response.score() >= 0 && response.score() <= 100);
        assertNotNull(response.matchingSkills());
        assertNotNull(response.missingSkills());
        assertNotNull(response.recommendation());
        assertNotNull(response.summary());
    }
}
