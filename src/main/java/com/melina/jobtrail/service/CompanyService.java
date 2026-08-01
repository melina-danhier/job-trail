package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.CompanyRequest;
import com.melina.jobtrail.dto.CompanyResponse;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.exception.CompanyNotFoundException;
import com.melina.jobtrail.exception.CompanyHasApplicationsException;
import com.melina.jobtrail.mapper.CompanyMapper;
import com.melina.jobtrail.repository.CompanyRepository;
import com.melina.jobtrail.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyMapper companyMapper;
    private final UserService userService;

    public CompanyResponse createCompany(String email, CompanyRequest requestDto) {
        User user = userService.findUserOrThrow(email);
        Company company = Company.builder()
                .user(user)
                .name(requestDto.name())
                .website(requestDto.website())
                .location(requestDto.location())
                .build();
        company = companyRepository.save(company);
        return companyMapper.toResponse(company);
    }

    public List<CompanyResponse> getAllCompanies(String email) {
        User user = userService.findUserOrThrow(email);
        List<Company> companies = companyRepository.findAllByUserId(user.getId());
        return companyMapper.toResponseList(companies);
    }

    public CompanyResponse getCompanyById(String email, Long id) {
        User user = userService.findUserOrThrow(email);
        Company company = findCompanyOrThrow(id, user.getId());
        return companyMapper.toResponse(company);
    }

    public CompanyResponse updateCompany(String email, Long id, CompanyRequest requestDto) {
        User user = userService.findUserOrThrow(email);
        Company company = findCompanyOrThrow(id, user.getId());
        companyMapper.update(company,requestDto);
        company = companyRepository.save(company);
        return companyMapper.toResponse(company);
    }

    public void deleteCompany(String email, Long id) {
        User user = userService.findUserOrThrow(email);
        Company company = findCompanyOrThrow(id, user.getId());
        if (applicationRepository.existsByCompanyIdAndUserId(company.getId(), user.getId())) {
            throw new CompanyHasApplicationsException(company.getId());
        }
        companyRepository.delete(company);
    }

    Company findCompanyOrThrow(long id, long userId) {
        return companyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CompanyNotFoundException(id));
    }
}
