package com.dakonst.audio_cartographer.controller;

import com.dakonst.audio_cartographer.dto.AnalysisResult;
import com.dakonst.audio_cartographer.service.AudioAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/analyze")
public class AudioAnalysisController {

    private final AudioAnalysisService audioAnalysisService;

    public AudioAnalysisController(AudioAnalysisService audioAnalysisService) {
        this.audioAnalysisService = audioAnalysisService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalysisResult analyze(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is missing or empty");
        }

        Path temp = null;
        try {
            temp = Files.createTempFile("audio-", ".tmp");
            file.transferTo(temp);

            return audioAnalysisService.analyze(temp.toFile());

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded file", e);

        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // intentionally ignored: temporary file cleanup is best-effort
                }
            }
        }
    }

    @GetMapping("/ping")
    public String ping() {
        return "analyze controller alive";
    }
}
