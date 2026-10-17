package com.melina.jobtrail.integration;

import com.melina.jobtrail.dto.application.ApplicationUpdateStatusRequest;
import com.melina.jobtrail.entity.Application;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext
class ApplicationTransactionalPostgresIntegrationTest {

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
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @Test
    @DisplayName("PostgreSQL rolls back entire transaction when status history persistence fails")
    void transactionRollback_revertsApplicationStatusInPostgresWhenHistoryFails() {
        String email = "tx-rollback-" + System.nanoTime() + "@example.com";
        User user = userRepository.save(User.builder().email(email).passwordHash("hash").build());
        Company company = companyRepository.save(Company.builder().user(user).name("TX Corp").build());
        Application application = applicationRepository.save(Application.builder()
                .user(user)
                .company(company)
                .positionTitle("Site Reliability Engineer")
                .status(ApplicationStatus.APPLIED)
                .build());

        // Simulate status history failure to force rollback
        doThrow(new IllegalStateException("Postgres transaction failure simulation"))
                .when(statusHistoryRepository).save(any());

        assertThrows(IllegalStateException.class, () -> applicationService.updateApplicationStatus(
                email,
                application.getId(),
                new ApplicationUpdateStatusRequest(ApplicationStatus.ACCEPTED)
        ));

        // Verify that the status in real PostgreSQL remains APPLIED due to rollback
        Application persisted = applicationRepository.findById(application.getId()).orElseThrow();
        assertEquals(ApplicationStatus.APPLIED, persisted.getStatus(),
                "Application status in Postgres must remain APPLIED after transaction rollback");
    }

    @Test
    @DisplayName("PostgreSQL optimistic locking prevents concurrent updates from overwriting stale data")
    void optimisticLocking_preventsStaleConcurrentUpdatesInPostgres() {
        String email = "tx-optimistic-" + System.nanoTime() + "@example.com";
        User user = userRepository.save(User.builder().email(email).passwordHash("hash").build());
        Company company = companyRepository.save(Company.builder().user(user).name("Concurrent Corp").build());
        Application application = applicationRepository.save(Application.builder()
                .user(user)
                .company(company)
                .positionTitle("Backend Engineer")
                .status(ApplicationStatus.SAVED)
                .build());

        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // Transaction 1 reads and updates the position title, bumping the version
        tx.executeWithoutResult(status -> {
            Application appTx1 = applicationRepository.findById(application.getId()).orElseThrow();
            assertEquals(0, appTx1.getVersion());
            appTx1.setPositionTitle("Lead Backend Engineer");
            applicationRepository.saveAndFlush(appTx1);
        });

        // Transaction 2 tries to save using the stale entity (version 0)
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            tx.executeWithoutResult(status -> {
                application.setPositionTitle("Conflicting Position Title");
                applicationRepository.saveAndFlush(application);
            });
        });

        // Verify final state in Postgres has the first transaction's update and incremented version
        Application finalApp = applicationRepository.findById(application.getId()).orElseThrow();
        assertEquals("Lead Backend Engineer", finalApp.getPositionTitle());
        assertEquals(1, finalApp.getVersion());
    }
}
