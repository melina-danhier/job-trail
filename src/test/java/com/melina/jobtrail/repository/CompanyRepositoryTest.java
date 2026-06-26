package com.melina.jobtrail.repository;

import com.melina.jobtrail.entity.Company;
import com.melina.jobtrail.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByIdAndUserId_withExistingIdAndUserId_returnsCompany() {
        User user = createUser("test@example.com");
        Company company = createCompany(user, "Test Company");

        Optional<Company> foundCompany = companyRepository.findByIdAndUserId(
                company.getId(), user.getId()
        );

        assertTrue(foundCompany.isPresent());
        assertEquals(company.getId(), foundCompany.get().getId());
    }

    @Test
    void findByIdAndUserId_withNonExistingId_returnsEmptyOptional() {
        User user = createUser("test@example.com");

        Optional<Company> foundCompany = companyRepository.findByIdAndUserId(1L, user.getId());

        assertFalse(foundCompany.isPresent());
    }

    @Test
    void findByIdAndUserId_withIdFromOtherUser_returnsEmptyOptional() {
        User user = createUser("test@example.com");
        User otherUser = createUser("other@example.com");
        Company company = createCompany(user, "Test Company");

        Optional<Company> foundCompany = companyRepository.findByIdAndUserId(
                company.getId(), otherUser.getId()
        );

        assertFalse(foundCompany.isPresent());
    }

    @Test
    void findByIdAndUserId_withNonExistingUserId_returnsEmptyOptional() {
        User user = createUser("test@example.com");
        Company company = createCompany(user, "Test Company");

        Optional<Company> foundCompany = companyRepository.findByIdAndUserId(
                company.getId(), user.getId() + 1
        );

        assertFalse(foundCompany.isPresent());
    }

    @Test
    void findAllByUserId_withExistingUserId_returnsAllCompaniesFromThatUser() {
        User user = createUser("test@example.com");
        User otherUser = createUser("other@example.com");

        createCompany(user, "Test Company");
        createCompany(otherUser, "Other Company");

        List<Company> foundCompanies = companyRepository.findAllByUserId(user.getId());

        assertTrue(foundCompanies.size() == 1);
    }

    @Test
    void findAllByUserId_withNonExistingUserId_returnsEmptyList() {
        List<Company> foundCompanies = companyRepository.findAllByUserId(1L);

        assertTrue(foundCompanies.isEmpty());
    }

    @Test
    void findAllByUserId_withNonExistingCompanies_returnsEmptyList() {
        User user = createUser("test@example.com");

        List<Company> foundCompanies = companyRepository.findAllByUserId(user.getId());

        assertTrue(foundCompanies.isEmpty());
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

    private Company createCompany(User user, String name) {
        Company company = Company.builder()
                .name(name)
                .user(user)
                .build();
        entityManager.persist(company);
        entityManager.flush();
        entityManager.clear();
        return entityManager.find(Company.class, company.getId());
    }
}