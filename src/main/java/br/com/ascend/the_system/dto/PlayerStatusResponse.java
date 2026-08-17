package br.com.ascend.the_system.dto;

import java.util.List;

public record PlayerStatusResponse(
        String name,
        String handle,
        int level,
        String rank,
        int experience,
        int nextLevelExperience,
        int streakDays,
        int coins,
        List<StatResponse> stats,
        List<MissionResponse> dailyMissions,
        List<String> completedMissionIds
) {
}
