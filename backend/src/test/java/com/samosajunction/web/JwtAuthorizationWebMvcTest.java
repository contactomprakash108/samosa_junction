package com.samosajunction.web;

import com.samosajunction.auth.security.JwtProperties;
import com.samosajunction.auth.security.JwtService;
import com.samosajunction.staff.controller.StaffController;
import com.samosajunction.staff.service.StaffAuditService;
import com.samosajunction.staff.service.StaffOperationsService;
import com.samosajunction.support.service.SupportService;
import com.samosajunction.testsupport.SecureWebMvc;
import com.samosajunction.user.entity.Role;
import com.samosajunction.user.entity.RoleName;
import com.samosajunction.user.entity.User;
import com.samosajunction.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StaffController.class)
@SecureWebMvc
@TestPropertySource(properties = {
        "samosa.jwt.secret=test-jwt-secret-at-least-32-bytes!!",
        "samosa.jwt.expiration=15m",
        "samosa.jwt.issuer=samosa-junction-test",
        "samosa.jwt.audience=samosa-junction-api-test",
        "samosa.jwt.clock-skew=60s"
})
class JwtAuthorizationWebMvcTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID STAFF_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private StaffOperationsService staffOperationsService;

    @MockitoBean
    private StaffAuditService staffAuditService;

    @MockitoBean
    private SupportService supportService;

    private String customerToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        customerToken = jwtService.createAccessToken(CUSTOMER_ID);
        staffToken = jwtService.createAccessToken(STAFF_ID);

        User customer = new User("customer@samosa.test", "hash", "Customer");
        customer.addRole(new Role(RoleName.CUSTOMER));
        org.springframework.test.util.ReflectionTestUtils.setField(customer, "id", CUSTOMER_ID);

        User staff = new User("staff@samosa.test", "hash", "Staff");
        staff.addRole(new Role(RoleName.STAFF));
        org.springframework.test.util.ReflectionTestUtils.setField(staff, "id", STAFF_ID);

        when(userRepository.findWithRolesById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(userRepository.findWithRolesById(STAFF_ID)).thenReturn(Optional.of(staff));
    }

    @Test
    void staffEndpointRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/staff/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void customerJwtCannotAccessStaffEndpoint() throws Exception {
        mockMvc.perform(get("/api/staff/dashboard")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void staffJwtCanAccessStaffEndpoint() throws Exception {
        when(staffOperationsService.dashboard()).thenReturn(
                new com.samosajunction.staff.dto.StaffDashboardResponse(
                        java.time.Instant.parse("2026-01-01T00:00:00Z"),
                        0,
                        java.math.BigDecimal.ZERO,
                        0,
                        0,
                        0,
                        0,
                        0
                )
        );

        mockMvc.perform(get("/api/staff/dashboard")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk());
    }

    @Test
    void malformedJwtIsRejected() throws Exception {
        mockMvc.perform(get("/api/staff/dashboard")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
