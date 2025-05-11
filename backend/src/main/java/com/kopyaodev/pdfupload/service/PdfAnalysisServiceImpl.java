package com.kopyaodev.pdfupload.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class PdfAnalysisServiceImpl implements PdfAnalysisService {

    @Value("${analysis.hf-token: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}")
    private String hfToken;

    @Value("${analysis.pdf-dir:C:\\Users\\rootz\\Desktop\\kopyaodev\\Kopia\\pdfs}")
    private String pdfDir;

    @Value("${analysis.report-file:C:\\Users\\rootz\\Desktop\\kopyaodev\\Kopia\\pdfs\\rapor.html}")
    private String reportFile;

    @Value("${analysis.detector-dir:C:\\Users\\rootz\\Desktop\\kopyaodev\\Kopia\\}")
    private String detectorDir;


    @PostConstruct
    void init() {
        if (hfToken != null) hfToken = hfToken.trim();
    }

    @Override
    public void startAnalysis() {
        if (hfToken == null || hfToken.isBlank()) {
            throw new IllegalStateException("HF_TOKEN missing – check analysis.hf-token property");
        }

        // Hızlı debug için token uzunluğunu göster (ilk 5 karakteri maskeli)
        System.out.println("[DEBUG] HF token length=" + hfToken.length() + " first5=" + hfToken.substring(0, 5) + "…");

        try {
            runMaven(detectorDir, "clean", "compile");

            runMaven(
                detectorDir,
                "exec:java",
                "-Dexec.mainClass=com.kawaki.PdfPlagiarismDetector",
                "-Dexec.args=" + quote(pdfDir) + " " + quote(reportFile)
            );

            System.out.println("📄  Plagiarism report generated → " + reportFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start analysis", e);
        }
    }

    // OSa gore system binary sec 
    private static String mvnCmd() {
        boolean win = System.getProperty("os.name").toLowerCase().contains("win");
        return win ? "mvnw.cmd" : "./mvnw";
    }

    private void runMaven(String workingDir, String... args) throws Exception {
        List<String> cmd = new ArrayList<>();

        //  Maven executable sec -----------------------------------------------------------
        String mvn = mvnCmd();
        if (!Files.exists(Path.of(workingDir, mvn))) {
            mvn = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
        }
        cmd.add(mvn);

        cmd.add("-DHF_TOKEN=" + hfToken);

        cmd.addAll(Arrays.asList(args));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(workingDir));
        pb.environment().put("HF_TOKEN", hfToken);          // env‑var path
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);  // stream output directly
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        int exit = pb.start().waitFor();
        if (exit != 0) {
            throw new IllegalStateException("Maven command " + cmd + " exited with code " + exit);
        }
    }

    private static String quote(String s) {
        return s.contains(" ") ? "\"" + s + "\"" : s;
    }
}
