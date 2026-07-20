package ir.hamgit.ahh.PvZ.model.special;


import ir.hamgit.ahh.PvZ.model.def.PlantDef;
import ir.hamgit.ahh.PvZ.model.registry.PlantRegistry;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;
import ir.hamgit.ahh.PvZ.model.enums.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * <ol>
 *   <li><b>Family lockout</b>: one plant is taken from a family (plants sharing a
 *   {@link Tag}, e.g. all PEA shooters) and the rest of that family is locked for
 *   the level.</li>
 *   <li><b>Forced starter kit</b>: a handful of specific plants are the only ones
 *   available - the player has no choice but to start with exactly them.</li>
 * </ol>
 * Use {@link #familyLockout(Tag, PlantType)} or {@link #forcedStarterKit(Set)} to
 * build the right variant; {@code GameController} picks one when it builds this
 * handler for a given chapter/level slot.
 */
public class LockedPlantsLevel extends SpecialLevelHandler {

    private final Set<PlantType> lockedTypes;
    private final Set<PlantType> forcedTypes;

    private LockedPlantsLevel(Set<PlantType> lockedTypes, Set<PlantType> forcedTypes) {
        this.lockedTypes = lockedTypes;
        this.forcedTypes = forcedTypes;
    }

    /** Variant 1: locks every other plant sharing {@code familyTag} except {@code keepAvailable}. */
    public static LockedPlantsLevel familyLockout(Tag familyTag, PlantType keepAvailable) {
        Set<PlantType> locked = EnumSet.noneOf(PlantType.class);
        for (PlantDef def : PlantRegistry.getAll()) {
            if (def.hasTag(familyTag) && def.getType() != keepAvailable) {
                locked.add(def.getType());
            }
        }
        return new LockedPlantsLevel(locked, Collections.emptySet());
    }

    /** Variant 2: only {@code mandatoryPlants} are selectable - the player is forced to start with them. */
    public static LockedPlantsLevel forcedStarterKit(Set<PlantType> mandatoryPlants) {
        return new LockedPlantsLevel(Collections.emptySet(), EnumSet.copyOf(mandatoryPlants));
    }

    @Override
    public Set<PlantType> getLockedPlantTypes() {
        return lockedTypes;
    }

    @Override
    public boolean isSelectablePlant(PlantType type) {
        if (!forcedTypes.isEmpty()) {
            return forcedTypes.contains(type);
        }
        return !lockedTypes.contains(type);
    }

    public List<PlantType> getForcedTypes() {
        return new ArrayList<>(forcedTypes);
    }
}
