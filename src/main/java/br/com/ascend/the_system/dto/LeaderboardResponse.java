package br.com.ascend.the_system.dto;

import java.util.List;

public record LeaderboardResponse(List<LeaderboardEntryResponse> players) {
}
