package com.pso.knowledge.controller;

import com.pso.knowledge.service.SanitizeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SanitizeController {

    private final SanitizeService sanitizeService;

    public SanitizeController(SanitizeService sanitizeService) {
        this.sanitizeService = sanitizeService;
    }

    @GetMapping("/jobs/sanitize")
    public Map<String, Object> sanitize(@RequestParam(defaultValue = "false") boolean full) {
        int count = full ? sanitizeService.fullSanitize() : sanitizeService.sanitize();
        return Map.of("status", "completed", "filesProcessed", count, "full", full);
    }
}
