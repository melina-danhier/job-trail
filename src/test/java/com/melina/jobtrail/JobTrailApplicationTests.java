package com.melina.jobtrail;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest
@Slf4j
class JobTrailApplicationTests extends AbstractPostgresIntegrationTest {

    @Test
    void contextLoads() {
        log.debug("contextLoads");
    }

}
