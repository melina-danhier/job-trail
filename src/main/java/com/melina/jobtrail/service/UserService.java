package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.RegisterRequest;
import com.melina.jobtrail.dto.UserResponse;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.entity.profile.Profile;
import com.melina.jobtrail.exception.EmailAlreadyExistsException;
import com.melina.jobtrail.exception.ProfileNotFoundException;
import com.melina.jobtrail.exception.UserNotFoundException;
import com.melina.jobtrail.mapper.UserMapper;
import com.melina.jobtrail.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getMe(String email) {
        User user = findUserOrThrow(email);
        return userMapper.toResponse(user);
    }

    public UserResponse createUser(RegisterRequest registerRequest) {
        String normalizedEmail = normalizeEmail(registerRequest.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(registerRequest.password()))
                .build();
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    Profile getProfile(String email) {
        User user = findUserOrThrow(email);
        if (user.getProfile() == null) {
            throw new ProfileNotFoundException();
        }
        return user.getProfile();
    }

    String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    User findUserOrThrow(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new UserNotFoundException(email));
    }
}
