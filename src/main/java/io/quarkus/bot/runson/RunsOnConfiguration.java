package io.quarkus.bot.runson;

import java.util.Map;

public record RunsOnConfiguration(Map<String, RunnerConfiguration> runners, int maxParallel) {

    public static final RunsOnConfiguration EMPTY = new RunsOnConfiguration(Map.of(), 0);

    public record RunnerConfiguration(String runsOn, String cacheAction) {
    }
}
