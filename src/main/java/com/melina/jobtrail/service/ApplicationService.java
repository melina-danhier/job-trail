package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.ApplicationDto;
import com.melina.jobtrail.dto.RequestApplicationDto;
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

    public ApplicationDto createApplication(String email, RequestApplicationDto requestDto) {
        User user = userService.findUserOrThrow(email);
        Company company = companyService.findCompanyOrThrow(requestDto.companyId(), user.getId());

        Application application = Application.builder()
                        .user(user)
                        .company(company)
                        .positionTitle(requestDto.positionTitle())
                        .status(ApplicationStatus.APPLIED)
                        .applicationDate(requestDto.applicationDate())
                        .jobUrl(requestDto.jobUrl())
                        .build();
        application = applicationRepository.save(application);
        return applicationMapper.toDto(application);
    }

    public List<ApplicationDto> getApplications(String email) {
        User user = userService.findUserOrThrow(email);
        List<Application> applications = applicationRepository.findAllByUserId(user.getId());
        return applicationMapper.toDtoList(applications);
    }

    public ApplicationDto getApplicationById(String email, long id) {
        User user = userService.findUserOrThrow(email);
        Application application = findApplicationOrThrow(id, user.getId());
        return applicationMapper.toDto(application);
    }

    public ApplicationDto updateApplication(
            String email, long id, RequestApplicationDto requestDto
    ) {
        User user = userService.findUserOrThrow(email);
        Application application = findApplicationOrThrow(id, user.getId());
        Company company = companyService.findCompanyOrThrow(requestDto.companyId(), user.getId());
        applicationMapper.updateApplication(application, requestDto);
        application.setCompany(company);
        application = applicationRepository.save(application);
        return applicationMapper.toDto(application);
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
