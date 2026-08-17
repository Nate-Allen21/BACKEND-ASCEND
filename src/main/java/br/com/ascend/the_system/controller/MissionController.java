package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.dto.QuestCompletionResponse;
import br.com.ascend.the_system.service.AuthService;
import br.com.ascend.the_system.service.MissionEngineService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/missions")
public class MissionController {

    private final MissionEngineService missionEngineService;
    private final AuthService authService;

    public MissionController(MissionEngineService missionEngineService, AuthService authService) {
        this.missionEngineService = missionEngineService;
        this.authService = authService;
    }

    @PostMapping("/user-quests/{userQuestId}/complete")
    public QuestCompletionResponse completeMission(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long userQuestId
    ) {
        return missionEngineService.completeQuest(authService.resolveUser(authorizationHeader), userQuestId);
    }
}
