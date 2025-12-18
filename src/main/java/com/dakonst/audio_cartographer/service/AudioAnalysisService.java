package com.dakonst.audio_cartographer.service;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import com.dakonst.audio_cartographer.dto.AnalysisResult;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;

@Service
public class AudioAnalysisService {

    public AnalysisResult analyze(File audioFile) throws Exception {

        AnalysisResult result = new AnalysisResult();
        result.setFilename(audioFile.getName());

        // 1️⃣ Duration ( JavaSound)
        double duration = extractDuration(audioFile);
        result.setDurationSeconds(duration);

        // 2️⃣ RMS (TarsosDSP)
        double avgRms = extractAverageRms(audioFile);
        result.setAverageRms(avgRms);

        result.setEnergySegments(extractEnergySegments(audioFile, 10.0)); //  10sec

        return result;
    }

    private double extractDuration(File file) {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
            long frames = stream.getFrameLength();
            float frameRate = stream.getFormat().getFrameRate();
            if (frames > 0 && frameRate > 0) {
                return frames / frameRate;
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private double extractAverageRms(File file) throws Exception {

        int bufferSize = 2048;
        int overlap = 0;

        AudioDispatcher dispatcher =
                AudioDispatcherFactory.fromFile(file, bufferSize, overlap);

        RmsCollector rmsCollector = new RmsCollector();
        dispatcher.addAudioProcessor(rmsCollector);

        dispatcher.run();

        return rmsCollector.getAverageRms();
    }

    // сборщик RMS
    private static class RmsCollector implements AudioProcessor {

        private double sum = 0;
        private long count = 0;

        @Override
        public boolean process(AudioEvent audioEvent) {
            float[] buffer = audioEvent.getFloatBuffer();

            double rms = 0;
            for (float sample : buffer) {
                rms += sample * sample;
            }
            rms = Math.sqrt(rms / buffer.length);

            sum += rms;
            count++;

            return true;
        }

        @Override
        public void processingFinished() {
        }

        public double getAverageRms() {
            return count == 0 ? 0 : sum / count;
        }
    }

    private List<Double> extractEnergySegments(File file, double segmentSeconds) throws Exception {

        int bufferSize = 2048;
        int overlap = 0;

        AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(file, bufferSize, overlap);

        final float sampleRate = dispatcher.getFormat().getSampleRate();
        final int samplesPerSegment = (int) (segmentSeconds * sampleRate);

        List<Double> segments = new ArrayList<>();

        dispatcher.addAudioProcessor(new AudioProcessor() {

            int samplesInCurrent = 0;
            double sumRms = 0;
            int frames = 0;

            @Override
            public boolean process(AudioEvent audioEvent) {
                float[] buffer = audioEvent.getFloatBuffer();

                double rms = 0;
                for (float s : buffer) rms += s * s;
                rms = Math.sqrt(rms / buffer.length);

                sumRms += rms;
                frames++;

                samplesInCurrent += buffer.length;

                if (samplesInCurrent >= samplesPerSegment) {
                    segments.add(frames == 0 ? 0 : sumRms / frames);
                    samplesInCurrent = 0;
                    sumRms = 0;
                    frames = 0;
                }
                return true;
            }

            @Override
            public void processingFinished() {
                if (frames > 0 && samplesInCurrent >= samplesPerSegment * 0.3) {
                    segments.add(sumRms / frames);
                }
            }

        });

        dispatcher.run();
        return segments;
    }

}
