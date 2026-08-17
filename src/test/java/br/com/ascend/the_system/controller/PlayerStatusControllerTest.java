package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.domain.User;
import br.com.ascend.the_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.JsonPathExpectationsHelper;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PlayerStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturnPlayerStatus() throws Exception {
        String email = "status-" + System.nanoTime() + "@ascend.local";
        String token = registerUserAndExtractToken("Kael Hunter", email);

        mockMvc.perform(get("/api/player/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value("E"))
                .andExpect(jsonPath("$.name").value("Kael Hunter"))
                .andExpect(jsonPath("$.stats[?(@.key == 'strength')].value").value(hasItem(0)))
                .andExpect(jsonPath("$.stats[?(@.key == 'agility')].value").value(hasItem(0)))
                .andExpect(jsonPath("$.stats[?(@.key == 'vitality')].value").value(hasItem(0)))
                .andExpect(jsonPath("$.stats[?(@.key == 'intelligence')].value").value(hasItem(0)))
                .andExpect(jsonPath("$.stats[?(@.key == 'spirit')].value").value(hasItem(0)))
                .andExpect(jsonPath("$.stats[?(@.key == 'missions')].value").value(hasItem(0)));
    }

    @Test
    void shouldCompleteMissionAndUpdateExperience() throws Exception {
        String email = "mission-" + System.nanoTime() + "@ascend.local";
        String token = registerUserAndExtractToken("Kael Hunter", email);
        String statusResponse = mockMvc.perform(get("/api/player/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String missionId = new JsonPathExpectationsHelper("$.dailyMissions[0].id")
                .evaluateJsonPath(statusResponse, String.class);

        mockMvc.perform(post("/api/player/status/missions/{missionId}/complete", missionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experience").value(greaterThan(0)))
                .andExpect(jsonPath("$.streakDays").value(1))
                .andExpect(jsonPath("$.completedMissionIds").value(hasItem(missionId)))
                .andExpect(jsonPath("$.stats[?(@.key == 'strength')].value").value(hasItem(greaterThan(0))))
                .andExpect(jsonPath("$.stats[?(@.key == 'missions')].value").value(hasItem(1)));
    }

    @Test
    void shouldReturnRankingComparingRegisteredUsers() throws Exception {
        String firstEmail = "ranking-a-" + System.nanoTime() + "@ascend.local";
        registerUserAndExtractToken("Primeiro", firstEmail);

        String secondEmail = "ranking-b-" + System.nanoTime() + "@ascend.local";
        String secondToken = registerUserAndExtractToken("Segundo", secondEmail);

        User firstUser = userRepository.findByEmail(firstEmail).orElseThrow();
        firstUser.addRewards(1_000_000, 0);
        userRepository.save(firstUser);

        mockMvc.perform(get("/api/player/status/ranking")
                        .header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players[*].name").value(hasItem("Primeiro")))
                .andExpect(jsonPath("$.players[*].name").value(hasItem("Segundo")))
                .andExpect(jsonPath("$.players[?(@.name == 'Primeiro')].position").value(hasItem(1)))
                .andExpect(jsonPath("$.players[?(@.name == 'Segundo')].currentUser").value(hasItem(true)));
    }

    @Test
    void shouldHideLegendaryMissionBeforeLevel30AndShowAfter() throws Exception {
        String email = "legendary-" + System.nanoTime() + "@ascend.local";
        String token = registerUserAndExtractToken("Kael Hunter", email);

        mockMvc.perform(get("/api/player/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyMissions[?(@.type == 'lendaria')]").doesNotExist());

        User user = userRepository.findByEmail(email).orElseThrow();
        user.addRewards(300_000, 0);
        userRepository.save(user);

        mockMvc.perform(get("/api/player/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value("S2"))
                .andExpect(jsonPath("$.dailyMissions[?(@.type == 'lendaria')]").isArray())
                .andExpect(jsonPath("$.dailyMissions[?(@.type == 'lendaria')]").value(hasSize(1)));
    }

    @Test
    void shouldReturnSmarterAdaptivePlanBasedOnEnergyAndTrainingProfile() throws Exception {
        String email = "planner-" + System.nanoTime() + "@ascend.local";
        String token = registerUserAndExtractToken("Kael Hunter", email);

        mockMvc.perform(post("/api/player/status/planner")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "routineSummary": "Minha rotina esta pesada e eu fico cansado no fim do dia.",
                                  "primaryGoals": "Voltar a treinar e recuperar consistencia.",
                                  "currentPriorities": "Treino, sono e energia.",
                                  "studyFocus": "",
                                  "workFocus": "",
                                  "healthFocus": "Treino de hipertrofia com foco em energia",
                                  "projectFocus": "",
                                  "habitFocus": "Dormir melhor e manter o ritmo",
                                  "obstacles": "Cansaco e procrastinacao",
                                  "availableMinutesPerDay": 70,
                                  "energyPattern": "Energia baixa no fim do dia",
                                  "motivationStyle": "Desafios curtos e progresso visivel",
                                  "workoutDays": ["segunda", "terca", "quinta", "sabado"],
                                  "bodyType": "mesomorfo",
                                  "trainingGoal": "hipertrofia",
                                  "trainingLevel": "intermediario"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adaptationNote").value(containsString("desgaste")))
                .andExpect(jsonPath("$.dailyMissions[0].type").value("saude"))
                .andExpect(jsonPath("$.insights").value(hasItem(containsString("objetivo hipertrofia"))))
                .andExpect(jsonPath("$.insights").value(hasItem(containsString("energia baixa"))));
    }

    private String registerUserAndExtractToken(String name, String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "email": "%s",
                                  "password": "senha1234"
                                }
                                """.formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new JsonPathExpectationsHelper("$.token")
                .evaluateJsonPath(response, String.class);
    }

}
