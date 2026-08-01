package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.application.ApplicationCreateRequest;
import com.melina.jobtrail.dto.application.ApplicationResponse;
import com.melina.jobtrail.dto.application.ApplicationUpdateRequest;
import com.melina.jobtrail.dto.application.ApplicationUpdateStatusRequest;
import com.melina.jobtrail.dto.application.ApplicationStatusHistoryResponse;
import com.melina.jobtrail.dto.PageResponse;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.service.ApplicationService;
import com.melina.jobtrail.util.ApplicationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Validated
public class ApplicationController {
    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ApplicationCreateRequest requestDto
    ) {
        ApplicationResponse dto = applicationService.createApplication(userDetails.email(),requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ApplicationResponse>> getApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate applicationDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate applicationDateTo
    ) {
        return ResponseEntity.ok(applicationService.getApplications(
                userDetails.email(), page, size, sortBy, direction,
                status, companyId, applicationDateFrom, applicationDateTo
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable long id
    ) {
        ApplicationResponse dto = applicationService.getApplicationById(userDetails.email(),id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationResponse> updateApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable long id,
            @Valid @RequestBody ApplicationUpdateRequest requestDto
    ) {
        ApplicationResponse dto = applicationService.updateApplication(userDetails.email(),id,requestDto);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateApplicationStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable long id,
            @Valid @RequestBody ApplicationUpdateStatusRequest requestDto
    ) {
        ApplicationResponse dto = applicationService.updateApplicationStatus(userDetails.email(),id,requestDto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/status-history")
    public ResponseEntity<List<ApplicationStatusHistoryResponse>> getStatusHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable long id
    ) {
        return ResponseEntity.ok(applicationService.getStatusHistory(userDetails.email(), id));
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
