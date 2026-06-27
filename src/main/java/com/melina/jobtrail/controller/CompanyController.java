package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.CompanyRequest;
import com.melina.jobtrail.dto.CompanyResponse;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CompanyRequest requestDto
    ) {
        CompanyResponse dto = companyService.createCompany(userDetails.email(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getAllCompanies(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<CompanyResponse> dto = companyService.getAllCompanies(userDetails.email());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompanyById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        CompanyResponse dto = companyService.getCompanyById(userDetails.email(), id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponse> updateCompany(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody CompanyRequest requestDto
    ) {
        CompanyResponse dto = companyService.updateCompany(userDetails.email(), id, requestDto);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        companyService.deleteCompany(userDetails.email(), id);
        return ResponseEntity.noContent().build();
    }
}
