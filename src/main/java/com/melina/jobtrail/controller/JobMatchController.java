package com.melina.jobtrail.controller;

import com.melina.jobtrail.dto.JobMatchRequest;
import com.melina.jobtrail.dto.JobMatchResponse;
import com.melina.jobtrail.security.CustomUserDetails;
import com.melina.jobtrail.service.JobMatchAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-match")
@RequiredArgsConstructor
public class JobMatchController {
    private final JobMatchAiService jobMatchAiService;

    @PostMapping("/analyze")
    public ResponseEntity<JobMatchResponse> analyze(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody JobMatchRequest request
    ) {
        JobMatchResponse response = jobMatchAiService.analyze(userDetails.email(), request);
        return ResponseEntity.ok(response);
    }

}
