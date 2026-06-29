package ir.hamgit.ahh.PvZ.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ZombieType {
    @JsonProperty("ZombieDefault")
    DEFAULT,

    @JsonProperty("ZombieArmor1")
    CONEHEAD,

    @JsonProperty("ZombieArmor2")
    BUCKETHEAD,

    @JsonProperty("ZombieArmor4")
    BRICKHEAD,

    @JsonProperty("ZombieDarkArmor3")
    KNIGHT,

    @JsonProperty("ZombieGargantuar")
    GARGANTUAR,

    @JsonProperty("ZombieImp")
    IMP,

    @JsonProperty("ZombieRa")
    RA,

    @JsonProperty("ZombieExplorer")
    EXPLORER,

    @JsonProperty("ZombieTombRaiser")
    TOMB_RAISER,

    @JsonProperty("ZombieIceAgeDodo")
    DODO,

    @JsonProperty("ZombieIceAgeHunter")
    HUNTER,

    @JsonProperty("ZombieIceAgeTroglobite")
    TROGLOBITE,

    @JsonProperty("ZombieBeachFisherman")
    FISHERMAN,

    @JsonProperty("ZombieBeachOctopus")
    OCTOPUS,

    @JsonProperty("ZombieBeachSnorkel")
    SNORKEL,

    @JsonProperty("ZombieDarkJuggler")
    JUGGLER,

    @JsonProperty("ZombieWizard")
    WIZARD,

    @JsonProperty("ZombieDarkKing")
    KING,

    @JsonProperty("ZombieDarkImpDragon")
    IMP_DRAGON,

    @JsonProperty("ZombieModernAllStar")
    ALLSTAR,

    @JsonProperty("ZombieArcade")
    ARCADE,

    @JsonProperty("ZombieLostCityJane")
    UMBRELLA,

    @JsonProperty("ZombieCrystalSkull")
    TURQUOISE,

    @JsonProperty("ZombieProspector")
    PROSPECTOR,

    @JsonProperty("ZombiePiano")
    PIANO,

    @JsonProperty("ZombieNewspaper")
    NEWSPAPER
}
