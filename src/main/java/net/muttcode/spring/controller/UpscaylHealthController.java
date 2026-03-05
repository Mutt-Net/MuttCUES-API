package net.muttcode.spring.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for Upscayl service health checks.
 * Provides endpoint to verify Upscayl microservice availability.
 */
@RestController
@RequestMapping("/api/upscayl")
public class UpscaylHealthController {

  private final RestTemplate restTemplate;

  @Value("${upscayl.service.url:http://upscayl:8081}")
  private String upscaylServiceUrl;

  public UpscaylHealthController() {
    this.restTemplate = new RestTemplate();
  }

  /**
   * Health check endpoint for Upscayl service.
   * Attempts to connect to the Upscayl microservice to verify availability.
   * @return Map with service name and status (available/unavailable)
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    Map<String, String> response = new HashMap<>();
    response.put("service", "upscayl");

    try {
      // Try to reach the Upscayl service
      // Using a simple GET request to the base URL
      restTemplate.getForObject(upscaylServiceUrl, String.class);
      response.put("status", "available");
      return ResponseEntity.ok(response);
    } catch (ResourceAccessException e) {
      // Connection failed - service unavailable
      response.put("status", "unavailable");
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      // Other errors - service may be unavailable or returning unexpected response
      response.put("status", "unavailable");
      return ResponseEntity.ok(response);
    }
  }
}
