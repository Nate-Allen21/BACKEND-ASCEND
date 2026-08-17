package br.com.ascend.the_system.dto;

public record QuestCompletionResponse(
        Long userQuestId,
        String missionTitle,
        String pillar,
        String rank,
        int level,
        int experienceGranted,
        int coinsGranted,
        int totalExperience,
        int nextLevelExperience,
        int currentCoins,
        String primaryAttribute,
        int primaryAttributeValue
) {
}
