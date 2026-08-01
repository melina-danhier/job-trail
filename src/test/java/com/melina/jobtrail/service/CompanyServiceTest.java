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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @InjectMocks
    private CompanyService companyService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private UserService userService;

    @Test
    void createCompany_withValidRequest_returnsCompanyResponse() {
        String email = "user@example.com";
        User user = createUser(email);
        CompanyRequest request = createRequest("Company Name");
        CompanyResponse expectedResponse = createResponse(1L, "Company Name");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(companyRepository.save(any(Company.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(companyMapper.toResponse(any(Company.class)))
                .thenReturn(expectedResponse);

        CompanyResponse response = companyService.createCompany(email, request);

        assertEquals(expectedResponse, response);
        verify(companyRepository).save(argThat(company ->
                company.getUser() == user && company.getName().equals(request.name())
        ));
    }

    @Test
    void getAllCompanies_returnsCompanyResponses() {
        String email = "user@example.com";
        User user = createUser(email);
        List<Company> companies = List.of(createCompany(1L, "Company Name"));
        List<CompanyResponse> expectedResponses = List.of(createResponse(1L, "Company Name"));

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(companyRepository.findAllByUserId(user.getId())).thenReturn(companies);
        when(companyMapper.toResponseList(companies)).thenReturn(expectedResponses);

        List<CompanyResponse> responses = companyService.getAllCompanies(email);

        assertEquals(expectedResponses, responses);
    }

    @Test
    void getCompanyById_withValidId_returnsCompanyResponse() {
        String email = "user@example.com";
        User user = createUser(email);
        Company company = createCompany(1L, "Company Name");
        CompanyResponse expectedResponse = createResponse(1L, "Company Name");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(companyRepository.findByIdAndUserId(company.getId(), user.getId()))
                .thenReturn(Optional.of(company));
        when(companyMapper.toResponse(company)).thenReturn(expectedResponse);

        CompanyResponse response = companyService.getCompanyById(email, company.getId());

        assertEquals(expectedResponse, response);
    }

    @Test
    void getCompanyById_withInvalidId_throwsCompanyNotFoundException() {
        String email = "user@example.com";
        User user = createUser(email);

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(companyRepository.findByIdAndUserId(99L, user.getId())).thenReturn(Optional.empty());

        assertThrows(
                CompanyNotFoundException.class,
                () -> companyService.getCompanyById(email, 99L)
        );
        verifyNoInteractions(companyMapper);
    }

    @Test
    void updateCompany_withValidRequest_returnsCompanyResponse() {
        String email = "user@example.com";
        User user = createUser(email);
        Company company = createCompany(1L, "Company Name");
        CompanyRequest request = createRequest("Updated Company");
        CompanyResponse expectedResponse = createResponse(1L, "Updated Company");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(companyRepository.findByIdAndUserId(company.getId(), user.getId()))
                .thenReturn(Optional.of(company));
        when(companyRepository.save(company)).thenReturn(company);
        when(companyMapper.toResponse(company)).thenReturn(expectedResponse);

        CompanyResponse response = companyService.updateCompany(email, company.getId(), request);

        assertEquals(expectedResponse, response);
        verify(companyMapper).update(company, request);
        verify(companyRepository).save(company);
    }

    @Test
    void deleteCompany_withValidId_deletesCompany() {
        String email = "user@example.com";
        User user = createUser(email);
        Company company = createCompany(1L, "Company Name");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(companyRepository.findByIdAndUserId(company.getId(), user.getId()))
                .thenReturn(Optional.of(company));

        companyService.deleteCompany(email, company.getId());

        verify(companyRepository).delete(company);
    }

    @Test
    void deleteCompany_withApplications_returnsDomainConflictWithoutDeleting() {
        String email = "user@example.com";
        User user = createUser(email);
        Company company = createCompany(1L, "Company Name");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(companyRepository.findByIdAndUserId(company.getId(), user.getId()))
                .thenReturn(Optional.of(company));
        when(applicationRepository.existsByCompanyIdAndUserId(company.getId(), user.getId()))
                .thenReturn(true);

        assertThrows(
                CompanyHasApplicationsException.class,
                () -> companyService.deleteCompany(email, company.getId())
        );

        verify(companyRepository, never()).delete(any());
    }

    @Test
    void updateCompany_ownedByAnotherUser_isRejectedWithoutChanges() {
        String email = "other@example.com";
        User otherUser = createUser(email);
        otherUser.setId(2L);
        CompanyRequest request = createRequest("Changed company");

        when(userService.findUserOrThrow(email)).thenReturn(otherUser);
        when(companyRepository.findByIdAndUserId(1L, otherUser.getId()))
                .thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class,
                () -> companyService.updateCompany(email, 1L, request));

        verify(companyRepository, never()).save(any());
        verifyNoInteractions(companyMapper);
    }

    @Test
    void deleteCompany_ownedByAnotherUser_isRejectedWithoutDeletion() {
        String email = "other@example.com";
        User otherUser = createUser(email);
        otherUser.setId(2L);

        when(userService.findUserOrThrow(email)).thenReturn(otherUser);
        when(companyRepository.findByIdAndUserId(1L, otherUser.getId()))
                .thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class,
                () -> companyService.deleteCompany(email, 1L));

        verify(companyRepository, never()).delete(any());
    }

    private CompanyRequest createRequest(String name) {
        return new CompanyRequest(name, null, null);
    }

    private CompanyResponse createResponse(long id, String name) {
        return new CompanyResponse(id, name, null, null);
    }

    private Company createCompany(long id, String name) {
        return Company.builder().id(id).name(name).build();
    }

    private User createUser(String email) {
        return User.builder().id(1L).email(email).build();
    }
}
