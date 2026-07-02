package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.profile.ProfileRequest;
import com.melina.jobtrail.dto.profile.ProfileResponse;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @PostMapping
    public ResponseEntity<ProfileResponse> createProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileRequest request
    ) {
        ProfileResponse profile = profileService.createProfile(userDetails.email(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ProfileResponse profile = profileService.getProfile(userDetails.email());
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileRequest request
    ) {
        ProfileResponse profile = profileService.updateProfile(userDetails.email(), request);
        return ResponseEntity.ok(profile);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        profileService.deleteProfile(userDetails.email());
        return ResponseEntity.noContent().build();
    }

}
