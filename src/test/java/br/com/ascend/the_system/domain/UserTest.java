package br.com.ascend.the_system.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UserTest {

    @Test
    void shouldStartStreakOnFirstRoutineActivity() {
        User user = new User("Kael Hunter", "kael@ascend.local", "hash");
        LocalDate today = LocalDate.of(2026, 4, 27);

        user.registerRoutineActivity(today);

        assertEquals(1, user.getStreakDays());
        assertEquals(1, user.getDisplayedStreakDays(today));
        assertEquals(1, user.getDisplayedStreakDays(today.plusDays(1)));
    }

    @Test
    void shouldNotIncreaseStreakTwiceOnSameDay() {
        User user = new User("Kael Hunter", "kael@ascend.local", "hash");
        LocalDate today = LocalDate.of(2026, 4, 27);

        user.registerRoutineActivity(today);
        user.registerRoutineActivity(today);

        assertEquals(1, user.getStreakDays());
        assertEquals(1, user.getDisplayedStreakDays(today));
    }

    @Test
    void shouldIncreaseStreakOnConsecutiveDays() {
        User user = new User("Kael Hunter", "kael@ascend.local", "hash");
        LocalDate firstDay = LocalDate.of(2026, 4, 26);
        LocalDate secondDay = firstDay.plusDays(1);

        user.registerRoutineActivity(firstDay);
        user.registerRoutineActivity(secondDay);

        assertEquals(2, user.getStreakDays());
        assertEquals(2, user.getDisplayedStreakDays(secondDay));
        assertEquals(2, user.getDisplayedStreakDays(secondDay.plusDays(1)));
    }

    @Test
    void shouldHideExpiredStreakAndRestartAfterGap() {
        User user = new User("Kael Hunter", "kael@ascend.local", "hash");
        LocalDate firstDay = LocalDate.of(2026, 4, 24);
        LocalDate gapDay = firstDay.plusDays(3);

        user.registerRoutineActivity(firstDay);

        assertEquals(0, user.getDisplayedStreakDays(gapDay));

        user.registerRoutineActivity(gapDay);

        assertEquals(1, user.getStreakDays());
        assertEquals(1, user.getDisplayedStreakDays(gapDay));
    }

    @Test
    void shouldExposeAccumulatedExperienceAcrossLevels() {
        User user = new User("Kael Hunter", "kael@ascend.local", "hash");

        user.addRewards(1_200, 10);

        assertEquals(2, user.getLevel());
        assertEquals("D", user.getRank());
        assertEquals(1_200, user.getExperience());
        assertEquals(3_000, user.getNextLevelExperience());
        assertEquals(10, user.getCoins());
    }

    @Test
    void shouldUnlockNewHighRanksAtAdvancedLevels() {
        User user = new User("Kael Hunter", "kael@ascend.local", "hash");

        user.addRewards(300_000, 0);
        assertEquals("S2", user.getRank());

        user.addRewards(700_000, 0);
        assertEquals("SSS+", user.getRank());
        assertFalse(user.getRank().isBlank());
    }

    @Test
    void shouldApplyPenaltyByReducingExperienceAndCoins() {
        User user = new User("Kael Hunter", "kael@ascend.local", "hash");

        user.addRewards(15_100, 260);
        user.applyPenalty(50, 200);

        assertEquals(15_050, user.getExperience());
        assertEquals(60, user.getCoins());
        assertEquals(6, user.getLevel());
        assertEquals("B", user.getRank());
    }

    @Test
    void shouldReachHardcoreRankMilestones() {
        User user = new User("Kael Hunter", "kael@ascend.local", "hash");

        user.addRewards(5_000, 0);
        assertEquals(4, user.getLevel());
        assertEquals("C", user.getRank());

        user.addRewards(70_000, 0);
        assertEquals(10, user.getLevel());
        assertEquals("S", user.getRank());

        user.addRewards(225_000, 0);
        assertEquals(30, user.getLevel());
        assertEquals("S2", user.getRank());
    }
}
