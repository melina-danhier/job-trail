package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.application.ApplicationRequest;
import com.melina.jobtrail.dto.application.ApplicationResponse;
import com.melina.jobtrail.dto.application.ApplicationUpdateStatusRequest;
import com.melina.jobtrail.entity.Application;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.exception.ApplicationNotFoundException;
import com.melina.jobtrail.mapper.ApplicationMapper;
import com.melina.jobtrail.repository.ApplicationRepository;
import com.melina.jobtrail.util.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final UserService userService;
    private final CompanyService companyService;

    public ApplicationResponse createApplication(String email, ApplicationRequest request) {
        User user = userService.findUserOrThrow(email);
        Company company = companyService.findCompanyOrThrow(request.companyId(), user.getId());
        Application application = applicationMapper.toEntity(request);
        application.setUser(user);
        application.setCompany(company);
        application.setStatus(ApplicationStatus.APPLIED);
        application = applicationRepository.save(application);
        return applicationMapper.toResponse(application);
    }

    public List<ApplicationResponse> getApplications(String email) {
        User user = userService.findUserOrThrow(email);
        List<Application> applications = applicationRepository.findAllByUserId(user.getId());
        return applicationMapper.toResponseList(applications);
    }

    public ApplicationResponse getApplicationById(String email, long id) {
        User user = userService.findUserOrThrow(email);
        Application application = findApplicationOrThrow(id, user.getId());
        return applicationMapper.toResponse(application);
    }

    public ApplicationResponse updateApplication(
            String email, long id, ApplicationRequest requestDto
    ) {
        User user = userService.findUserOrThrow(email);
        Application application = findApplicationOrThrow(id, user.getId());
        Company company = companyService.findCompanyOrThrow(requestDto.companyId(), user.getId());
        applicationMapper.updateApplication(application, requestDto);
        application.setCompany(company);
        application = applicationRepository.save(application);
        return applicationMapper.toResponse(application);
    }

    public ApplicationResponse updateApplicationStatus(String email, long id, ApplicationUpdateStatusRequest requestDto) {
        Application application = findApplicationOrThrow(id, userService.findUserOrThrow(email).getId());
        application.setStatus(requestDto.status());
        application = applicationRepository.save(application);
        return applicationMapper.toResponse(application);
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
}
