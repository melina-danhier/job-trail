package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.CompanyDto;
import com.melina.jobtrail.dto.RequestCompanyDto;
import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.exception.CompanyNotFoundException;
import com.melina.jobtrail.mapper.CompanyMapper;
import com.melina.jobtrail.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final UserService userService;

    public CompanyDto createCompany(String email, RequestCompanyDto requestDto) {
        User user = userService.findUserOrThrow(email);
        Company company = Company.builder()
                .user(user)
                .name(requestDto.name())
                .website(requestDto.website())
                .location(requestDto.location())
                .build();
        company = companyRepository.save(company);
        return companyMapper.toDto(company);
    }

    public List<CompanyDto> getAllCompanies(String email) {
        User user = userService.findUserOrThrow(email);
        List<Company> companies = companyRepository.findAllByUserId(user.getId());
        return companyMapper.toDtoList(companies);
    }

    public CompanyDto getCompanyById(String email, Long id) {
        User user = userService.findUserOrThrow(email);
        Company company = findCompanyOrThrow(id, user.getId());
        return companyMapper.toDto(company);
    }

    public CompanyDto updateCompany(String email, Long id, RequestCompanyDto requestDto) {
        User user = userService.findUserOrThrow(email);
        Company company = findCompanyOrThrow(id, user.getId());
        companyMapper.update(company,requestDto);
        company = companyRepository.save(company);
        return companyMapper.toDto(company);
    }

    public void deleteCompany(String email, Long id) {
        User user = userService.findUserOrThrow(email);
        Company company = findCompanyOrThrow(id, user.getId());
        companyRepository.delete(company);
    }

    Company findCompanyOrThrow(long id, long userId) {
        return companyRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CompanyNotFoundException(id));
    }
}
