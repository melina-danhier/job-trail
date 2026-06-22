package com.melina.jobtrail.repository;

import com.melina.jobtrail.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByEmail_withExistingEmail_returnsOptionalWithUser() {
        User user = User.builder()
                .email("user@example.com")
                .passwordHash("password1234")
                .build();
        entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> result = userRepository.findByEmail("user@example.com");
        assertTrue(result.isPresent());
        assertAll(
                () -> assertEquals(user.getId(), result.get().getId()),
                () -> assertEquals(user.getEmail(), result.get().getEmail()),
                () -> assertEquals(user.getPasswordHash(), result.get().getPasswordHash()),
                () -> assertNotNull(result.get().getCreatedAt())
        );
    }

    @Test
    void findByEmail_withNonExistingEmail_returnEmptyOptional() {
        Optional<User> result = userRepository.findByEmail("nonexisting@example.com");
        assertTrue(result.isEmpty());
    }
}
