package com.melina.jobtrail.integration;

import com.melina.jobtrail.AbstractPostgresIntegrationTest;
import org.springframework.test.context.jdbc.Sql;

@Sql(
        statements = {
                "DELETE FROM application_status_history",
                "DELETE FROM applications",
                "DELETE FROM companies",
                "DELETE FROM users",
                "DELETE FROM profile_project_technologies",
                "DELETE FROM profile_projects",
                "DELETE FROM profile_skills",
                "DELETE FROM profile_languages",
                "DELETE FROM profile_preferred_roles",
                "DELETE FROM profile_avoid_keywords",
                "DELETE FROM profiles"
        },
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
public abstract class AbstractPostgresCleanIntegrationTest extends AbstractPostgresIntegrationTest {
}
