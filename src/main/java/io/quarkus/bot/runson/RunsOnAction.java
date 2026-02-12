package io.quarkus.bot.runson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.Inputs;
import io.quarkus.bot.runson.RunsOnConfiguration.RunnerConfiguration;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RunsOnAction {

    public static final String MAIN_REPOSITORY = "quarkusio/quarkus";

    public static final String RUNS_ON_CACHE_ACTION = "runs-on/cache";
    public static final String GITHUB_CACHE_ACTION = "actions/cache";

    public static final String RUNS_ON = "runs-on=%d/family=%s/image=%s/spot=%s%s";
    @Deprecated
    public static final String RUNS_ON_LEGACY = "runs-on=%d/%s/spot=%s%s";

    // Linux images
    public static final String IMAGE_UBUNTU_LATEST = "ubuntu-latest";

    // Windows images
    // for now Windows images are problematic as they don't contain Git Bash

    // Instances
    public static final String INSTANCE_SMALL = "small";
    public static final String INSTANCE_LARGE = "large";

    @Inject
    ObjectMapper objectMapper;

    @Action
    void action(Context context, Inputs inputs, Commands commands) throws JsonProcessingException {
        String mainRepository = inputs.getRequired(InputKeys.MAIN_REPOSITORY);
        boolean isMainRepository = mainRepository.equals(context.getGitHubRepository());
        boolean enabled = isMainRepository && inputs.getBoolean(InputKeys.RUNS_ON).orElse(false);
        //String ubuntuLatest = inputs.getRequired(InputKeys.UBUNTU_LATEST);
        String smallInstance = inputs.getRequired(InputKeys.SMALL_INSTANCE);
        String largeInstance = inputs.getRequired(InputKeys.LARGE_INSTANCE);
        boolean spot = inputs.getRequiredBoolean(InputKeys.SPOT);
        boolean magicCache = inputs.getRequiredBoolean(InputKeys.MAGIC_CACHE);
        Optional<String> ami = inputs.get(InputKeys.AMI);

        String branch = context.getGitHubBaseRef() == null ? context.getGitHubRefName() : context.getGitHubBaseRef();
        String ubuntuLatest = Images.getUbuntuLatest(branch);

        StringBuilder notice = new StringBuilder("Resolving Runs-On configuration with:\n\n");
        notice.append("Enabled: ").append(enabled).append("\n");
        notice.append("Main repository: ").append(isMainRepository)
                .append(isMainRepository ? "" : context.getGitHubRepository() + " detected as a fork of " + mainRepository).append("\n");
        notice.append("Images:\n");
        notice.append("- ubuntu-latest: ").append(ubuntuLatest).append("\n");
        notice.append("Use Spot instances: ").append(spot);

        commands.notice(notice.toString());

        String config = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(generateConfig(enabled, context, ubuntuLatest, smallInstance, largeInstance, spot, magicCache, ami));
        commands.setOutput(OutputKeys.CONFIG, config);

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

        commands.notice("Resolved configuration:\n\n" + jobSummary);
    }

    private static RunsOnConfiguration generateConfig(boolean enabled, Context context, String ubuntuLatest,
                                                      String smallInstance, String largeInstance, boolean spot, boolean magicCache, Optional<String> ami) {
        if (!enabled) {
            return RunsOnConfiguration.EMPTY;
        }

        StringBuilder additionalLabels = new StringBuilder();
        if (ami.isPresent()) {
            additionalLabels.append("/ami=").append(ami.get());
        }
        if (magicCache) {
            additionalLabels.append("/extras=s3-cache");
        }

        Map<String, RunnerConfiguration> runnerConfigurations;
        if (!ubuntuLatest.contains("/")) {
            // this is the standard behavior as of February 2026
            runnerConfigurations = new HashMap<>();
            runnerConfigurations.put(IMAGE_UBUNTU_LATEST + "-" + INSTANCE_SMALL, new RunnerConfiguration(
                    String.format(RUNS_ON, context.getGitHubRunId(), smallInstance, ubuntuLatest, spot, additionalLabels), magicCache ? GITHUB_CACHE_ACTION : RUNS_ON_CACHE_ACTION));
            runnerConfigurations.put(IMAGE_UBUNTU_LATEST + "-" + INSTANCE_LARGE, new RunnerConfiguration(
                    String.format(RUNS_ON, context.getGitHubRunId(), largeInstance, ubuntuLatest, spot, additionalLabels), magicCache ? GITHUB_CACHE_ACTION : RUNS_ON_CACHE_ACTION));
            // in case we still have an older config
            runnerConfigurations.put(IMAGE_UBUNTU_LATEST, new RunnerConfiguration(
                    String.format(RUNS_ON, context.getGitHubRunId(), smallInstance, ubuntuLatest, spot, additionalLabels), magicCache ? GITHUB_CACHE_ACTION : RUNS_ON_CACHE_ACTION));
        } else {
            runnerConfigurations = Map.of(IMAGE_UBUNTU_LATEST, new RunnerConfiguration(
                    String.format(RUNS_ON_LEGACY, context.getGitHubRunId(), ubuntuLatest, spot, additionalLabels), magicCache ? GITHUB_CACHE_ACTION : RUNS_ON_CACHE_ACTION));
        }

        return new RunsOnConfiguration(runnerConfigurations, 999);
    }
}