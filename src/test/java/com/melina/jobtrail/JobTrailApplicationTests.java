package com.melina.jobtrail;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class JobTrailApplicationTests {

    @Test
    void contextLoads() {
        log.debug("contextLoads");
    }

}
