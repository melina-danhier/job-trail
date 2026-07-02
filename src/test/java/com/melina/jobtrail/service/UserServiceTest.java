package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.RegisterRequest;
import com.melina.jobtrail.dto.UserResponse;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.exception.EmailAlreadyExistsException;
import com.melina.jobtrail.exception.UserNotFoundException;
import com.melina.jobtrail.mapper.UserMapper;
import com.melina.jobtrail.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void getMe_withValidEmail_returnsUserResponse() {
        String email = "user@example.com";
        User user = createUser(email);
        UserResponse expectedResponse = new UserResponse(email, null);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse response = userService.getMe(email);

        assertEquals(expectedResponse, response);
    }

    @Test
    void getMe_withUnknownEmail_throwsUserNotFoundException() {
        String email = "missing@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getMe(email));
        verifyNoInteractions(userMapper);
    }

    @Test
    void createUser_withValidRequest_returnsUserResponse() {
        RegisterRequest request = new RegisterRequest(" User@Example.com ", "password123");
        UserResponse expectedResponse = new UserResponse("user@example.com", null);

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        UserResponse response = userService.createUser(request);

        assertEquals(expectedResponse, response);
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("user@example.com")
                        && user.getPasswordHash().equals("encodedPassword")
        ));
    }

    @Test
    void createUser_withExistingEmail_throwsEmailAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> userService.createUser(request));
        verifyNoInteractions(passwordEncoder, userMapper);
    }

    private User createUser(String email) {
        return User.builder().id(1L).email(email).build();
    }
}
