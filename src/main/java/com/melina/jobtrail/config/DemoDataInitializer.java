package com.melina.jobtrail.config;

import com.melina.jobtrail.service.DemoDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jobtrail.demo-data.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    private final DemoDataService demoDataService;

    @Override
    public void run(String... args) {
        log.info("Checking and initializing demo data for showcase & recruiter testing...");
        demoDataService.initDemoData(false);
        log.info("Demo data ready: user '{}' is available.", DemoDataService.DEMO_EMAIL);
    }
}
