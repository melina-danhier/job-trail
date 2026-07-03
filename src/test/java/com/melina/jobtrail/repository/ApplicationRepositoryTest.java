package com.melina.jobtrail.repository;

import com.melina.jobtrail.entity.Application;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.util.ApplicationStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ApplicationRepositoryTest {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByIdAndUserId_withExistingIdAndUserId_returnsApplication() {
        User user = createUser("user@example.com");
        Application application = createApplication(user,"Software Developer");

        Optional<Application> applicationOp = applicationRepository.findByIdAndUserId(
                application.getId(), user.getId()
        );

        assertTrue(applicationOp.isPresent());
        assertEquals("Software Developer", applicationOp.get().getPositionTitle());
    }

    @Test
    void findByIdAndUserId_withNonExistingId_returnsEmptyOptional() {
        User user = createUser("user@example.com");

        Optional<Application> applicationOp = applicationRepository.findByIdAndUserId(
                1L, user.getId()
        );

        assertFalse(applicationOp.isPresent());
    }

    @Test
    void findByIdAndUserId_withIdFromOtherUser_returnsEmptyOptional() {
        User user = createUser("user@example.com");
        User otherUser = createUser("othermail@example.com");
        Application application = createApplication(user,"Software Developer");

        Optional<Application> applicationOp = applicationRepository.findByIdAndUserId(
                application.getId(), otherUser.getId()
        );

        assertFalse(applicationOp.isPresent());
    }

    @Test
    void findAllByUserId_withExistingUserId_returnsAllApplicationsFromThatUser() {
        User user = createUser("user@example.com");
        createApplication(user,"Software Developer");
        createApplication(user,"Software Engineer");
        User otherUser = createUser("othermail@example.com");
        Application applicationFromOtherUser = createApplication(otherUser,"HR Manager");

        List<Application> applications = applicationRepository.findAllByUserId(user.getId());

        assertEquals(2, applications.size());
    }

    @Test
    void findAllByUserId_withNonExistingApplications_returnsEmptyList() {
        User user = createUser("nonexisting@example.com");

        List<Application> applications = applicationRepository.findAllByUserId(user.getId());

        assertTrue(applications.isEmpty());
    }

    @Test
    void findAllByUserId_withNonExistingUser_returnsEmptyList() {
        List<Application> applications = applicationRepository.findAllByUserId(10L);

        assertTrue(applications.isEmpty());
    }

    @Test
    void findAllByUserId_withPageable_returnsRequestedSortedPage() {
        User user = createUser("page@example.com");
        createApplication(user, "Zulu Developer");
        createApplication(user, "Alpha Developer");
        createApplication(user, "Middle Developer");

        Page<Application> result = applicationRepository.findAllByUserId(
                user.getId(), PageRequest.of(0, 2, Sort.by("positionTitle").ascending())
        );

        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals("Alpha Developer", result.getContent().getFirst().getPositionTitle());
        assertEquals("Middle Developer", result.getContent().get(1).getPositionTitle());
    }

    private User createUser(String mail) {
        User user = User.builder()
                .email(mail)
                .passwordHash("passwordHash")
                .build();
        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(User.class, user.getId());
    }

    private Application createApplication(User user, String positionTitle) {
        Company company = Company.builder()
                .user(user)
                .name("Company X")
                .build();
        entityManager.persist(company);

        Application application = Application.builder()
                .user(user)
                .positionTitle(positionTitle)
                .company(company)
                .status(ApplicationStatus.APPLIED)
                .applicationDate(null)
                .jobUrl(null)
                .build();
        entityManager.persist(application);
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(Application.class, application.getId());
    }
}
