package com.melina.jobtrail.repository;

import com.melina.jobtrail.entity.Application;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.util.ApplicationStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.melina.jobtrail.AbstractPostgresIntegrationTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ApplicationPostgresRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void connectionEstablished() {
        assertTrue(postgres.isCreated());
        assertTrue(postgres.isRunning());
    }

    @Test
    @DisplayName("findAllFiltered runs successfully against Postgres with null parameters, pagination and sorting")
    void findAllFiltered_againstRealPostgres_handlesDynamicFiltersAndPaging() {
        User user = createUser("repo-pg1@example.com");
        Company acme = createCompany(user, "Acme Tech");
        Company beta = createCompany(user, "Beta Solutions");

        createApplication(user, acme, "Senior Java Developer", ApplicationStatus.APPLIED, LocalDate.of(2026, 3, 1));
        createApplication(user, acme, "Fullstack Engineer", ApplicationStatus.INTERVIEW_SCHEDULED, LocalDate.of(2026, 3, 10));
        createApplication(user, beta, "DevOps Specialist", ApplicationStatus.APPLIED, LocalDate.of(2026, 4, 1));
        createApplication(user, beta, "Junior Developer", ApplicationStatus.REJECTED, LocalDate.of(2026, 4, 15));

        // Test 1: Filter by status APPLIED only (other filters null)
        Page<Application> appliedOnly = applicationRepository.findAllFiltered(
                user.getId(), ApplicationStatus.APPLIED, null, null, null,
                PageRequest.of(0, 10, Sort.by("applicationDate").ascending())
        );
        assertEquals(2, appliedOnly.getTotalElements());

        // Test 2: Combined filter (company + date range)
        Page<Application> acmeInRange = applicationRepository.findAllFiltered(
                user.getId(), null, acme.getId(), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5),
                PageRequest.of(0, 10)
        );
        assertEquals(1, acmeInRange.getTotalElements());
        assertEquals("Senior Java Developer", acmeInRange.getContent().getFirst().getPositionTitle());

        // Test 3: All null filters returns all applications of user
        Page<Application> allUserApps = applicationRepository.findAllFiltered(
                user.getId(), null, null, null, null,
                PageRequest.of(0, 10, Sort.by("positionTitle").ascending())
        );
        assertEquals(4, allUserApps.getTotalElements());
    }

    @Test
    @DisplayName("PostgreSQL enforces composite unique constraint uc_applications_user_company_position")
    void duplicateUserCompanyAndPosition_triggersPostgresUniqueConstraintViolation() {
        User user = createUser("unique-test@example.com");
        Company company = createCompany(user, "Constraint Corp");

        createApplication(user, company, "Platform Engineer", ApplicationStatus.APPLIED, LocalDate.now());

        // Attempting to insert duplicate (user, company, positionTitle)
        Application duplicate = Application.builder()
                .user(user)
                .company(company)
                .positionTitle("Platform Engineer")
                .status(ApplicationStatus.SAVED)
                .build();

        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(duplicate);
            entityManager.flush();
        });

        assertTrue(
                exception.getMessage().contains("uc_applications_user_company_position")
                        || (exception.getCause() != null && exception.getCause().getMessage().contains("uc_applications_user_company_position")),
                "Expected violation of uc_applications_user_company_position constraint"
        );
    }

    @Test
    @DisplayName("PostgreSQL enforces composite foreign key fk_applications_company_user")
    void crossUserCompanyReference_triggersPostgresForeignKeyViolation() {
        User userA = createUser("user-a@example.com");
        User userB = createUser("user-b@example.com");
        Company companyOfA = createCompany(userA, "Company of User A");

        // Attempt to create application for User B referencing Company belonging to User A
        Application crossApp = Application.builder()
                .user(userB)
                .company(companyOfA)
                .positionTitle("Security Engineer")
                .status(ApplicationStatus.APPLIED)
                .build();

        ConstraintViolationException exception = assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(crossApp);
            entityManager.flush();
        });

        assertTrue(
                exception.getMessage().contains("fk_applications_company_user")
                        || (exception.getCause() != null && exception.getCause().getMessage().contains("fk_applications_company_user")),
                "Expected violation of fk_applications_company_user constraint"
        );
    }

    @Test
    @DisplayName("PostgreSQL increments version column and handles optimistic locking")
    void versionColumn_isMaintainedAndIncrementedByPostgres() {
        User user = createUser("version-test@example.com");
        Company company = createCompany(user, "Version Corp");
        Application application = createApplication(
                user, company, "Cloud Architect",
                ApplicationStatus.APPLIED, LocalDate.now()
        );
        Long id = application.getId();

        Application first = applicationRepository.findById(id).orElseThrow();
        entityManager.detach(first);
        Application second = applicationRepository.findById(id).orElseThrow();
        entityManager.detach(second);

        assertEquals(0L, first.getVersion());
        assertEquals(0L, second.getVersion());

        entityManager.merge(first);
        Application managedFirst = applicationRepository.findById(id).orElseThrow();
        managedFirst.setPositionTitle("Principal Cloud Architect");
        entityManager.flush();
        assertEquals(1L, managedFirst.getVersion());

        second.setPositionTitle("Cloud Architect");
        assertThrows(OptimisticLockException.class, () -> {
            entityManager.merge(second);
            entityManager.flush();
        });
    }

    private User createUser(String mail) {
        User user = User.builder()
                .email(mail)
                .passwordHash("hashedPassword")
                .build();
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }

    private Company createCompany(User user, String name) {
        Company company = Company.builder()
                .user(user)
                .name(name)
                .build();
        entityManager.persist(company);
        entityManager.flush();
        return company;
    }

    private Application createApplication(
            User user, Company company, String positionTitle, ApplicationStatus status, LocalDate applicationDate
    ) {
        Application application = Application.builder()
                .user(user)
                .company(company)
                .positionTitle(positionTitle)
                .status(status)
                .applicationDate(applicationDate)
                .build();
        entityManager.persist(application);
        entityManager.flush();
        entityManager.clear();
        return applicationRepository.findById(application.getId()).orElseThrow();
    }
}
