package net.muttcode.spring.controller;

import net.muttcode.spring.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard-summary")
    public Map<String, Object> dashboardSummary() {
        return dashboardService.getDashboardSummary();
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return dashboardService.getStatus();
    }
}
