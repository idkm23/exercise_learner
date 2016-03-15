package com.example.chris.myapplication;

import java.util.concurrent.TimeUnit;

/**
 * Created by chris on 1/20/16.
 */
public class PlayerStats {

    private int promptCount;
    private long startTime;
    private long timeElapsed;
    private double accuracy;

    public PlayerStats() {
        startTime = System.nanoTime();
        timeElapsed = -1;
    }

    public long finish() {
        return timeElapsed = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
    }

    public void promptedPlayer() {
        promptCount++;
    }

    public int getPromptCount() {
        return promptCount;
    }

    public long getTimeElapsed() {
        return timeElapsed;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getAccuracy() {
        return accuracy;
    }
}
