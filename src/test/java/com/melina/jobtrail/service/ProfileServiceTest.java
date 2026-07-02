package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.profile.ProfileRequest;
import com.melina.jobtrail.dto.profile.ProfileResponse;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.entity.profile.Profile;
import com.melina.jobtrail.exception.ProfileAlreadyExistsException;
import com.melina.jobtrail.exception.ProfileNotFoundException;
import com.melina.jobtrail.mapper.profile.ProfileMapper;
import com.melina.jobtrail.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @InjectMocks
    private ProfileService profileService;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileMapper profileMapper;

    @Test
    void createProfile_withValidRequest_returnsProfileResponse() {
        String email = "user@example.com";
        User user = createUser(email, null);
        ProfileRequest request = createRequest("Backend Developer");
        Profile profile = createProfile(1L, "Backend Developer");
        ProfileResponse expectedResponse = createResponse(1L, "Backend Developer");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(profileMapper.toEntity(request)).thenReturn(profile);
        when(profileMapper.toResponse(profile)).thenReturn(expectedResponse);

        ProfileResponse response = profileService.createProfile(email, request);

        assertEquals(expectedResponse, response);
        assertSame(profile, user.getProfile());
        verify(userRepository).save(user);
    }

    @Test
    void createProfile_whenProfileAlreadyExists_throwsIllegalStateException() {
        String email = "user@example.com";
        User user = createUser(email, createProfile(1L, "Backend Developer"));
        when(userService.findUserOrThrow(email)).thenReturn(user);

        assertThrows(
                ProfileAlreadyExistsException.class,
                () -> profileService.createProfile(email, createRequest("Backend Developer"))
        );
        verifyNoInteractions(profileMapper, userRepository);
    }

    @Test
    void getProfile_returnsProfileResponse() {
        String email = "user@example.com";
        Profile profile = createProfile(1L, "Backend Developer");
        ProfileResponse expectedResponse = createResponse(1L, "Backend Developer");

        when(userService.getProfile(email)).thenReturn(profile);
        when(profileMapper.toResponse(profile)).thenReturn(expectedResponse);

        ProfileResponse response = profileService.getProfile(email);

        assertEquals(expectedResponse, response);
    }

    @Test
    void updateProfile_withExistingProfile_returnsProfileResponse() {
        String email = "user@example.com";
        Profile profile = createProfile(1L, "Backend Developer");
        User user = createUser(email, profile);
        ProfileRequest request = createRequest("Software Engineer");
        ProfileResponse expectedResponse = createResponse(1L, "Software Engineer");

        when(userService.findUserOrThrow(email)).thenReturn(user);
        when(profileMapper.toResponse(profile)).thenReturn(expectedResponse);

        ProfileResponse response = profileService.updateProfile(email, request);

        assertEquals(expectedResponse, response);
        verify(profileMapper).updateEntityFromRequest(profile, request);
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_whenProfileDoesNotExist_throwsIllegalStateException() {
        String email = "user@example.com";
        User user = createUser(email, null);
        when(userService.findUserOrThrow(email)).thenReturn(user);

        assertThrows(
                ProfileNotFoundException.class,
                () -> profileService.updateProfile(email, createRequest("Software Engineer"))
        );
        verifyNoInteractions(profileMapper, userRepository);
    }

    @Test
    void deleteProfile_removesProfile() {
        String email = "user@example.com";
        User user = createUser(email, createProfile(1L, "Backend Developer"));
        when(userService.findUserOrThrow(email)).thenReturn(user);

        profileService.deleteProfile(email);

        assertNull(user.getProfile());
        verify(userRepository).save(user);
    }

    @Test
    void deleteProfile_whenProfileDoesNotExist_throwsProfileNotFoundException() {
        String email = "user@example.com";
        User user = createUser(email, null);
        when(userService.findUserOrThrow(email)).thenReturn(user);

        assertThrows(ProfileNotFoundException.class, () -> profileService.deleteProfile(email));

        verifyNoInteractions(userRepository);
    }

    private ProfileRequest createRequest(String targetRole) {
        return new ProfileRequest(
                targetRole,
                "Berlin",
                "Immediately",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private ProfileResponse createResponse(long id, String targetRole) {
        return new ProfileResponse(
                id,
                targetRole,
                "Berlin",
                "Immediately",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Profile createProfile(long id, String targetRole) {
        return Profile.builder().id(id).targetRole(targetRole).build();
    }

    private User createUser(String email, Profile profile) {
        return User.builder().id(1L).email(email).profile(profile).build();
    }
}
