package br.com.ascend.the_system.dto;

import java.util.List;

public record RoutineProfileRequest(
        String routineSummary,
        String primaryGoals,
        String currentPriorities,
        String studyFocus,
        String workFocus,
        String healthFocus,
        String projectFocus,
        String habitFocus,
        String obstacles,
        int availableMinutesPerDay,
        String energyPattern,
        String motivationStyle,
        List<String> workoutDays,
        String bodyType,
        String trainingGoal,
        String trainingLevel
) {
}
