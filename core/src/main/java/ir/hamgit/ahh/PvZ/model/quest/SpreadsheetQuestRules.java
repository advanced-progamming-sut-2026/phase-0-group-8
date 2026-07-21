package ir.hamgit.ahh.PvZ.model.quest;

import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.User;

import ir.hamgit.ahh.PvZ.model.def.PlantAbilityProfiles;
import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.enums.BehaviorType;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.PlantFamily;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.Tag;

import java.util.List;
import java.util.Set;

final class SpreadsheetQuestRules {

    private SpreadsheetQuestRules() {
    }

    static void record(User user, Board board, Set<PlantType> selected, ChapterType chapter,
                       boolean won, LevelQuestTelemetry.Snapshot session) {
        Set<PlantType> used = session.placements().isEmpty() ? selected : session.placements().keySet();
        recordCounters(user, board, chapter, session, used);
        recordWinConditions(user, board, chapter, won, session, used);
        updateMaximumDifficultyStreak(user, won);
    }

    private static void recordCounters(User user, Board board, ChapterType chapter,
                                       LevelQuestTelemetry.Snapshot session, Set<PlantType> used) {
        QuestService.increment(user, "daily_sun_catcher", session.sunCollected());
        if (chapterIndex(chapter) == QuestService.variable(user, "chapter_hunter")) {
            QuestService.increment(user, "chapter_hunter", session.kills());
        }
        PlantType chosen = PlantType.values()[QuestService.variable(user,
            "professional_unlocker") - 1];
        int plantKills = Math.max(0, session.kills() - session.mowerKills());
        if (usesOnlyOffensivePlant(used, chosen)) {
            QuestService.increment(user, "professional_unlocker", plantKills);
        }
        if (usesOnlyOffensivePlant(used, PlantType.CACTUS)) {
            QuestService.increment(user, "only_cactus", plantKills);
        }
        QuestService.increment(user, "speed_runner", session.earlyKills());
        QuestService.increment(user, "almost_won", session.firstColumnMowerlessKills());
        QuestService.increment(user, "mowing_time", session.mowerKills());
        QuestService.complete(user, "professional_destroyer",
            session.countPlants(SpreadsheetQuestRules::isExplosive) >= 3);
        PlantFamily family = familyVariable(user, "family_massacre");
        QuestService.complete(user, "family_massacre", plantKills > 0
            && offensivePlantsBelongTo(used, family));
    }

    private static void recordWinConditions(User user, Board board, ChapterType chapter, boolean won,
                                            LevelQuestTelemetry.Snapshot session,
                                            Set<PlantType> used) {
        if (!won) {
            return;
        }
        int allowedLosses = QuestService.variable(user, "economic_plant_eater") - 1;
        QuestService.complete(user, "economic_plant_eater", session.lostPlants() <= allowedLosses);
        QuestService.complete(user, "defense_master", board.getSunAmount() == 0);
        QuestService.complete(user, "symmetry", isMiddleRowSymmetric(board));
        PlantFamily banned = familyVariable(user, "limited_bloom");
        QuestService.complete(user, "limited_bloom", used.stream()
            .noneMatch(type -> PlantAbilityProfiles.getFamily(type) == banned));
        QuestService.complete(user, "night_or_morning", chapter != ChapterType.DARK_AGES
            && !used.isEmpty() && used.stream().allMatch(SpreadsheetQuestRules::isMushroom));
        QuestService.complete(user, "ocd", hasNoSymmetryExceptMiddleRow(board));
        QuestService.complete(user, "cloudy_day",
            session.countPlants(SpreadsheetQuestRules::producesSun) == 3);
        int column = QuestService.variable(user, "one_less_column") - 1;
        int row = QuestService.variable(user, "undefended_row") - 1;
        int cross = QuestService.variable(user, "undefended_cross") - 1;
        QuestService.complete(user, "one_less_column", isColumnEmpty(board, column));
        QuestService.complete(user, "undefended_row", isRowEmpty(board, row));
        QuestService.complete(user, "undefended_cross",
            isRowEmpty(board, cross) && isColumnEmpty(board, cross));
    }

