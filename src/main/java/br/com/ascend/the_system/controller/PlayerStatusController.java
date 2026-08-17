package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.dto.LeaderboardResponse;
import br.com.ascend.the_system.dto.PlayerStatusResponse;
import br.com.ascend.the_system.dto.ProductivityPlanResponse;
import br.com.ascend.the_system.dto.RoutineProfileRequest;
import br.com.ascend.the_system.service.AuthService;
import br.com.ascend.the_system.service.PlayerStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/player/status")
public class PlayerStatusController {

    private final PlayerStatusService playerStatusService;
    private final AuthService authService;

    public PlayerStatusController(PlayerStatusService playerStatusService, AuthService authService) {
        this.playerStatusService = playerStatusService;
        this.authService = authService;
    }

    @GetMapping
    public PlayerStatusResponse getStatus(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return playerStatusService.getCurrentStatus(authService.resolveUser(authorizationHeader));
    }

    @GetMapping("/ranking")
    public LeaderboardResponse getRanking(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        return playerStatusService.getLeaderboard(authService.resolveUser(authorizationHeader));
    }

    @PostMapping("/missions/{missionId}/complete")
    public PlayerStatusResponse completeMission(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String missionId
    ) {
        return playerStatusService.completeMission(authService.resolveUser(authorizationHeader), missionId);
    }

    @PostMapping("/planner")
    public ProductivityPlanResponse generatePlan(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody(required = false) RoutineProfileRequest profile
    ) {
        return playerStatusService.generateAdaptivePlan(authService.resolveUser(authorizationHeader), profile);
    }

    @PostMapping("/planner/missions/{missionId}/complete")
    public PlayerStatusResponse completeAdaptiveMission(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable String missionId,
            @RequestBody(required = false) RoutineProfileRequest profile
    ) {
        return playerStatusService.completeAdaptiveMission(authService.resolveUser(authorizationHeader), missionId, profile);
    }
}
