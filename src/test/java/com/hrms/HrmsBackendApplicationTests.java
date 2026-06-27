package com.hrms;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads the full Spring application context against the H2 test profile. This is deliberately
 * not a per-feature test — its only job is to catch the class of bug unit tests can't:
 * a missing bean, a broken @Configuration, a JPA mapping that doesn't actually map, a
 * security filter chain that fails to assemble. If this test goes red, something in the
 * wiring is broken regardless of whether any individual service's logic is correct.
 */
@SpringBootTest
@ActiveProfiles("test")
class HrmsBackendApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void allControllersAreRegistered() {
        String[] expectedControllerBeans = {
                "authController", "userController", "employeeController", "masterDataController",
                "leaveController", "leaveEncashmentController", "medicalClaimController",
                "pfController", "postingController", "roleController", "permissionController",
                "dashboardController"
        };
        for (String beanName : expectedControllerBeans) {
            assertThat(applicationContext.containsBean(beanName))
                    .as("Expected controller bean '%s' to be registered", beanName)
                    .isTrue();
        }
    }
}
