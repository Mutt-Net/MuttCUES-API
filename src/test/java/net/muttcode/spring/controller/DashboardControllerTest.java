package net.muttcode.spring.controller;

import net.muttcode.spring.config.JwtAuthenticationFilter;
import net.muttcode.spring.service.CustomUserDetailsService;
import net.muttcode.spring.service.DashboardService;
import net.muttcode.spring.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void dashboardSummary_returns200WithContract() throws Exception {
        when(dashboardService.getDashboardSummary()).thenReturn(Map.of(
                "alerts", List.of(),
                "recent", List.of(),
                "upcoming", List.of()
        ));

        mockMvc.perform(get("/api/dashboard-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.recent").isArray())
                .andExpect(jsonPath("$.upcoming").isArray());
    }

    @Test
    void status_returns200WithRequiredFields() throws Exception {
        when(dashboardService.getStatus()).thenReturn(Map.of(
                "queue_depth", 0,
                "jobs", Map.of("completed_24h", 5, "failed_24h", 0, "running", 0),
                "recent_jobs", List.of(),
                "models", List.of("ultramix_balanced"),
                "storage", Map.of("uploads_mb", 100, "outputs_mb", 500)
        ));

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queue_depth").exists())
                .andExpect(jsonPath("$.jobs.completed_24h").value(5))
                .andExpect(jsonPath("$.models").isArray())
                .andExpect(jsonPath("$.storage.uploads_mb").exists());
    }
}
