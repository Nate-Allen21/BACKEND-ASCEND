package br.com.ascend.the_system.dto;

public record UserResponse(
        Long id,
        String name,
        String email,
        int level,
        String rank,
        int experience,
        int nextLevelExperience,
        int streakDays,
        int coins
) {
}
