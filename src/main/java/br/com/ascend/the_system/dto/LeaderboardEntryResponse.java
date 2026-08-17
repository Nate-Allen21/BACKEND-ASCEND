package br.com.ascend.the_system.dto;

public record LeaderboardEntryResponse(
        int position,
        String name,
        String handle,
        int level,
        String rank,
        int experience,
        boolean currentUser
) {
}
