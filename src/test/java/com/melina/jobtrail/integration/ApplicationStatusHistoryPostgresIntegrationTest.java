package com.melina.jobtrail.integration;

import com.melina.jobtrail.dto.application.ApplicationCreateRequest;
import com.melina.jobtrail.dto.application.ApplicationResponse;
import com.melina.jobtrail.dto.application.ApplicationStatusHistoryResponse;
import com.melina.jobtrail.dto.application.ApplicationUpdateStatusRequest;
import com.melina.jobtrail.entity.ApplicationStatusHistory;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.repository.ApplicationRepository;
import com.melina.jobtrail.repository.ApplicationStatusHistoryRepository;
import com.melina.jobtrail.repository.CompanyRepository;
import com.melina.jobtrail.repository.UserRepository;
import com.melina.jobtrail.service.ApplicationService;
import com.melina.jobtrail.util.ApplicationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext
class ApplicationStatusHistoryPostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("jobtrail")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    @DisplayName("Status history tracks full transition lifecycle in Postgres with precise timestamps")
    void statusHistory_recordsCompleteLifecycleWithTimestampsInPostgres() {
        String email = "history-" + System.nanoTime() + "@example.com";
        User user = userRepository.save(User.builder().email(email).passwordHash("secret").build());
        Company company = companyRepository.save(Company.builder().user(user).name("History GmbH").build());

        Instant beforeCreation = Instant.now().minus(1, ChronoUnit.SECONDS);

        // 1. Create Application -> automatically creates initial status history entry (null -> SAVED)
        ApplicationResponse created = applicationService.createApplication(
                email,
                new ApplicationCreateRequest("Senior SRE", company.getId(), null, null)
        );
        long appId = created.id();

        // 2. Transition status: SAVED -> APPLIED
        applicationService.updateApplicationStatus(
                email, appId, new ApplicationUpdateStatusRequest(ApplicationStatus.APPLIED)
        );

        // 3. Transition status: APPLIED -> INTERVIEW_SCHEDULED
        applicationService.updateApplicationStatus(
                email, appId, new ApplicationUpdateStatusRequest(ApplicationStatus.INTERVIEW_SCHEDULED)
        );

        // 4. Verify history trail via service
        List<ApplicationStatusHistoryResponse> history = applicationService.getStatusHistory(email, appId);
        assertEquals(3, history.size());

        // Check entry 1: Initial creation
        assertNull(history.get(0).previousStatus());
        assertEquals(ApplicationStatus.SAVED, history.get(0).newStatus());
        assertTrue(history.get(0).changedAt().isAfter(beforeCreation));

        // Check entry 2: First transition
        assertEquals(ApplicationStatus.SAVED, history.get(1).previousStatus());
        assertEquals(ApplicationStatus.APPLIED, history.get(1).newStatus());
        assertFalse(history.get(1).changedAt().isBefore(history.get(0).changedAt()));

        // Check entry 3: Second transition
        assertEquals(ApplicationStatus.APPLIED, history.get(2).previousStatus());
        assertEquals(ApplicationStatus.INTERVIEW_SCHEDULED, history.get(2).newStatus());
        assertFalse(history.get(2).changedAt().isBefore(history.get(1).changedAt()));

        // Verify directly in Postgres repository
        List<ApplicationStatusHistory> directEntities =
                statusHistoryRepository.findAllByApplicationIdOrderByChangedAtAscIdAsc(appId);
        assertEquals(3, directEntities.size());
    }

    @Test
    @DisplayName("PostgreSQL ON DELETE CASCADE automatically removes status history records when application is deleted")
    void deletingApplication_cascadesAndRemovesStatusHistoryInPostgres() {
        String email = "cascade-" + System.nanoTime() + "@example.com";
        User user = userRepository.save(User.builder().email(email).passwordHash("secret").build());
        Company company = companyRepository.save(Company.builder().user(user).name("Cascade Corp").build());

        ApplicationResponse created = applicationService.createApplication(
                email,
                new ApplicationCreateRequest("DevOps Architect", company.getId(), null, null)
        );
        long appId = created.id();

        applicationService.updateApplicationStatus(
                email, appId, new ApplicationUpdateStatusRequest(ApplicationStatus.APPLIED)
        );

        // Verify status history exists in DB
        List<ApplicationStatusHistory> beforeDelete =
                statusHistoryRepository.findAllByApplicationIdOrderByChangedAtAscIdAsc(appId);
        assertEquals(2, beforeDelete.size());

        // Delete application
        applicationService.deleteApplication(email, appId);

        // Verify application was removed
        assertTrue(applicationRepository.findById(appId).isEmpty());

        // Verify real Postgres ON DELETE CASCADE removed all history records
        List<ApplicationStatusHistory> afterDelete =
                statusHistoryRepository.findAllByApplicationIdOrderByChangedAtAscIdAsc(appId);
        assertTrue(afterDelete.isEmpty(), "Status history records should be deleted by Postgres ON DELETE CASCADE");
    }
}
