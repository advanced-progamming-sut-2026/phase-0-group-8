package ir.hamgit.ahh.PvZ.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ArmorType {
    @JsonProperty("Cone")
    CONE,

    @JsonProperty("Bucket")
    BUCKET,

    @JsonProperty("Brick")
    BRICK,

    @JsonProperty("ShoulderArmor")
    SHOULDER_ARMOR,

    @JsonProperty("Crown")
    CROWN,

    @JsonProperty("Newspaper")
    NEWSPAPER,

    NONE
}
