package io.quarkus.bot.runson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.Inputs;
import io.quarkus.bot.runson.RunsOnConfiguration.RunnerConfiguration;
import jakarta.inject.Inject;

import java.util.Map;

public class RunsOnAction {

    public static final String MAIN_REPOSITORY = "quarkusio/quarkus";

    public static final String RUNS_ON_CACHE_ACTION = "runs-on/cache@v4";

    public static final String RUNS_ON = "runs-on=%d/%s/spot=%s";

    // Linux images
    public static final String IMAGE_UBUNTU_LATEST = "ubuntu-latest";

    // Windows images
    // for now Windows images are problematic as they don't contain Git Bash

    @Inject
    ObjectMapper objectMapper;

    @Action
    void action(Context context, Inputs inputs, Commands commands) throws JsonProcessingException {
        commands.notice("Resolving Runs-On configuration");

        String config = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(generateConfig(context, inputs));
        commands.setOutput(OutputKeys.CONFIG, config);

        boolean enabled = inputs.getBoolean(InputKeys.RUNS_ON).orElse(false);
        StringBuilder jobSummary = new StringBuilder();
        jobSummary.append("## Runs-On configuration\n\n");
        jobSummary.append("Enabled: ").append(enabled).append("\n");
        if (enabled) {
            jobSummary.append("Configuration:\n\n");
            jobSummary.append("```json\n");
            jobSummary.append(config).append("\n");
            jobSummary.append("```\n");
        }
        commands.appendJobSummary(jobSummary.toString());
    }

    private static RunsOnConfiguration generateConfig(Context context, Inputs inputs) {
        if (inputs.getBoolean(InputKeys.RUNS_ON).orElse(false)
                || inputs.getRequired(InputKeys.MAIN_REPOSITORY).equals(context.getGitHubRepository())) {
            return RunsOnConfiguration.EMPTY;
        }

        return new RunsOnConfiguration(Map.of(IMAGE_UBUNTU_LATEST, new RunnerConfiguration(
                String.format(RUNS_ON, context.getGitHubRunId(), inputs.getRequired(InputKeys.UBUNTU_LATEST), inputs.getRequiredBoolean(InputKeys.SPOT)),
                RUNS_ON_CACHE_ACTION)),
                999);
    }
}