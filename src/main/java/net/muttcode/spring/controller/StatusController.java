package net.muttcode.spring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
            "status", "online",
            "models", List.of(
                "realesrgan-x4plus",
                "realesrgan-x2plus",
                "realcugan",
                "ultramix-balanced",
                "ultramix_balanced",
                "4x_NMKD-Superscale-SP_178000_G"
            )
        );
    }
}
