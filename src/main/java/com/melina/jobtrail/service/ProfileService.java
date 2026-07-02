package com.melina.jobtrail.service;

import com.melina.jobtrail.dto.profile.ProfileRequest;
import com.melina.jobtrail.dto.profile.ProfileResponse;
import com.melina.jobtrail.entity.profile.Profile;
import com.melina.jobtrail.entity.User;
import com.melina.jobtrail.exception.ProfileAlreadyExistsException;
import com.melina.jobtrail.exception.ProfileNotFoundException;
import com.melina.jobtrail.mapper.profile.ProfileMapper;
import com.melina.jobtrail.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {
    private final UserService userService;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;

    public ProfileResponse createProfile(String email, ProfileRequest request) {
        User user = userService.findUserOrThrow(email);
        if (user.getProfile() != null) {
            throw new ProfileAlreadyExistsException();
        }
        Profile profile = profileMapper.toEntity(request);
        user.setProfile(profile);
        userRepository.save(user);
        return profileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String email) {
        Profile profile = userService.getProfile(email);
        return profileMapper.toResponse(profile);
    }

    public ProfileResponse updateProfile(String email, ProfileRequest request) {
        User user = userService.findUserOrThrow(email);
        Profile profile = user.getProfile();
        if (profile == null) {
            throw new ProfileNotFoundException();
        }
        profileMapper.updateEntityFromRequest(profile, request);
        userRepository.save(user);
        return profileMapper.toResponse(profile);
    }

    public void deleteProfile(String email) {
        User user = userService.findUserOrThrow(email);
        if (user.getProfile() == null) {
            throw new ProfileNotFoundException();
        }
        user.setProfile(null);
        userRepository.save(user);
    }

}
