package com.dakonst.audio_cartographer.dto;

import java.util.List;

public class AnalysisResult {

    private String filename;
    private double durationSeconds;
    private double averageRms;
    private List<Double> energySegments;

    public List<Double> getEnergySegments() { return energySegments; }
    public void setEnergySegments(List<Double> energySegments) { this.energySegments = energySegments; }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public double getAverageRms() {
        return averageRms;
    }

    public void setAverageRms(double averageRms) {
        this.averageRms = averageRms;
    }
}
