package com.melina.jobtrail.service;


import com.melina.jobtrail.dto.application.ApplicationRequest;
import com.melina.jobtrail.dto.application.ApplicationResponse;
import com.melina.jobtrail.dto.application.ApplicationUpdateStatusRequest;
import com.melina.jobtrail.dto.PageResponse;
import com.melina.jobtrail.entity.Application;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.exception.ApplicationNotFoundException;
import com.melina.jobtrail.exception.CompanyNotFoundException;
import com.melina.jobtrail.mapper.ApplicationMapper;
import com.melina.jobtrail.repository.ApplicationRepository;
import com.melina.jobtrail.repository.ApplicationStatusHistoryRepository;
import com.melina.jobtrail.util.ApplicationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @InjectMocks
    private ApplicationService applicationService;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationStatusHistoryRepository statusHistoryRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private UserService userService;

    @Mock
    private CompanyService companyService;

    @Test
    void createApplication_withValidRequest_returnsApplicationResponse() {
        String email = "user@example.com";
        ApplicationRequest request = createApplicationRequest("Software Engineer");
        Application application = createApplication(1L, "Software Engineer");
        ApplicationResponse expectedResponse = createApplicationResponse(1L, "Software Engineer");
        User user = createUser(email);
        Company company = createCompany(1L, "Company Name");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(companyService.findCompanyOrThrow(request.companyId(), user.getId())).thenReturn(company);
        when(applicationMapper.toEntity(request)).thenReturn(application);
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toResponse(application)).thenReturn(expectedResponse);

        ApplicationResponse response = applicationService.createApplication(email, request);

        assertEquals(expectedResponse, response);
        assertSame(user, application.getUser());
        assertSame(company, application.getCompany());
        assertEquals(ApplicationStatus.SAVED, application.getStatus());
        verify(applicationRepository).save(application);
        verify(statusHistoryRepository).save(any());
    }

    @Test
    void createApplication_withInvalidCompanyId_throwsCompanyNotFoundException() {
        String email = "user@example.com";
        User user = createUser(email);
        ApplicationRequest request = createApplicationRequest("Software Engineer");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(companyService.findCompanyOrThrow(request.companyId(), user.getId()))
                .thenThrow(new CompanyNotFoundException(request.companyId()));

        assertThrows(
                CompanyNotFoundException.class,
                () -> applicationService.createApplication(email, request)
        );
        verifyNoInteractions(applicationMapper, applicationRepository);
    }

    @Test
    void getApplications_returnsApplicationResponses() {
        String email = "user@example.com";
        User user = createUser(email);
        List<Application> applications = List.of(createApplication(1L, "Software Engineer"));
        List<ApplicationResponse> expectedResponses = List.of(
                createApplicationResponse(1L, "Software Engineer")
        );

        when(userService.findUserOrThrow(email)).thenReturn(user);
        PageRequest pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "positionTitle")
                .and(Sort.by("id")));
        when(applicationRepository.findAllFiltered(user.getId(), null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(applications, pageable, 11));
        when(applicationMapper.toResponse(applications.getFirst())).thenReturn(expectedResponses.getFirst());

        PageResponse<ApplicationResponse> response = applicationService.getApplications(
                email, 1, 10, "positionTitle", Sort.Direction.ASC,
                null, null, null, null
        );

        assertEquals(expectedResponses, response.content());
        assertEquals(1, response.page());
        assertEquals(2, response.totalPages());
        assertEquals(11, response.totalElements());
    }

    @Test
    void getApplications_withInvalidSortField_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> applicationService.getApplications(
                        "user@example.com", 0, 20, "company.password", Sort.Direction.ASC,
                        null, null, null, null
                )
        );

        verifyNoInteractions(userService, applicationRepository, applicationMapper);
    }

    @Test
    void getApplications_withFilters_passesFiltersToRepository() {
        String email = "user@example.com";
        User user = createUser(email);
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by("id")));

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(applicationRepository.findAllFiltered(
                user.getId(), ApplicationStatus.ACCEPTED, 3L, from, to, pageable
        )).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        applicationService.getApplications(
                email, 0, 20, "createdAt", Sort.Direction.DESC,
                ApplicationStatus.ACCEPTED, 3L, from, to
        );

        verify(applicationRepository).findAllFiltered(
                user.getId(), ApplicationStatus.ACCEPTED, 3L, from, to, pageable
        );
    }

    @Test
    void getApplications_withReversedDateRange_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> applicationService.getApplications(
                "user@example.com", 0, 20, "createdAt", Sort.Direction.DESC,
                null, null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 30)
        ));

        verifyNoInteractions(userService, applicationRepository, applicationMapper);
    }

    @Test
    void getApplicationById_withValidId_returnsApplicationResponse() {
        String email = "user@example.com";
        User user = createUser(email);
        Application application = createApplication(1L, "Software Engineer");
        ApplicationResponse expectedResponse = createApplicationResponse(1L, "Software Engineer");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(applicationRepository.findByIdAndUserId(application.getId(), user.getId()))
                .thenReturn(Optional.of(application));
        when(applicationMapper.toResponse(application)).thenReturn(expectedResponse);

        ApplicationResponse response = applicationService.getApplicationById(email, application.getId());

        assertEquals(expectedResponse, response);
    }

    @Test
    void getApplicationById_withInvalidId_throwsApplicationNotFoundException() {
        String email = "user@example.com";
        User user = createUser(email);

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(applicationRepository.findByIdAndUserId(99L, user.getId()))
                .thenReturn(Optional.empty());

        assertThrows(
                ApplicationNotFoundException.class,
                () -> applicationService.getApplicationById(email, 99L)
        );
        verifyNoInteractions(applicationMapper);
    }

    @Test
    void updateApplication_withValidRequest_returnsApplicationResponse() {
        String email = "user@example.com";
        User user = createUser(email);
        Company company = createCompany(1L, "Company Name");
        Application application = createApplication(1L, "Software Engineer");
        ApplicationRequest request = createApplicationRequest("Backend Engineer");
        ApplicationResponse expectedResponse = createApplicationResponse(1L, "Backend Engineer");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(applicationRepository.findByIdAndUserId(application.getId(), user.getId()))
                .thenReturn(Optional.of(application));
        when(companyService.findCompanyOrThrow(request.companyId(), user.getId())).thenReturn(company);
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toResponse(application)).thenReturn(expectedResponse);

        ApplicationResponse response = applicationService.updateApplication(
                email, application.getId(), request
        );

        assertEquals(expectedResponse, response);
        assertSame(company, application.getCompany());
        verify(applicationMapper).updateApplication(application, request);
        verify(applicationRepository).save(application);
    }

    @Test
    void updateApplicationStatus_withSameStatus_doesNotCreateHistoryEntry() {
        String email = "user@example.com";
        User user = createUser(email);
        Application application = createApplication(1L, "Software Engineer");
        application.setStatus(ApplicationStatus.SAVED);
        ApplicationUpdateStatusRequest request = new ApplicationUpdateStatusRequest(ApplicationStatus.SAVED);
        ApplicationResponse expectedResponse = createApplicationResponse(1L, "Software Engineer");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(applicationRepository.findByIdAndUserId(application.getId(), user.getId()))
                .thenReturn(Optional.of(application));
        when(applicationMapper.toResponse(application)).thenReturn(expectedResponse);

        assertEquals(expectedResponse, applicationService.updateApplicationStatus(email, application.getId(), request));

        verify(applicationRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void updateApplicationStatus_withValidRequest_returnsApplicationResponse() {
        String email = "user@example.com";
        User user = createUser(email);
        Application application = createApplication(1L, "Software Engineer");
        ApplicationUpdateStatusRequest request = new ApplicationUpdateStatusRequest(
                ApplicationStatus.INTERVIEW_SCHEDULED
        );
        ApplicationResponse expectedResponse = createApplicationResponse(1L, "Software Engineer");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(applicationRepository.findByIdAndUserId(application.getId(), user.getId()))
                .thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toResponse(application)).thenReturn(expectedResponse);

        ApplicationResponse response = applicationService.updateApplicationStatus(
                email, application.getId(), request
        );

        assertEquals(expectedResponse, response);
        assertEquals(ApplicationStatus.INTERVIEW_SCHEDULED, application.getStatus());
        verify(applicationRepository).save(application);
        verify(statusHistoryRepository).save(any());
    }

    @Test
    void deleteApplication_withValidId_deletesApplication() {
        String email = "user@example.com";
        User user = createUser(email);
        Application application = createApplication(1L, "Software Engineer");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(applicationRepository.findByIdAndUserId(application.getId(), user.getId()))
                .thenReturn(Optional.of(application));

        applicationService.deleteApplication(email, application.getId());

        verify(applicationRepository).delete(application);
    }

    private ApplicationRequest createApplicationRequest(String positionTitle) {
        return new ApplicationRequest(
                positionTitle,
                1L,
                null,
                null,
                null
        );
    }

    private ApplicationResponse createApplicationResponse(long id, String positionTitle) {
        return new ApplicationResponse(
                id,
                null,
                positionTitle,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Application createApplication(long id, String positionTitle) {
        return Application.builder()
                .id(id)
                .positionTitle(positionTitle)
                .build();
    }

    private Company createCompany(long id, String companyName) {
        return Company.builder()
                .id(id)
                .name(companyName)
                .build();
    }

    private User createUser(String email) {
        return User.builder()
                .email(email)
                .build();
    }
}
