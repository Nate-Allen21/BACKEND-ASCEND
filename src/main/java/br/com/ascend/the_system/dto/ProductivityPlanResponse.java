package br.com.ascend.the_system.dto;

import java.util.List;

public record ProductivityPlanResponse(
        String assistantName,
        String headline,
        String summary,
        String adaptationNote,
        List<String> insights,
        List<AdaptiveMissionResponse> dailyMissions,
        List<AdaptiveMissionResponse> weeklyMissions,
        List<AdaptiveMissionResponse> monthlyMissions,
        List<String> completedMissionIds
) {
}
