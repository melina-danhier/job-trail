package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.application.ApplicationUpdateStatusRequest;
import com.melina.jobtrail.entity.Application;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.repository.ApplicationRepository;
import com.melina.jobtrail.repository.ApplicationStatusHistoryRepository;
import com.melina.jobtrail.repository.CompanyRepository;
import com.melina.jobtrail.repository.UserRepository;
import com.melina.jobtrail.util.ApplicationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationServiceTransactionTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @Test
    void updateStatus_whenHistoryCannotBeStored_rollsBackStatusChange() {
        String email = "rollback-" + System.nanoTime() + "@example.com";
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash("passwordHash")
                .build());
        Company company = companyRepository.save(Company.builder()
                .user(user)
                .name("Rollback Company")
                .build());
        Application application = applicationRepository.save(Application.builder()
                .user(user)
                .company(company)
                .positionTitle("Rollback Engineer")
                .status(ApplicationStatus.APPLIED)
                .build());

        doThrow(new IllegalStateException("history storage unavailable"))
                .when(statusHistoryRepository).save(any());

        assertThrows(IllegalStateException.class, () -> applicationService.updateApplicationStatus(
                email,
                application.getId(),
                new ApplicationUpdateStatusRequest(ApplicationStatus.ACCEPTED)
        ));

        Application persistedApplication = applicationRepository.findById(application.getId()).orElseThrow();
        assertEquals(ApplicationStatus.APPLIED, persistedApplication.getStatus());

        applicationRepository.deleteById(application.getId());
        companyRepository.deleteById(company.getId());
        userRepository.deleteById(user.getId());
    }
}
