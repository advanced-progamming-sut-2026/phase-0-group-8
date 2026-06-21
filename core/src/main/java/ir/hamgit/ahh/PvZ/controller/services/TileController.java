package ir.hamgit.ahh.PvZ.controller.services;

import ir.hamgit.ahh.PvZ.model.Result;
import ir.hamgit.ahh.PvZ.model.Tile;
import ir.hamgit.ahh.PvZ.model.entities.Plant;
import ir.hamgit.ahh.PvZ.model.enums.PlantType;

public class TileController {
    private static TileController instance;

    private TileController(){};

    public static TileController getInstance(){
        if(instance == null){
            instance = new TileController();
        }
        return instance;
    }

    public Result plantOnTile(Plant plant, Tile tile){
        Result result;
        result = switch (tile.getType()){
            case GRAVE -> new Result(false, "Can't plant on grave stone.");
            case SLIPPERY_UP, SLIPPERY_DOWN -> new Result(false, "Can't plant on slippery ground.");
            case ICY_GROUND -> new Result(false, "Can't plant on icy ground");
            case NECROMANCY -> new Result(false, "Can't plant on necromancy ground");
            case NORMAL -> isNormalTileAvailable(plant, tile);
            case WATER -> isWaterTileAvailable(plant, tile);
        };
        return result;
    }

    private Result isWaterTileAvailable(Plant plant, Tile tile){
        if (tile.getPlant() == null){
            if (plant.getDef().canPlantOnWater()){
                return new Result(true, "planted successfully!");
            }
            else {
                return new Result(false, "can not plant this plant on water.");
            }
        }
        else {
            if (tile.getPlant().getDef().getType() != PlantType.LILY_PAD){
                return new Result(false, "can not plant on top of this plant.");
            }
            else {
                return new Result(true, "planted successfully!");
            }
        }
    }


    private Result isNormalTileAvailable(Plant plant, Tile tile){
        if(tile.getPlant() == null){
            return new Result(true, "planted successfully!");
        }
        else {
            if (plant.getDef().canStackOn()){
                return new Result(true, "planted successfully!");
            }
            else {
                return new Result(false, "can not plant on top of this plant.");
            }
        }
    }




}
