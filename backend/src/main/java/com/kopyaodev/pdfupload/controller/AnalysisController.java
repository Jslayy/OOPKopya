package com.kopyaodev.pdfupload.controller;

import com.kopyaodev.pdfupload.service.PdfAnalysisService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ogretmen/analysis")
public class AnalysisController {

    private final PdfAnalysisService analysisService;

    public AnalysisController(PdfAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PreAuthorize("hasRole('OGRETMEN')")
    @PostMapping("/start")
    public String start() {
        analysisService.startAnalysis();
        return "Analysis job triggered";
    }
}
