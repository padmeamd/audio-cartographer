package com.dakonst.audio_cartographer.controller;


import com.dakonst.audio_cartographer.dto.AnalysisResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.dakonst.audio_cartographer.service.AudioAnalysisService;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/analyze")
public class AudioAnalysisController  {

    private final AudioAnalysisService audioAnalysisService;

    public AudioAnalysisController(AudioAnalysisService audioAnalysisService) {
        this.audioAnalysisService = audioAnalysisService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResult analyze(@RequestParam("file") MultipartFile file) throws Exception {

        Path temp = Files.createTempFile("audio_", ".tmp");
        file.transferTo(temp);

        AnalysisResult result = audioAnalysisService.analyze(temp.toFile());

        Files.deleteIfExists(temp);
        return result;
    }

    @GetMapping("/ping")
    public String ping() {
        return "analyze controller alive";
    }
}

