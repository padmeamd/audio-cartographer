package com.dakonst.audio_cartographer.service;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import com.dakonst.audio_cartographer.dto.AnalysisResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AudioAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AudioAnalysisService.class);
    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private static final int DEFAULT_OVERLAP = 0;
    private static final double DEFAULT_SEGMENT_SECONDS = 10.0;

    public AnalysisResult analyze(File audioFile) {
        File file = requireExistingFile(audioFile);

        double durationSeconds = readDurationSeconds(file);         // JavaSound
        double averageRms = computeAverageRms(file);                // TarsosDSP
        List<Double> energySegments = computeEnergySegments(file, DEFAULT_SEGMENT_SECONDS);
        AnalysisResult result = new AnalysisResult();
        result.setFilename(file.getName());
        result.setDurationSeconds(durationSeconds);
        result.setAverageRms(averageRms);
        result.setEnergySegments(energySegments);
        return result;
    }

    private File requireExistingFile(File file) {
        Objects.requireNonNull(file, "audioFile must not be null");
        if (!file.exists() || !file.isFile()) {
            throw new InvalidAudioFileException("Audio file does not exist: " + file.getAbsolutePath());
        }
        return file;
    }

    private double readDurationSeconds(File file) {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
            long frames = stream.getFrameLength();
            float frameRate = stream.getFormat().getFrameRate();

            if (frames <= 0 || frameRate <= 0) {
                log.warn("Unable to read duration (frames={}, frameRate={}) for {}", frames, frameRate, file.getName());
                return 0.0;
            }

            return frames / frameRate;

        } catch (Exception e) {
            // Это решение: длительность — “best effort”, но мы не молчим.
            log.warn("Failed to read duration for {}: {}", file.getName(), e.getMessage());
            return 0.0;
        }
    }

    private double computeAverageRms(File file) {
        AudioDispatcher dispatcher = dispatcherFrom(file);

        RmsCollector rmsCollector = new RmsCollector();
        dispatcher.addAudioProcessor(rmsCollector);

        runDispatcher(dispatcher, file);
        return rmsCollector.averageRms();
    }

    private List<Double> computeEnergySegments(File file, double segmentSeconds) {
        if (segmentSeconds <= 0) {
            throw new IllegalArgumentException("segmentSeconds must be > 0");
        }

        AudioDispatcher dispatcher = dispatcherFrom(file);

        EnergySegmentCollector collector = new EnergySegmentCollector(
                dispatcher.getFormat().getSampleRate(),
                segmentSeconds
        );
        dispatcher.addAudioProcessor(collector);

        runDispatcher(dispatcher, file);
        return collector.segments();
    }

    private AudioDispatcher dispatcherFrom(File file) {
        try {
            return AudioDispatcherFactory.fromFile(file, DEFAULT_BUFFER_SIZE, DEFAULT_OVERLAP);
        } catch (Exception e) {
            throw new AudioAnalysisException("Failed to create audio dispatcher for: " + file.getName(), e);
        }
    }

    private void runDispatcher(AudioDispatcher dispatcher, File file) {
        try {
            dispatcher.run();
        } catch (Exception e) {
            throw new AudioAnalysisException("Audio processing failed for: " + file.getName(), e);
        }
    }

    /**
     * Internal AudioProcessor (used only by AudioAnalysisService)
     *
     * A private nested class:
     * - this is an implementation detail, not a domain component
     * - currently not reused outside of RMS aggregation
     * - extracted into a named class for readability, not exposed at package level
     */
    private static final class RmsCollector implements AudioProcessor {
        private double sum;
        private long frames;

        @Override
        public boolean process(AudioEvent event) {
            sum += rms(event.getFloatBuffer());
            frames++;
            return true;
        }

        @Override
        public void processingFinished() {
            // no-op intentionally
        }

        double averageRms() {
            return frames == 0 ? 0.0 : sum / frames;
        }
    }

    /**
     * Internal AudioProcessor that aggregates RMS values into fixed-time energy segments
     *
     *  A private nested class because:
     * 1) segmentation logic is currently tightly coupled to AudioAnalysisService use cases
     * 2) segment duration and tail-handling rules are not part of a stable public API yet
     * 3) this processor represents an analysis strategy, not a reusable domain component
     */
    private static final class EnergySegmentCollector implements AudioProcessor {
        private final int samplesPerSegment;
        private final List<Double> segments = new ArrayList<>();

        private int samplesInSegment;
        private double sumRms;
        private int frames;

        EnergySegmentCollector(float sampleRate, double segmentSeconds) {
            this.samplesPerSegment = (int) Math.round(sampleRate * segmentSeconds);
        }

        @Override
        public boolean process(AudioEvent event) {
            float[] buffer = event.getFloatBuffer();
            sumRms += rms(buffer);
            frames++;
            samplesInSegment += buffer.length;

            if (samplesInSegment >= samplesPerSegment) {
                segments.add(frames == 0 ? 0.0 : sumRms / frames);
                resetSegment();
            }
            return true;
        }

        @Override
        public void processingFinished() {
            // “почерк”: явное правило “добиваем хвост, если он достаточно большой”
            if (frames > 0 && samplesInSegment >= samplesPerSegment * 0.3) {
                segments.add(sumRms / frames);
            }
        }

        List<Double> segments() {
            return List.copyOf(segments);
        }

        private void resetSegment() {
            samplesInSegment = 0;
            sumRms = 0.0;
            frames = 0;
        }
    }

    private static double rms(float[] buffer) {
        double sumSquares = 0.0;
        for (float sample : buffer) {
            sumSquares += (double) sample * sample;
        }
        return buffer.length == 0 ? 0.0 : Math.sqrt(sumSquares / buffer.length);
    }

    public static class AudioAnalysisException extends RuntimeException {
        public AudioAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InvalidAudioFileException extends RuntimeException {
        public InvalidAudioFileException(String message) {
            super(message);
        }
    }
}