    private static void updateMaximumDifficultyStreak(User user, boolean won) {
        if (won && user.getDifficulty() == 5) {
            QuestService.increment(user, "win_streak", 1);
        } else {
            QuestService.reset(user, "win_streak");
        }
    }

    private static int chapterIndex(ChapterType chapter) {
        return switch (chapter) {
            case ANCIENT_EGYPT -> 1;
            case FROSTBITE_CAVES -> 2;
            case BIG_WAVE_BEACH -> 3;
            case DARK_AGES -> 4;
            default -> 0;
        };
    }

    private static PlantFamily familyVariable(User user, String id) {
        return PlantFamily.values()[QuestService.variable(user, id) - 1];
    }

    private static boolean usesOnlyOffensivePlant(Set<PlantType> used, PlantType required) {
        return used.contains(required) && used.stream().filter(SpreadsheetQuestRules::isOffensive)
            .allMatch(type -> type == required);
    }

    private static boolean offensivePlantsBelongTo(Set<PlantType> used, PlantFamily family) {
        List<PlantType> attackers = used.stream().filter(SpreadsheetQuestRules::isOffensive).toList();
        return !attackers.isEmpty() && attackers.stream()
            .allMatch(type -> PlantAbilityProfiles.getFamily(type) == family);
    }

    static boolean isOffensive(PlantType type) {
        PlantDef def = PlantRegistry.get(type);
        return def != null && (def.getDamage() > 0 || def.hasTag(Tag.EXPLOSIVE)
            || def.hasBehavior(BehaviorType.INSTANT_KILL_ZOMBIE)
            || def.hasBehavior(BehaviorType.AQUATIC_INSTANT_KILL)
            || def.hasBehavior(BehaviorType.SWALLOW_ZOMBIE));
    }

    private static boolean isExplosive(PlantType type) {
        PlantDef def = PlantRegistry.get(type);
        return def.hasTag(Tag.EXPLOSIVE) || def.hasBehavior(BehaviorType.CONTACT_EXPLOSION)
            || def.hasBehavior(BehaviorType.AREA_EXPLOSION)
            || def.hasBehavior(BehaviorType.LANE_EXPLOSION)
            || def.hasBehavior(BehaviorType.BOARD_EXPLOSION);
    }

    private static boolean producesSun(PlantType type) {
        PlantDef def = PlantRegistry.get(type);
        return def.hasBehavior(BehaviorType.PRODUCE_SUN)
            || def.hasBehavior(BehaviorType.INSTANT_SUN);
    }

    private static boolean isMushroom(PlantType type) {
        return PlantRegistry.get(type).hasTag(Tag.SHROOM);
    }

    private static boolean isMiddleRowSymmetric(Board board) {
        for (int row = 0; row < board.getRows() / 2; row++) {
            for (int column = 0; column < board.getColumns(); column++) {
                if (!plantLayers(board, column, row).equals(
                    plantLayers(board, column, board.getRows() - 1 - row))) {
                    return false;
                }
            }
        }
        return board.getPlantsRemaining() > 0;
    }

    private static boolean hasNoSymmetryExceptMiddleRow(Board board) {
        if (board.getPlantsRemaining() == 0) {
            return false;
        }
        for (int row = 0; row < board.getRows() / 2; row++) {
            for (int column = 0; column < board.getColumns(); column++) {
                List<PlantType> first = plantLayers(board, column, row);
                List<PlantType> mirror = plantLayers(board, column, board.getRows() - 1 - row);
                if (!first.isEmpty() && first.equals(mirror)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<PlantType> plantLayers(Board board, int column, int row) {
        return board.getTileAt(column, row).getPlantLayers().stream()
            .map(plant -> plant.getDef().getType()).toList();
    }

    private static boolean isColumnEmpty(Board board, int column) {
        for (int row = 0; row < board.getRows(); row++) {
            if (!board.getTileAt(column, row).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRowEmpty(Board board, int row) {
        for (int column = 0; column < board.getColumns(); column++) {
            if (!board.getTileAt(column, row).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
