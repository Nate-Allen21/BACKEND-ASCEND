package br.com.ascend.the_system.service;

import br.com.ascend.the_system.domain.AttributeType;
import br.com.ascend.the_system.domain.MissionCompletion;
import br.com.ascend.the_system.domain.User;
import br.com.ascend.the_system.domain.UserAttribute;
import br.com.ascend.the_system.dto.AdaptiveMissionResponse;
import br.com.ascend.the_system.dto.LeaderboardEntryResponse;
import br.com.ascend.the_system.dto.LeaderboardResponse;
import br.com.ascend.the_system.dto.MissionResponse;
import br.com.ascend.the_system.dto.PlayerStatusResponse;
import br.com.ascend.the_system.dto.ProductivityPlanResponse;
import br.com.ascend.the_system.dto.RoutineProfileRequest;
import br.com.ascend.the_system.dto.StatResponse;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.repository.MissionCompletionRepository;
import br.com.ascend.the_system.repository.UserAttributeRepository;
import br.com.ascend.the_system.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlayerStatusService {

    private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int PENALTY_EXP_LOSS = 50;
    private static final int PENALTY_COIN_FEE = 200;
    private static final String ASSISTANT_NAME = "ASCEND AI";
    private static final Comparator<User> LEADERBOARD_COMPARATOR = Comparator
            .comparingInt(User::getLevel).reversed()
            .thenComparing(Comparator.comparingInt(User::getExperience).reversed())
            .thenComparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(User::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final UserRepository userRepository;
    private final MissionCompletionRepository missionCompletionRepository;
    private final UserAttributeRepository userAttributeRepository;

    public PlayerStatusService(
            UserRepository userRepository,
            MissionCompletionRepository missionCompletionRepository,
            UserAttributeRepository userAttributeRepository
    ) {
        this.userRepository = userRepository;
        this.missionCompletionRepository = missionCompletionRepository;
        this.userAttributeRepository = userAttributeRepository;
    }

    public PlayerStatusResponse getCurrentStatus(User user) {
        LocalDate today = LocalDate.now(APP_ZONE);
        user = processPendingPenaltyMissions(user, today);
        List<MissionResponse> dailyMissions = buildDailyMissions(today, user);
        List<String> completedMissionIds = missionCompletionRepository.findCompletedMissionIds(
                user.getId(),
                dailyMissions.stream().map(MissionResponse::id).toList()
        );

        return new PlayerStatusResponse(
                user.getName(),
                "@" + user.getEmail().split("@")[0],
                user.getLevel(),
                user.getRank(),
                user.getExperience(),
                user.getNextLevelExperience(),
                user.getDisplayedStreakDays(today),
                user.getCoins(),
                buildStats(user),
                dailyMissions,
                completedMissionIds
        );
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(User currentUser) {
        List<User> sortedUsers = userRepository.findAll().stream()
                .sorted(LEADERBOARD_COMPARATOR)
                .toList();

        List<LeaderboardEntryResponse> rankedPlayers = java.util.stream.IntStream.range(0, sortedUsers.size())
                .mapToObj(index -> {
                    User user = sortedUsers.get(index);

                    return new LeaderboardEntryResponse(
                            index + 1,
                            user.getName(),
                            buildHandle(user),
                            user.getLevel(),
                            user.getRank(),
                            user.getExperience(),
                            user.getId().equals(currentUser.getId())
                    );
                })
                .toList();

        return new LeaderboardResponse(rankedPlayers);
    }

    @Transactional
    public PlayerStatusResponse completeMission(User user, String missionId) {
        LocalDate today = LocalDate.now(APP_ZONE);
        user = processPendingPenaltyMissions(user, today);
        MissionResponse mission = buildDailyMissions(today, user).stream()
                .filter(dailyMission -> dailyMission.id().equals(missionId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Missao invalida para o dia atual."));

        if (!missionCompletionRepository.existsByUser_IdAndMissionId(user.getId(), missionId)) {
            user.addRewards(mission.experienceReward(), mission.coinReward());
            user.registerRoutineActivity(today);
            User savedUser = userRepository.save(user);
            increaseStatsForMission(savedUser, mission);
            missionCompletionRepository.save(new MissionCompletion(savedUser, missionId));
            return getCurrentStatus(savedUser);
        }

        return getCurrentStatus(user);
    }

    public ProductivityPlanResponse generateAdaptivePlan(User user, RoutineProfileRequest profile) {
        LocalDate today = LocalDate.now(APP_ZONE);
        user = processPendingPenaltyMissions(user, today);
        PlannerProfile plannerProfile = PlannerProfile.from(profile);
        AdaptivePlan plan = buildAdaptivePlan(today, user, plannerProfile);
        List<String> completedMissionIds = missionCompletionRepository.findCompletedMissionIds(
                user.getId(),
                plan.dailyMissions().stream().map(AdaptiveMissionResponse::id).toList()
        );

        return new ProductivityPlanResponse(
                ASSISTANT_NAME,
                plan.headline(),
                plan.summary(),
                plan.adaptationNote(),
                plan.insights(),
                plan.dailyMissions(),
                plan.weeklyMissions(),
                plan.monthlyMissions(),
                completedMissionIds
        );
    }

    public String describeProfile(RoutineProfileRequest profile) {
        if (profile == null) {
            return "perfil nao informado";
        }

        PlannerProfile plannerProfile = PlannerProfile.from(profile);
        return plannerProfile.primaryGoals() + " | prioridades: " + plannerProfile.currentPriorities()
                + " | rotina: " + plannerProfile.routineSummary()
                + " | estudos: " + plannerProfile.studyFocus()
                + " | trabalho: " + plannerProfile.workFocus()
                + " | saude: " + plannerProfile.healthFocus()
                + " | projetos: " + plannerProfile.projectFocus()
                + " | habito-chave: " + plannerProfile.habitFocus()
                + " | energia: " + plannerProfile.energyPattern()
                + " | motivacao: " + plannerProfile.motivationStyle()
                + " | treino: " + plannerProfile.trainingSummary();
    }

    @Transactional
    public PlayerStatusResponse completeAdaptiveMission(User user, String missionId, RoutineProfileRequest profile) {
        LocalDate today = LocalDate.now(APP_ZONE);
        user = processPendingPenaltyMissions(user, today);
        PlannerProfile plannerProfile = PlannerProfile.from(profile);
        AdaptiveMissionResponse mission = buildAdaptivePlan(today, user, plannerProfile).dailyMissions().stream()
                .filter(dailyMission -> dailyMission.id().equals(missionId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Missao adaptativa invalida para o dia atual."));

        if (!mission.actionable()) {
            throw new BadRequestException("Somente missoes diarias podem ser concluidas neste painel.");
        }

        if (!missionCompletionRepository.existsByUser_IdAndMissionId(user.getId(), missionId)) {
            user.addRewards(mission.experienceReward(), mission.coinReward());
            user.registerRoutineActivity(today);
            User savedUser = userRepository.save(user);
            increaseStatsForAdaptiveMission(savedUser, mission);
            missionCompletionRepository.save(new MissionCompletion(savedUser, missionId));
            return getCurrentStatus(savedUser);
        }

        return getCurrentStatus(user);
    }

    private List<StatResponse> buildStats(User user) {
        Map<AttributeType, Integer> attributes = new EnumMap<>(AttributeType.class);
        userAttributeRepository.findAllByUser_Id(user.getId()).forEach(attribute ->
                attributes.put(attribute.getAttributeType(), attribute.getTotalValue())
        );
        int totalMissions = (int) missionCompletionRepository.countByUser_Id(user.getId());

        return List.of(
                new StatResponse("strength", "Forca", attributes.getOrDefault(AttributeType.STRENGTH, 0)),
                new StatResponse("agility", "Agilidade", attributes.getOrDefault(AttributeType.AGILITY, 0)),
                new StatResponse("vitality", "Vitalidade", attributes.getOrDefault(AttributeType.VITALITY, 0)),
                new StatResponse("intelligence", "Inteligencia", attributes.getOrDefault(AttributeType.INTELLIGENCE, 0)),
                new StatResponse("spirit", "Espirito", attributes.getOrDefault(AttributeType.SPIRIT, 0)),
                new StatResponse("missions", "Missoes", totalMissions)
        );
    }

    private void increaseStatsForMission(User user, MissionResponse mission) {
        switch (mission.type()) {
            case "treino" -> {
                increaseAttribute(user, AttributeType.STRENGTH, 1);
                increaseAttribute(user, AttributeType.VITALITY, 1);
            }
            case "estudo" -> increaseAttribute(user, AttributeType.INTELLIGENCE, 1);
            case "financeiro" -> increaseAttribute(user, AttributeType.SPIRIT, 1);
            case "lendaria" -> {
                increaseAttribute(user, AttributeType.STRENGTH, 2);
                increaseAttribute(user, AttributeType.AGILITY, 2);
                increaseAttribute(user, AttributeType.VITALITY, 2);
                increaseAttribute(user, AttributeType.INTELLIGENCE, 2);
                increaseAttribute(user, AttributeType.SPIRIT, 2);
            }
            case "penalidade" -> increaseAttribute(user, AttributeType.AGILITY, 1);
            default -> {
            }
        }
    }

    private void increaseAttribute(User user, AttributeType attributeType, int points) {
        UserAttribute userAttribute = userAttributeRepository.findByUser_IdAndAttributeType(user.getId(), attributeType)
                .orElseGet(() -> userAttributeRepository.save(new UserAttribute(user, attributeType, 0)));
        userAttribute.increase(points);
        userAttributeRepository.save(userAttribute);
    }

    private void increaseStatsForAdaptiveMission(User user, AdaptiveMissionResponse mission) {
        switch (mission.type()) {
            case "estudo" -> increaseAttribute(user, AttributeType.INTELLIGENCE, mission.required() ? 2 : 1);
            case "trabalho" -> {
                increaseAttribute(user, AttributeType.INTELLIGENCE, 1);
                increaseAttribute(user, AttributeType.AGILITY, 1);
            }
            case "saude" -> {
                increaseAttribute(user, AttributeType.VITALITY, mission.required() ? 2 : 1);
                increaseAttribute(user, AttributeType.STRENGTH, 1);
            }
            case "projeto" -> {
                increaseAttribute(user, AttributeType.INTELLIGENCE, 1);
                increaseAttribute(user, AttributeType.SPIRIT, 1);
            }
            case "habito" -> increaseAttribute(user, AttributeType.SPIRIT, 1);
            case "foco" -> increaseAttribute(user, AttributeType.AGILITY, 1);
            case "penalidade" -> increaseAttribute(user, AttributeType.AGILITY, 1);
            default -> increaseAttribute(user, AttributeType.SPIRIT, 1);
        }
    }

    private User processPendingPenaltyMissions(User user, LocalDate today) {
        LocalDate createdDate = user.getCreatedAt() == null
                ? today
                : user.getCreatedAt().atZoneSameInstant(APP_ZONE).toLocalDate();
        LocalDate startDate = user.getLastPenaltyProcessedDate() == null
                ? createdDate
                : user.getLastPenaltyProcessedDate().plusDays(1);
        LocalDate endDate = today.minusDays(1);

        if (startDate.isAfter(endDate)) {
            return user;
        }

        boolean userChanged = false;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            MissionResponse penaltyMission = withDate(selectSingleMission(buildPenaltyMissions(), date, 53), date.toString());
            boolean completed = missionCompletionRepository.existsByUser_IdAndMissionId(user.getId(), penaltyMission.id());

            if (!completed) {
                user.applyPenalty(PENALTY_EXP_LOSS, PENALTY_COIN_FEE);
                userChanged = true;
            }

            user.markPenaltyProcessed(date);
            userChanged = true;
        }

        return userChanged ? userRepository.save(user) : user;
    }

    private List<MissionResponse> buildDailyMissions(LocalDate date, User user) {
        String dateKey = date.toString();
        List<MissionResponse> trainingMissions = List.of(
                new MissionResponse("treino-peito-triceps", "treino", "Treino - Peito e Triceps", "Obrigatoria - +1 Forca +1 Vitalidade", 75, 15, true),
                new MissionResponse("treino-costas-biceps", "treino", "Treino - Costas e Biceps", "Obrigatoria - +1 Forca", 75, 15, true),
                new MissionResponse("treino-pernas-core", "treino", "Treino - Pernas e Core", "Obrigatoria - +1 Vitalidade", 85, 18, true),
                new MissionResponse("treino-cardio", "treino", "Cardio - 30 minutos", "Obrigatoria - +1 Agilidade", 60, 12, true),
                new MissionResponse("treino-ombros", "treino", "Treino - Ombros e Abdomen", "Obrigatoria - +1 Forca +1 Agilidade", 80, 16, true),
                new MissionResponse("treino-mobilidade", "treino", "Mobilidade e alongamento - 25 minutos", "Obrigatoria - +1 Vitalidade", 55, 10, true),
                new MissionResponse("treino-hiit", "treino", "HIIT - 20 minutos intensos", "Obrigatoria - +1 Agilidade +1 Vitalidade", 70, 14, true),
                new MissionResponse("treino-caminhada", "treino", "Caminhada acelerada - 45 minutos", "Obrigatoria - +1 Vitalidade", 58, 11, true)
        );

        List<MissionResponse> studyMissions = List.of(
                new MissionResponse("estudo-algebra", "estudo", "Estudar Algebra Linear - 2h", "Diaria - +1 Inteligencia", 50, 10, false),
                new MissionResponse("estudo-programacao", "estudo", "Revisar programacao - 90 min", "Diaria - +1 Inteligencia", 55, 10, false),
                new MissionResponse("estudo-exercicios", "estudo", "Resolver exercicios pendentes", "Diaria - +1 Inteligencia", 45, 8, false),
                new MissionResponse("estudo-leitura", "estudo", "Ler 20 paginas", "Diaria - +1 Espirito", 40, 8, false),
                new MissionResponse("estudo-resumo", "estudo", "Criar um resumo de 1 pagina", "Diaria - +1 Inteligencia", 42, 8, false),
                new MissionResponse("estudo-videoaula", "estudo", "Assistir uma videoaula e anotar insights", "Diaria - +1 Inteligencia", 48, 9, false),
                new MissionResponse("estudo-revisao", "estudo", "Revisao ativa com flashcards - 30 min", "Diaria - +1 Inteligencia", 44, 8, false),
                new MissionResponse("estudo-projeto", "estudo", "Avancar uma feature do projeto pessoal", "Diaria - +1 Inteligencia", 60, 12, false)
        );

        List<MissionResponse> financeMissions = List.of(
                new MissionResponse("financeiro-registro", "financeiro", "Registrar gastos do dia no Vault", "Diaria - +1 Espirito", 30, 5, false),
                new MissionResponse("financeiro-saldo", "financeiro", "Conferir saldo e metas", "Diaria - +1 Espirito", 30, 5, false),
                new MissionResponse("financeiro-reserva", "financeiro", "Separar uma pequena reserva", "Diaria - +1 Espirito", 35, 6, false),
                new MissionResponse("financeiro-compras", "financeiro", "Revisar compras planejadas", "Diaria - +1 Espirito", 30, 5, false),
                new MissionResponse("financeiro-corte", "financeiro", "Identificar um gasto que pode ser cortado", "Diaria - +1 Espirito", 32, 6, false),
                new MissionResponse("financeiro-orcamento", "financeiro", "Atualizar o orcamento semanal", "Diaria - +1 Espirito", 38, 7, false),
                new MissionResponse("financeiro-fatura", "financeiro", "Revisar fatura ou assinaturas ativas", "Diaria - +1 Espirito", 34, 6, false),
                new MissionResponse("financeiro-meta", "financeiro", "Definir a meta financeira de amanha", "Diaria - +1 Espirito", 28, 5, false)
        );

        List<MissionResponse> legendaryMissions = List.of(
                new MissionResponse("lendaria-boss", "lendaria", "Missao Lendaria: Boss do Dia", "Especial - complete o desafio principal e receba recompensa massiva", 1_500, 350, true),
                new MissionResponse("lendaria-combo", "lendaria", "Missao Lendaria: Combo Perfeito", "Especial - conclua treino, estudo e organizacao no mesmo ciclo", 1_750, 420, true),
                new MissionResponse("lendaria-disciplina", "lendaria", "Missao Lendaria: Disciplina Absoluta", "Especial - execute sua prioridade mais dificil sem adiar", 1_600, 380, true),
                new MissionResponse("lendaria-focus", "lendaria", "Missao Lendaria: Foco Monstruoso", "Especial - feche um bloco longo de concentracao profunda", 1_800, 450, true)
        );

        List<MissionResponse> dailyMissions = new ArrayList<>();
        dailyMissions.addAll(selectDailyMissions(trainingMissions, date, 2, 11));
        dailyMissions.addAll(selectDailyMissions(studyMissions, date, 2, 23));
        dailyMissions.addAll(selectDailyMissions(financeMissions, date, 2, 37));
        if (user.getLevel() >= 30) {
            dailyMissions.add(withDate(selectSingleMission(legendaryMissions, date, 41), dateKey));
        }
        dailyMissions.add(withDate(selectSingleMission(buildPenaltyMissions(), date, 53), dateKey));
        return dailyMissions;
    }

    private List<MissionResponse> buildPenaltyMissions() {
        return List.of(
                new MissionResponse("penalidade-recuperacao", "penalidade", "Penalty Quest: rotina incompleta", "Obrigatoria - conclua para evitar -50 EXP e taxa de 200 moedas", 25, 0, true),
                new MissionResponse("penalidade-reinicio", "penalidade", "Penalty Quest: recompor disciplina", "Obrigatoria - execute a tarefa para evitar -50 EXP e taxa de 200 moedas", 30, 0, true),
                new MissionResponse("penalidade-ajuste", "penalidade", "Penalty Quest: corrigir falha do ciclo", "Obrigatoria - feche a pendencia hoje para evitar -50 EXP e taxa de 200 moedas", 28, 0, true)
        );
    }

    private List<MissionResponse> selectDailyMissions(List<MissionResponse> pool, LocalDate date, int count, int salt) {
        return pool.stream()
                .sorted(Comparator.comparingInt(mission -> rotationWeight(mission.id(), date, salt)))
                .limit(count)
                .map(mission -> withDate(mission, date.toString()))
                .toList();
    }

    private MissionResponse selectSingleMission(List<MissionResponse> pool, LocalDate date, int salt) {
        int index = Math.floorMod((int) date.toEpochDay() + salt, pool.size());
        return pool.get(index);
    }

    private int rotationWeight(String key, LocalDate date, int salt) {
        long seed = (date.toEpochDay() * 31L) + salt;
        return Math.floorMod((int) (seed + key.hashCode() * 17L), 10_000);
    }

    private MissionResponse withDate(MissionResponse mission, String dateKey) {
        return new MissionResponse(
                mission.id() + "-" + dateKey,
                mission.type(),
                mission.title(),
                mission.description(),
                mission.experienceReward(),
                mission.coinReward(),
                mission.required()
        );
    }

    private String buildHandle(User user) {
        return "@" + user.getEmail().split("@")[0];
    }

    private AdaptivePlan buildAdaptivePlan(LocalDate date, User user, PlannerProfile profile) {
        int dailyMinutes = Math.max(30, profile.availableMinutesPerDay());
        int streak = user.getDisplayedStreakDays(date);
        List<String> activeAreas = profile.activeAreas();
        List<AdaptiveMissionResponse> dailyMissions = new ArrayList<>();
        int dailyMissionTarget = Math.min(activeAreas.size(), profile.dailyMissionTarget(streak));

        String topArea = activeAreas.get(0);
        dailyMissions.add(createAdaptiveMission(topArea, "daily", date, 11, true, dailyMinutes, streak, profile));

        for (int index = 1; index < dailyMissionTarget; index++) {
            dailyMissions.add(createAdaptiveMission(activeAreas.get(index), "daily", date, 11 + (index * 7), false, dailyMinutes, streak, profile));
        }

        dailyMissions.add(createFocusMission(date, dailyMinutes, profile, streak));

        if (profile.requiresRecoveryMission(streak)) {
            dailyMissions.add(createPenaltyRecoveryMission(date, profile));
        }

        List<AdaptiveMissionResponse> weeklyMissions = new ArrayList<>();
        for (int index = 0; index < Math.min(3, activeAreas.size()); index++) {
            weeklyMissions.add(createAdaptiveMission(activeAreas.get(index), "weekly", date, 41 + (index * 5), index == 0, dailyMinutes, streak, profile));
        }

        List<AdaptiveMissionResponse> monthlyMissions = new ArrayList<>();
        for (int index = 0; index < Math.min(3, activeAreas.size()); index++) {
            monthlyMissions.add(createAdaptiveMission(activeAreas.get(index), "monthly", date, 71 + (index * 5), index == 0, dailyMinutes, streak, profile));
        }

        String primaryGoal = profile.primaryGoalLabel();
        String priorityLabel = profile.priorityLabel();
        String summary = "Plano montado para transformar " + primaryGoal + " em execucao consistente, respeitando "
                + profile.availableMinutesPerDay() + " min livres por dia, ritmo " + profile.executionRhythmLabel(streak)
                + " e priorizando " + priorityLabel + ".";
        String adaptation = buildAdaptationNote(profile, streak, dailyMissionTarget);

        List<String> insights = new ArrayList<>();
        insights.add("Prioridade central: " + priorityLabel + ".");
        insights.add("Ritmo tatico: " + profile.executionRhythmLabel(streak) + " com energia " + profile.energyPatternLabel() + ".");
        insights.add("Motivador dominante: " + profile.motivationProfileLabel() + ".");
        insights.add("Treino lido pela IA: " + profile.trainingSummary() + ".");
        insights.add("Bloqueio principal monitorado: " + profile.obstacleLabel() + ".");

        return new AdaptivePlan(
                "Missoes inteligentes para " + primaryGoal,
                summary,
                adaptation,
                insights,
                dailyMissions,
                weeklyMissions,
                monthlyMissions
        );
    }

    private AdaptiveMissionResponse createAdaptiveMission(
            String area,
            String cadence,
            LocalDate date,
            int salt,
            boolean required,
            int dailyMinutes,
            int streak,
            PlannerProfile profile
    ) {
        String normalizedArea = area == null ? "habito" : area;
        int difficultyBoost = cadence.equals("monthly") ? 2 : cadence.equals("weekly") ? 1 : 0;
        int rewardBase = switch (cadence) {
            case "monthly" -> 180;
            case "weekly" -> 95;
            default -> 45;
        };
        int coinBase = switch (cadence) {
            case "monthly" -> 60;
            case "weekly" -> 28;
            default -> 12;
        };
        int streakBonus = Math.min(20, streak * 2);
        int timeFactor = Math.max(0, Math.min(30, (dailyMinutes - 60) / 10));
        int intensityBonus = profile.intensityBonus();
        String difficulty = switch (difficultyBoost + (required ? 1 : 0)) {
            case 0, 1 -> "leve";
            case 2 -> "medio";
            default -> "alto";
        };

        String goalSnippet = profile.goalSnippetForArea(normalizedArea);
        String title;
        String description;
        String rationale;
        String type = normalizedArea;
        String pillar = pillarForArea(normalizedArea);
        String cadenceLabel = cadenceLabel(cadence);
        String missionSeed = normalizedArea + "-" + cadence + "-" + rotationWeight(normalizedArea + cadence + goalSnippet, date, salt);

        switch (normalizedArea) {
            case "estudo" -> {
                int minutes = cadence.equals("daily") ? Math.min(120, Math.max(30, profile.adjustMinutesForEnergy(dailyMinutes / 2))) : cadence.equals("weekly") ? 240 : 720;
                title = cadence.equals("daily")
                        ? "Sprint de estudo: " + goalSnippet
                        : cadence.equals("weekly")
                            ? "Semana de consolidacao: " + goalSnippet
                            : "Marco mensal de estudo: " + goalSnippet;
                description = cadenceLabel + " - " + buildStudyDirective(cadence, minutes, profile);
                rationale = "Aprofunda conhecimento util para " + goalSnippet + " sem perder consistencia.";
            }
            case "trabalho" -> {
                title = cadence.equals("daily")
                        ? "Entrega de trabalho prioritario"
                        : cadence.equals("weekly")
                            ? "Avanco semanal da frente profissional"
                            : "Marco mensal profissional";
                description = cadenceLabel + " - " + buildWorkDirective(cadence, profile);
                rationale = "Protege a prioridade profissional e reduz dispersao operacional.";
            }
            case "saude" -> {
                int minutes = cadence.equals("daily") ? Math.min(90, Math.max(20, profile.adjustMinutesForEnergy(dailyMinutes / 3))) : cadence.equals("weekly") ? 180 : 600;
                title = cadence.equals("daily")
                        ? "Bloco de saude e energia"
                        : cadence.equals("weekly")
                            ? "Checkpoint fisico da semana"
                            : "Meta mensal de condicionamento";
                description = cadenceLabel + " - " + buildHealthDirective(cadence, minutes, profile);
                rationale = "Ajusta corpo e energia para sustentar as outras frentes.";
            }
            case "projeto" -> {
                title = cadence.equals("daily")
                        ? "Mover projeto pessoal um passo"
                        : cadence.equals("weekly")
                            ? "Entrega semanal do projeto"
                            : "Marco mensal do projeto";
                description = cadenceLabel + " - " + buildProjectDirective(cadence, profile);
                rationale = "Garante progresso visivel no projeto que mais aproxima voce da meta.";
            }
            default -> {
                type = "habito";
                title = cadence.equals("daily")
                        ? "Habit loop: disciplina de base"
                        : cadence.equals("weekly")
                            ? "Consolidar habito-chave"
                            : "Instalar rotina de longo prazo";
                description = cadenceLabel + " - " + buildHabitDirective(cadence, profile);
                rationale = "Usa repeticao guiada para tornar constancia automatica.";
            }
        }

        return new AdaptiveMissionResponse(
                "adaptive-" + missionSeed + "-" + date,
                type,
                title,
                description,
                rewardBase + streakBonus + timeFactor + intensityBonus + (required ? 12 : 0) + (difficultyBoost * 18),
                coinBase + Math.max(0, difficultyBoost * 12) + (required ? 6 : 0),
                required,
                cadence,
                pillar,
                difficulty,
                rationale,
                cadence.equals("daily")
        );
    }

    private AdaptiveMissionResponse createFocusMission(LocalDate date, int dailyMinutes, PlannerProfile profile, int streak) {
        int focusMinutes = Math.min(90, Math.max(20, profile.adjustMinutesForEnergy(dailyMinutes / 2)));
        String modeLabel = switch (profile.motivationProfile()) {
            case "desafio" -> "Boss room de foco";
            case "recompensa" -> "Sprint premium de foco";
            case "disciplina" -> "Guardiao de foco";
            default -> "Dungeon de foco profundo";
        };
        return new AdaptiveMissionResponse(
                "adaptive-focus-" + rotationWeight(profile.primaryGoals(), date, 97) + "-" + date,
                "foco",
                modeLabel,
                "Diaria - execute " + focusMinutes + " min sem interrupcao na prioridade " + profile.priorityLabel()
                        + " usando um ritual " + profile.focusRitualLabel() + ".",
                40 + Math.min(20, streak * 2) + (focusMinutes / 5) + profile.intensityBonus(),
                10 + (focusMinutes / 15),
                true,
                "daily",
                "Foco",
                focusMinutes >= 50 ? "medio" : "leve",
                "Cria tracao real no que mais importa hoje.",
                true
        );
    }

    private AdaptiveMissionResponse createPenaltyRecoveryMission(LocalDate date, PlannerProfile profile) {
        String title = profile.energyProfile().equals("baixa")
                ? "Penalty Quest: recuperar tracao sem se desgastar"
                : "Penalty Quest: recuperar consistencia";
        return new AdaptiveMissionResponse(
                "adaptive-penalty-" + rotationWeight(profile.obstacles(), date, 131) + "-" + date,
                "penalidade",
                title,
                "Diaria - execute um bloco curto na prioridade principal para evitar cair em " + profile.obstacleLabel()
                        + " e feche o dia com uma vitoria minima clara.",
                28,
                0,
                true,
                "daily",
                "Recuperacao",
                "leve",
                "Missao de seguranca para retomar ritmo quando a rotina oscila.",
                true
        );
    }

    private String buildStudyDirective(String cadence, int minutes, PlannerProfile profile) {
        if (cadence.equals("monthly")) {
            return "fechar um modulo, livro ou certificacao ligado a " + profile.goalSnippetForArea("estudo") + ".";
        }
        if (cadence.equals("weekly")) {
            return "acumular " + minutes + " min de estudo ativo com resumo, revisao e checkpoints " + profile.motivationProfileLabel() + ".";
        }
        return "estudar por " + minutes + " min com anotacoes acionaveis, revisao final de 5 min e fechamento do proximo passo.";
    }

    private String buildWorkDirective(String cadence, PlannerProfile profile) {
        if (cadence.equals("monthly")) {
            return "entregar um resultado mensuravel no trabalho que fortalece " + profile.goalSnippetForArea("trabalho") + ".";
        }
        if (cadence.equals("weekly")) {
            return "resolver a frente mais critica e fechar uma pendencia com impacto real.";
        }
        return "fechar a tarefa profissional de maior alavanca antes das demandas secundarias em um bloco " + profile.executionRhythmLabel(0) + ".";
    }

    private String buildHealthDirective(String cadence, int minutes, PlannerProfile profile) {
        if (cadence.equals("monthly")) {
            return "sustentar um bloco consistente de treino, sono ou alimentacao ligado a " + profile.goalSnippetForArea("saude")
                    + ", respeitando o perfil " + profile.trainingGoalLabel() + ".";
        }
        if (cadence.equals("weekly")) {
            return "somar " + minutes + " min em treinos, caminhada ou recuperacao ativa dentro da divisao " + profile.workoutDaysLabel() + ".";
        }
        return "cumprir " + minutes + " min de treino, mobilidade ou caminhada com intencao e foco em " + profile.trainingGoalLabel() + ".";
    }

    private String buildProjectDirective(String cadence, PlannerProfile profile) {
        if (cadence.equals("monthly")) {
            return "finalizar um marco visivel do projeto em " + profile.goalSnippetForArea("projeto") + ".";
        }
        if (cadence.equals("weekly")) {
            return "entregar uma parte concreta do projeto e registrar o proximo passo.";
        }
        return "avancar uma etapa objetiva do projeto sem abrir novas frentes.";
    }

    private String buildHabitDirective(String cadence, PlannerProfile profile) {
        if (cadence.equals("monthly")) {
            return "instalar uma rotina base que reduza atrito para " + profile.goalSnippetForArea("habito") + ".";
        }
        if (cadence.equals("weekly")) {
            return "registrar repeticao consistente do habito em pelo menos 5 dias.";
        }
        return "executar o habito-chave mesmo em versao minima para proteger disciplina com gatilho " + profile.motivationProfileLabel() + ".";
    }

    private String buildAdaptationNote(PlannerProfile profile, int streak, int dailyMissionTarget) {
        if (profile.requiresRecoveryMission(streak) && profile.energyProfile().equals("baixa")) {
            return "Sinal de desgaste detectado: reduzi a friccao, mantive o foco nas frentes criticas e inclui uma missao de recuperacao para preservar consistencia.";
        }
        if (streak >= 5 && dailyMissionTarget >= 3) {
            return "Sequencia forte detectada: aumentei o peso de entregas profundas, checkpoints semanais e metas com mais alavanca.";
        }
        if (profile.motivationProfile().equals("desafio")) {
            return "Plano calibrado com desafios curtos e claros para manter sua energia competitiva sem dispersar o foco.";
        }
        return "Plano calibrado para construir constancia primeiro, com passos curtos e relevantes antes de escalar dificuldade.";
    }

    private String pillarForArea(String area) {
        return switch (area) {
            case "estudo" -> "Estudos";
            case "trabalho" -> "Trabalho";
            case "saude" -> "Saude";
            case "projeto" -> "Projetos";
            case "foco" -> "Foco";
            default -> "Habitos";
        };
    }

    private String cadenceLabel(String cadence) {
        return switch (cadence) {
            case "weekly" -> "Semanal";
            case "monthly" -> "Mensal";
            default -> "Diaria";
        };
    }

    private record AdaptivePlan(
            String headline,
            String summary,
            String adaptationNote,
            List<String> insights,
            List<AdaptiveMissionResponse> dailyMissions,
            List<AdaptiveMissionResponse> weeklyMissions,
            List<AdaptiveMissionResponse> monthlyMissions
    ) {
    }

    private record PlannerProfile(
            String routineSummary,
            String primaryGoals,
            String currentPriorities,
            String studyFocus,
            String workFocus,
            String healthFocus,
            String projectFocus,
            String habitFocus,
            String obstacles,
            int availableMinutesPerDay,
            String energyPattern,
            String motivationStyle,
            List<String> workoutDays,
            String bodyType,
            String trainingGoal,
            String trainingLevel
    ) {
        private static PlannerProfile from(RoutineProfileRequest request) {
            if (request == null) {
                return starterProfile();
            }

            return new PlannerProfile(
                    safe(request.routineSummary(), "Quero estruturar melhor minha rotina."),
                    safe(request.primaryGoals(), "Construir consistencia e evoluir nas metas principais."),
                    safe(request.currentPriorities(), request.primaryGoals()),
                    safe(request.studyFocus(), ""),
                    safe(request.workFocus(), ""),
                    safe(request.healthFocus(), ""),
                    safe(request.projectFocus(), ""),
                    safe(request.habitFocus(), "Criar disciplina diaria."),
                    safe(request.obstacles(), "Dispersao e procrastinacao."),
                    request.availableMinutesPerDay() > 0 ? request.availableMinutesPerDay() : 90,
                    safe(request.energyPattern(), "media"),
                    safe(request.motivationStyle(), "progressao visivel"),
                    sanitizeWorkoutDays(request.workoutDays()),
                    safe(request.bodyType(), "mesomorfo"),
                    safe(request.trainingGoal(), "hipertrofia"),
                    safe(request.trainingLevel(), "iniciante")
            );
        }

        private static PlannerProfile starterProfile() {
            return new PlannerProfile(
                    "Quero organizar melhor meus dias.",
                    "Criar consistencia entre trabalho, estudos e saude.",
                    "Disciplina e progresso constante.",
                    "",
                    "",
                    "",
                    "",
                    "Constancia diaria.",
                    "Oscilacao de foco.",
                    90,
                    "media",
                    "progressao visivel",
                    List.of("segunda", "quarta", "sexta"),
                    "mesomorfo",
                    "hipertrofia",
                    "iniciante"
            );
        }

        private static String safe(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value.trim();
        }

        private static List<String> sanitizeWorkoutDays(List<String> days) {
            if (days == null || days.isEmpty()) {
                return List.of("segunda", "quarta", "sexta");
            }

            return days.stream()
                    .filter(day -> day != null && !day.isBlank())
                    .map(day -> day.toLowerCase(Locale.ROOT).trim())
                    .distinct()
                    .toList();
        }

        private List<String> activeAreas() {
            Set<String> areas = new LinkedHashSet<>();
            String combined = normalize(primaryGoals + " " + currentPriorities + " " + routineSummary);

            if (!studyFocus.isBlank() || contains(combined, "estud", "faculd", "curso", "prova", "aprend", "leitura")) {
                areas.add("estudo");
            }
            if (!workFocus.isBlank() || contains(combined, "trabalho", "emprego", "carreira", "cliente", "profission")) {
                areas.add("trabalho");
            }
            if (!healthFocus.isBlank() || contains(combined, "saud", "treino", "academ", "sono", "aliment", "cardio")) {
                areas.add("saude");
            }
            if (!projectFocus.isBlank() || contains(combined, "projeto", "negocio", "portfolio", "conteudo", "produto")) {
                areas.add("projeto");
            }
            if (!habitFocus.isBlank() || contains(combined, "rotina", "habito", "disciplina", "organiz", "constan")) {
                areas.add("habito");
            }

            if (areas.isEmpty()) {
                areas.addAll(Arrays.asList("trabalho", "estudo", "saude", "habito"));
            }

            return List.copyOf(areas);
        }

        private String primaryGoalLabel() {
            return firstMeaningful(primaryGoals);
        }

        private String priorityLabel() {
            return firstMeaningful(currentPriorities);
        }

        private String obstacleLabel() {
            return firstMeaningful(obstacles);
        }

        private String energyProfile() {
            String combined = normalize(energyPattern + " " + routineSummary + " " + obstacles);
            if (contains(combined, "baixa", "cans", "exaust", "pesad", "sono", "sem energia")) {
                return "baixa";
            }
            if (contains(combined, "alta", "forte", "pico", "intensa")) {
                return "alta";
            }
            return "media";
        }

        private String energyPatternLabel() {
            return switch (energyProfile()) {
                case "alta" -> "alta e pronta para carga";
                case "baixa" -> "baixa, pedindo atrito minimo";
                default -> "media, boa para consistencia";
            };
        }

        private String motivationProfile() {
            String combined = normalize(motivationStyle);
            if (contains(combined, "desafio", "compet", "boss", "meta agress")) {
                return "desafio";
            }
            if (contains(combined, "recomp", "premio", "moeda", "lazer")) {
                return "recompensa";
            }
            if (contains(combined, "disciplina", "constan", "ritual", "habito")) {
                return "disciplina";
            }
            return "progresso";
        }

        private String motivationProfileLabel() {
            return switch (motivationProfile()) {
                case "desafio" -> "desafio curto e mensuravel";
                case "recompensa" -> "recompensa visivel e rapida";
                case "disciplina" -> "disciplina e repeticao guiada";
                default -> "progresso visivel";
            };
        }

        private int adjustMinutesForEnergy(int baseMinutes) {
            return switch (energyProfile()) {
                case "alta" -> baseMinutes + 10;
                case "baixa" -> Math.max(20, baseMinutes - 10);
                default -> baseMinutes;
            };
        }

        private int intensityBonus() {
            int bonus = switch (energyProfile()) {
                case "alta" -> 10;
                case "baixa" -> -4;
                default -> 4;
            };
            if (trainingLevel.contains("avanc")) {
                bonus += 4;
            } else if (trainingLevel.contains("intermedi")) {
                bonus += 2;
            }
            return Math.max(0, bonus);
        }

        private int dailyMissionTarget(int streak) {
            int target = availableMinutesPerDay >= 150 ? 4 : availableMinutesPerDay >= 80 ? 3 : 2;
            if (energyProfile().equals("baixa")) {
                target--;
            }
            if (streak >= 6 && energyProfile().equals("alta")) {
                target++;
            }
            return Math.max(2, Math.min(4, target));
        }

        private boolean requiresRecoveryMission(int streak) {
            return streak <= 1
                    || energyProfile().equals("baixa")
                    || contains(normalize(obstacles), "adiar", "atraso", "procrast", "desorganiz", "cans");
        }

        private String executionRhythmLabel(int streak) {
            if (energyProfile().equals("baixa")) {
                return "conservador";
            }
            if (streak >= 5 || availableMinutesPerDay >= 120) {
                return "agressivo";
            }
            return "estavel";
        }

        private String trainingGoalLabel() {
            return trainingGoal.replace('-', ' ');
        }

        private String workoutDaysLabel() {
            if (workoutDays.isEmpty()) {
                return "3 dias base";
            }
            return workoutDays.size() + " dias (" + workoutDays.stream()
                    .map(day -> day.substring(0, 1).toUpperCase(Locale.ROOT) + day.substring(1, Math.min(day.length(), 3)))
                    .collect(Collectors.joining(", ")) + ")";
        }

        private String trainingSummary() {
            return workoutDaysLabel() + ", objetivo " + trainingGoalLabel() + ", nivel " + trainingLevel + " e biotipo " + bodyType + ".";
        }

        private String focusRitualLabel() {
            return switch (motivationProfile()) {
                case "desafio" -> "de desafio";
                case "recompensa" -> "de recompensa";
                case "disciplina" -> "de disciplina";
                default -> "de progresso";
            };
        }

        private String goalSnippetForArea(String area) {
            return switch (area) {
                case "estudo" -> firstMeaningful(studyFocus, primaryGoals);
                case "trabalho" -> firstMeaningful(workFocus, currentPriorities, primaryGoals);
                case "saude" -> firstMeaningful(healthFocus, primaryGoals);
                case "projeto" -> firstMeaningful(projectFocus, primaryGoals);
                default -> firstMeaningful(habitFocus, currentPriorities, primaryGoals);
            };
        }

        private static String firstMeaningful(String... values) {
            for (String value : values) {
                if (value != null) {
                    String normalized = value.replace('\n', ' ').trim();
                    if (!normalized.isBlank()) {
                        return normalized.length() > 72 ? normalized.substring(0, 72).trim() : normalized;
                    }
                }
            }
            return "sua evolucao";
        }

        private static boolean contains(String source, String... tokens) {
            for (String token : tokens) {
                if (source.contains(token)) {
                    return true;
                }
            }
            return false;
        }

        private static String normalize(String value) {
            return value == null ? "" : value.toLowerCase(Locale.ROOT);
        }
    }
}
