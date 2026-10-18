package com.melina.jobtrail;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@ActiveProfiles("test")
public abstract class AbstractPostgresIntegrationTest {

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgres
            = new PostgreSQLContainer<>("postgres:16-alpine");

}
