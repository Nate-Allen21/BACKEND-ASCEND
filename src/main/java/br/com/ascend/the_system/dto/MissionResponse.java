package br.com.ascend.the_system.dto;

public record MissionResponse(
        String id,
        String type,
        String title,
        String description,
        int experienceReward,
        int coinReward,
        boolean required
) {
}
