package br.com.ascend.the_system.service;

import br.com.ascend.the_system.domain.AttributeType;
import br.com.ascend.the_system.domain.MissionKind;
import br.com.ascend.the_system.domain.QuestStatus;
import br.com.ascend.the_system.domain.SystemMission;
import br.com.ascend.the_system.domain.User;
import br.com.ascend.the_system.domain.UserAttribute;
import br.com.ascend.the_system.domain.UserQuest;
import br.com.ascend.the_system.domain.VaultTransaction;
import br.com.ascend.the_system.dto.QuestCompletionResponse;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.repository.UserAttributeRepository;
import br.com.ascend.the_system.repository.UserQuestRepository;
import br.com.ascend.the_system.repository.UserRepository;
import br.com.ascend.the_system.repository.VaultTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;

@Service
public class MissionEngineService {

    private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Map<AttributeType, Integer> DEFAULT_ATTRIBUTE_VALUES = buildDefaultAttributes();

    private final UserQuestRepository userQuestRepository;
    private final UserAttributeRepository userAttributeRepository;
    private final VaultTransactionRepository vaultTransactionRepository;
    private final UserRepository userRepository;

    public MissionEngineService(
            UserQuestRepository userQuestRepository,
            UserAttributeRepository userAttributeRepository,
            VaultTransactionRepository vaultTransactionRepository,
            UserRepository userRepository
    ) {
        this.userQuestRepository = userQuestRepository;
        this.userAttributeRepository = userAttributeRepository;
        this.vaultTransactionRepository = vaultTransactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public QuestCompletionResponse completeQuest(User user, Long userQuestId) {
        UserQuest userQuest = userQuestRepository.findByIdAndUser_Id(userQuestId, user.getId())
                .orElseThrow(() -> new BadRequestException("Missao diaria nao encontrada para este usuario."));

        if (userQuest.getStatus() == QuestStatus.COMPLETED) {
            throw new BadRequestException("Essa missao ja foi concluida.");
        }

        LocalDate today = LocalDate.now(APP_ZONE);
        if (!userQuest.getScheduledDate().equals(today)) {
            throw new BadRequestException("A missao selecionada nao pertence ao ciclo diario atual.");
        }

        SystemMission mission = userQuest.getMission();
        int experienceGranted = calculateExperienceReward(user, mission);
        int coinsGranted = mission.getCoinReward();

        userQuest.complete(experienceGranted, coinsGranted);
        increaseAttribute(user, mission.getPrimaryAttribute(), mission.getStatReward());
        if (mission.getSecondaryAttribute() != null) {
            increaseAttribute(user, mission.getSecondaryAttribute(), Math.max(1, mission.getStatReward() / 2));
        }

        user.addRewards(experienceGranted, coinsGranted);
        user.registerRoutineActivity(today);
        User savedUser = userRepository.save(user);

        if (coinsGranted > 0) {
            vaultTransactionRepository.save(new VaultTransaction(
                    savedUser,
                    VaultTransaction.VaultTransactionType.REWARD,
                    "Quest concluida: " + mission.getTitle(),
                    coinsGranted
            ));
        }

        UserAttribute primaryAttribute = getOrCreateAttribute(savedUser, mission.getPrimaryAttribute());

        return new QuestCompletionResponse(
                userQuest.getId(),
                mission.getTitle(),
                mission.getPillar().name(),
                savedUser.getRank(),
                savedUser.getLevel(),
                experienceGranted,
                coinsGranted,
                savedUser.getExperience(),
                savedUser.getNextLevelExperience(),
                savedUser.getCoins(),
                mission.getPrimaryAttribute().name(),
                primaryAttribute.getTotalValue()
        );
    }

    private int calculateExperienceReward(User user, SystemMission mission) {
        int streakBonus = Math.min(25, user.getDisplayedStreakDays(LocalDate.now(APP_ZONE)) * 3);
        int requiredBonus = mission.isRequiredMission() ? 10 : 0;
        int focusBonus = mission.getKind() == MissionKind.FOCUS ? mission.getFocusMinutes() / 5 : 0;
        return mission.getExperienceReward() + streakBonus + requiredBonus + focusBonus;
    }

    private void increaseAttribute(User user, AttributeType attributeType, int points) {
        UserAttribute userAttribute = getOrCreateAttribute(user, attributeType);
        userAttribute.increase(points);
        userAttributeRepository.save(userAttribute);
    }

    private UserAttribute getOrCreateAttribute(User user, AttributeType attributeType) {
        return userAttributeRepository.findByUser_IdAndAttributeType(user.getId(), attributeType)
                .orElseGet(() -> userAttributeRepository.save(new UserAttribute(
                        user,
                        attributeType,
                        DEFAULT_ATTRIBUTE_VALUES.getOrDefault(attributeType, 10)
                )));
    }

    private static Map<AttributeType, Integer> buildDefaultAttributes() {
        Map<AttributeType, Integer> defaults = new EnumMap<>(AttributeType.class);
        defaults.put(AttributeType.STRENGTH, 0);
        defaults.put(AttributeType.AGILITY, 0);
        defaults.put(AttributeType.VITALITY, 0);
        defaults.put(AttributeType.INTELLIGENCE, 0);
        defaults.put(AttributeType.SPIRIT, 0);
        return defaults;
    }
}
