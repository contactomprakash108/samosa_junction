package com.samosajunction.web;

import com.samosajunction.auth.security.JwtService;
import com.samosajunction.complaint.controller.ComplaintController;
import com.samosajunction.complaint.dto.ComplaintResponse;
import com.samosajunction.complaint.entity.ComplaintCategory;
import com.samosajunction.complaint.entity.ComplaintPriority;
import com.samosajunction.complaint.entity.ComplaintStatus;
import com.samosajunction.complaint.service.ComplaintService;
import com.samosajunction.order.controller.OrderController;
import com.samosajunction.order.dto.OrderResponse;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.service.OrderService;
import com.samosajunction.testsupport.SecureWebMvc;
import com.samosajunction.testsupport.WithSamosaUser;
import com.samosajunction.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OrderController.class, ComplaintController.class})
@SecureWebMvc
class OwnershipWebMvcTest {

    private static final UUID ORDER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID COMPLAINT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ComplaintService complaintService;

    @MockitoBean
    private com.samosajunction.staff.service.StaffAuditService staffAuditService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void orderRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", ORDER_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithSamosaUser
    void customerCanReadOwnOrderThroughController() throws Exception {
        when(orderService.get(any(), eq(ORDER_ID))).thenReturn(order());

        mockMvc.perform(get("/api/orders/{id}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()));
    }

    @Test
    void complaintCreateRequiresJwt() throws Exception {
        mockMvc.perform(post("/api/complaints")
                        .header("Idempotency-Key", "c-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"%s","category":"DAMAGED","description":"My samosas arrived damaged."}
                                """.formatted(ORDER_ID)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithSamosaUser
    void customerCannotPatchComplaintStatus() throws Exception {
        mockMvc.perform(patch("/api/complaints/{id}", COMPLAINT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verify(complaintService, never()).update(any(), any());
    }

    @Test
    @WithSamosaUser(role = "STAFF")
    void staffCanPatchComplaintStatus() throws Exception {
        when(complaintService.update(eq(COMPLAINT_ID), any())).thenReturn(complaint());

        mockMvc.perform(patch("/api/complaints/{id}", COMPLAINT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    private static OrderResponse order() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new OrderResponse(
                ORDER_ID,
                OrderStatus.CONFIRMED,
                "Ada",
                "1 Road",
                "Pune",
                "MH",
                "411001",
                new BigDecimal("40.00"),
                List.of(),
                now
        );
    }

    private static ComplaintResponse complaint() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new ComplaintResponse(
                COMPLAINT_ID,
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                ORDER_ID,
                ComplaintCategory.DAMAGED,
                "My samosas arrived damaged.",
                ComplaintStatus.IN_PROGRESS,
                ComplaintPriority.MEDIUM,
                now,
                now,
                List.of()
        );
    }
}
