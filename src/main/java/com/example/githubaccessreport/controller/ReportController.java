package com.example.githubaccessreport.controller;

import com.example.githubaccessreport.model.RepositoryAccess;
import com.example.githubaccessreport.service.AccessReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/github")
public class ReportController {

    private final AccessReportService reportService;

    public ReportController(AccessReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/access-report")
    public Mono<Map<String, List<RepositoryAccess>>> getAccessReport(@RequestParam String org) {
        return reportService.generateReport(org);
    }
}
