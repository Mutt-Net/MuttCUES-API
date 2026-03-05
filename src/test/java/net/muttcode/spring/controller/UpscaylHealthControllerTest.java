package net.muttcode.spring.controller;

import net.muttcode.spring.config.JwtAuthenticationFilter;
import net.muttcode.spring.service.CustomUserDetailsService;
import net.muttcode.spring.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UpscaylHealthController.
 * Tests verify the health endpoint behavior.
 */
@WebMvcTest(UpscaylHealthController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class UpscaylHealthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockBean
  private JwtService jwtService;

  @MockBean
  private CustomUserDetailsService userDetailsService;

  @Test
  void healthEndpoint_shouldReturnServiceNameAndStatus() throws Exception {
    // Note: In a real scenario, the Upscayl service may or may not be available.
    // This test verifies the endpoint returns the correct structure.
    // The status will be "available" if Upscayl is running, "unavailable" otherwise.
    mockMvc.perform(get("/api/upscayl/health"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.service").value("upscayl"))
        .andExpect(jsonPath("$.status").exists());
  }

  @Test
  void healthEndpoint_shouldReturnValidStatusValues() throws Exception {
    // Status should be either "available" or "unavailable"
    String response = mockMvc.perform(get("/api/upscayl/health"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    // Verify response contains valid status
    org.junit.jupiter.api.Assertions.assertTrue(
        response.contains("\"status\":\"available\"") || 
        response.contains("\"status\":\"unavailable\""),
        "Status should be either 'available' or 'unavailable'"
    );
  }

  @Test
  void healthEndpoint_shouldReturnJsonContentType() throws Exception {
    mockMvc.perform(get("/api/upscayl/health"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  void healthEndpoint_shouldReturnHttp200() throws Exception {
    mockMvc.perform(get("/api/upscayl/health"))
        .andExpect(status().isOk());
  }
}
