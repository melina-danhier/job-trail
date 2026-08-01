package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.application.ApplicationCreateRequest;
import com.melina.jobtrail.dto.application.ApplicationResponse;
import com.melina.jobtrail.dto.application.ApplicationUpdateRequest;
import com.melina.jobtrail.dto.application.ApplicationUpdateStatusRequest;
import com.melina.jobtrail.dto.application.ApplicationStatusHistoryResponse;
import com.melina.jobtrail.dto.PageResponse;
import com.melina.jobtrail.entity.Application;
import com.melina.jobtrail.entity.ApplicationStatusHistory;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.exception.ApplicationNotFoundException;
import com.melina.jobtrail.exception.DuplicateApplicationException;
import com.melina.jobtrail.mapper.ApplicationMapper;
import com.melina.jobtrail.repository.ApplicationRepository;
import com.melina.jobtrail.repository.ApplicationStatusHistoryRepository;
import com.melina.jobtrail.util.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService {
    private static final List<String> ALLOWED_SORT_FIELDS = List.of(
            "createdAt", "updatedAt", "applicationDate", "positionTitle", "status"
    );
    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final ApplicationMapper applicationMapper;
    private final UserService userService;
    private final CompanyService companyService;

    public ApplicationResponse createApplication(String email, ApplicationCreateRequest request) {
        User user = userService.findUserOrThrow(email);
        Company company = companyService.findCompanyOrThrow(request.companyId(), user.getId());
        rejectDuplicate(user.getId(), company.getId(), request.positionTitle());
        Application application = applicationMapper.toEntity(request);
        application.setUser(user);
        application.setCompany(company);
        application.setStatus(ApplicationStatus.SAVED);
        application = saveOrRejectDuplicate(application);
        saveStatusChange(application, null, ApplicationStatus.SAVED);
        return applicationMapper.toResponse(application);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getApplications(
            String email, int page, int size, String sortBy, Sort.Direction direction,
            ApplicationStatus status, Long companyId, LocalDate applicationDateFrom, LocalDate applicationDateTo
    ) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sortBy value. Allowed values: " + String.join(", ", ALLOWED_SORT_FIELDS)
            );
        }
        if (applicationDateFrom != null && applicationDateTo != null
                && applicationDateFrom.isAfter(applicationDateTo)) {
            throw new IllegalArgumentException("applicationDateFrom must not be after applicationDateTo");
        }
        User user = userService.findUserOrThrow(email);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy).and(Sort.by("id")));
        Page<ApplicationResponse> applications = applicationRepository
                .findAllFiltered(
                        user.getId(), status, companyId, applicationDateFrom, applicationDateTo, pageable
                )
                .map(applicationMapper::toResponse);
        return PageResponse.from(applications);
    }

    public ApplicationResponse getApplicationById(String email, long id) {
        User user = userService.findUserOrThrow(email);
        Application application = findApplicationOrThrow(id, user.getId());
        return applicationMapper.toResponse(application);
    }

    public ApplicationResponse updateApplication(
            String email, long id, ApplicationUpdateRequest requestDto
    ) {
        User user = userService.findUserOrThrow(email);
        Application application = findApplicationOrThrow(id, user.getId());
        Company company = companyService.findCompanyOrThrow(requestDto.companyId(), user.getId());
        rejectDuplicate(user.getId(), company.getId(), requestDto.positionTitle(), application.getId());
        applicationMapper.updateApplication(application, requestDto);
        application.setCompany(company);
        application = saveOrRejectDuplicate(application);
        return applicationMapper.toResponse(application);
    }

    public ApplicationResponse updateApplicationStatus(String email, long id, ApplicationUpdateStatusRequest requestDto) {
        Application application = findApplicationOrThrow(id, userService.findUserOrThrow(email).getId());
        ApplicationStatus previousStatus = application.getStatus();
        if (previousStatus == requestDto.status()) {
            return applicationMapper.toResponse(application);
        }
        application.setStatus(requestDto.status());
        application = applicationRepository.save(application);
        saveStatusChange(application, previousStatus, requestDto.status());
        return applicationMapper.toResponse(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationStatusHistoryResponse> getStatusHistory(String email, long id) {
        Application application = findApplicationOrThrow(id, userService.findUserOrThrow(email).getId());
        return statusHistoryRepository.findAllByApplicationIdOrderByChangedAtAscIdAsc(application.getId())
                .stream()
                .map(entry -> new ApplicationStatusHistoryResponse(
                        entry.getId(), entry.getPreviousStatus(), entry.getNewStatus(), entry.getChangedAt()
                ))
                .toList();
    }

    private void saveStatusChange(
            Application application, ApplicationStatus previousStatus, ApplicationStatus newStatus
    ) {
        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .application(application)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .build());
    }

    public void deleteApplication(String email, long id) {
        User user = userService.findUserOrThrow(email);
        Application application = findApplicationOrThrow(id, user.getId());
        applicationRepository.delete(application);
    }

    Application findApplicationOrThrow(long id, long userId) {
        return applicationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    private void rejectDuplicate(long userId, long companyId, String positionTitle) {
        if (applicationRepository.existsByUserIdAndCompanyIdAndPositionTitle(userId, companyId, positionTitle)) {
            throw new DuplicateApplicationException();
        }
    }

    private void rejectDuplicate(long userId, long companyId, String positionTitle, long applicationId) {
        if (applicationRepository.existsByUserIdAndCompanyIdAndPositionTitleAndIdNot(
                userId, companyId, positionTitle, applicationId
        )) {
            throw new DuplicateApplicationException();
        }
    }

    private Application saveOrRejectDuplicate(Application application) {
        try {
            // Flush here so a concurrent duplicate is translated inside the service boundary.
            Application savedApplication = applicationRepository.save(application);
            applicationRepository.flush();
            return savedApplication;
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateApplicationException();
        }
    }
}
