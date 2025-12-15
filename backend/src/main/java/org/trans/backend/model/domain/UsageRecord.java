package org.trans.backend.model.domain;

import java.util.concurrent.atomic.AtomicInteger;

public class UsageRecord {
    private final AtomicInteger inputTokens = new AtomicInteger(0);
    private final AtomicInteger outputTokens = new AtomicInteger(0);
    private final AtomicInteger audioInputSeconds = new AtomicInteger(0);
    private final AtomicInteger audioOutputSeconds = new AtomicInteger(0);

    public UsageRecord() {
    }

    public UsageRecord(int inputTokens, int outputTokens, int audioInputSeconds, int audioOutputSeconds) {
        this.inputTokens.set(inputTokens);
        this.outputTokens.set(outputTokens);
        this.audioInputSeconds.set(audioInputSeconds);
        this.audioOutputSeconds.set(audioOutputSeconds);
    }

    public void addInputTokens(int tokens) {
        inputTokens.addAndGet(tokens);
    }

    public void addOutputTokens(int tokens) {
        outputTokens.addAndGet(tokens);
    }

    public void addAudioInputSeconds(int seconds) {
        audioInputSeconds.addAndGet(seconds);
    }

    public void addAudioOutputSeconds(int seconds) {
        audioOutputSeconds.addAndGet(seconds);
    }

    public int getInputTokens() {
        return inputTokens.get();
    }

    public int getOutputTokens() {
        return outputTokens.get();
    }

    public int getTotalTokens() {
        return inputTokens.get() + outputTokens.get();
    }

    public int getAudioInputSeconds() {
        return audioInputSeconds.get();
    }

    public int getAudioOutputSeconds() {
        return audioOutputSeconds.get();
    }

    public int getTotalAudioSeconds() {
        return audioInputSeconds.get() + audioOutputSeconds.get();
    }

    public UsageRecord copy() {
        return new UsageRecord(
                inputTokens.get(),
                outputTokens.get(),
                audioInputSeconds.get(),
                audioOutputSeconds.get()
        );
    }
}
