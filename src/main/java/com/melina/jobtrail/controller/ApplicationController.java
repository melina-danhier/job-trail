package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.ApplicationDto;
import com.melina.jobtrail.dto.RequestApplicationDto;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationDto> createApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RequestApplicationDto requestDto
    ) {
        ApplicationDto dto = applicationService.createApplication(userDetails.email(),requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<ApplicationDto>> getApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(applicationService.getApplications(userDetails.email()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDto> getApplicationById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable long id
    ) {
        ApplicationDto dto = applicationService.getApplicationById(userDetails.email(),id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationDto> updateApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable long id,
            @Valid @RequestBody RequestApplicationDto requestDto
    ) {
        ApplicationDto dto = applicationService.updateApplication(userDetails.email(),id,requestDto);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable long id
    ) {
        applicationService.deleteApplication(userDetails.email(),id);
        return ResponseEntity.noContent().build();
    }
}
