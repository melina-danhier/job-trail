package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.DemoLoginResponse;
import com.melina.jobtrail.entity.Application;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.repository.ApplicationRepository;
import com.melina.jobtrail.repository.CompanyRepository;
import com.melina.jobtrail.repository.UserRepository;
import com.melina.jobtrail.util.ApplicationStatus;
import com.melina.jobtrail.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DemoDataServiceTest extends com.melina.jobtrail.AbstractPostgresIntegrationTest {

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ProfileService profileService;

    @AfterEach
    void tearDown() {
        Optional<User> demoUserOpt = userRepository.findByEmail(DemoDataService.DEMO_EMAIL);
        demoUserOpt.ifPresent(user -> {
            applicationRepository.deleteAll(applicationRepository.findAllByUserId(user.getId()));
            companyRepository.deleteAll(companyRepository.findAllByUserId(user.getId()));
            userRepository.delete(user);
        });
    }

    @Test
    void initDemoData_shouldCreateCompleteDemoWorkspace() {
        demoDataService.initDemoData(false);

        Optional<User> userOpt = userRepository.findByEmail(DemoDataService.DEMO_EMAIL);
        assertTrue(userOpt.isPresent(), "Demo user should be created");
        User user = userOpt.get();

        var profile = profileService.getProfile(DemoDataService.DEMO_EMAIL);
        assertNotNull(profile, "User profile should be initialized");
        assertEquals("Senior Java & Cloud Software Engineer", profile.targetRole());
        assertFalse(profile.skills().isEmpty(), "Skills should be populated");
        assertFalse(profile.languages().isEmpty(), "Languages should be populated");
        assertFalse(profile.projects().isEmpty(), "Projects should be populated");

        List<Company> companies = companyRepository.findAllByUserId(user.getId());
        assertEquals(3, companies.size(), "Should have created 3 companies (Spotify, SAP, Zalando)");

        List<Application> applications = applicationRepository.findAllByUserId(user.getId());
        assertEquals(3, applications.size(), "Should have created 3 applications");

        assertTrue(applications.stream().anyMatch(a -> a.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED));
        assertTrue(applications.stream().anyMatch(a -> a.getStatus() == ApplicationStatus.APPLIED));
        assertTrue(applications.stream().anyMatch(a -> a.getStatus() == ApplicationStatus.OFFER_RECEIVED));
    }

    @Test
    void getOrCreateDemoToken_shouldReturnValidJwt() {
        DemoLoginResponse response = demoDataService.getOrCreateDemoToken(false);

        assertNotNull(response);
        assertEquals("Bearer", response.tokenType());
        assertEquals(DemoDataService.DEMO_EMAIL, response.email());
        assertNotNull(response.token());

        String extractedEmail = jwtUtil.validateTokenAndExtractEmail(response.token());
        assertEquals(DemoDataService.DEMO_EMAIL, extractedEmail);
    }

    @Test
    void getOrCreateDemoToken_withResetTrue_shouldResetCleanly() {
        // Initial creation
        demoDataService.getOrCreateDemoToken(false);

        // Reset
        DemoLoginResponse resetResponse = demoDataService.getOrCreateDemoToken(true);
        assertNotNull(resetResponse.token());

        User user = userRepository.findByEmail(DemoDataService.DEMO_EMAIL).orElseThrow();
        assertEquals(3, companyRepository.findAllByUserId(user.getId()).size());
        assertEquals(3, applicationRepository.findAllByUserId(user.getId()).size());
    }
}
