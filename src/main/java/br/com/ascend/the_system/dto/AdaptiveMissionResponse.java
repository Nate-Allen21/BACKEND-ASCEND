package br.com.ascend.the_system.dto;

public record AdaptiveMissionResponse(
        String id,
        String type,
        String title,
        String description,
        int experienceReward,
        int coinReward,
        boolean required,
        String cadence,
        String pillar,
        String difficulty,
        String rationale,
        boolean actionable
) {
}
