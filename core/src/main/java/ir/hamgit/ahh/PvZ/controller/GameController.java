package ir.hamgit.ahh.PvZ.controller;

import ir.hamgit.ahh.PvZ.controller.services.TileController;
import ir.hamgit.ahh.PvZ.model.Board;
import ir.hamgit.ahh.PvZ.model.Result;
import ir.hamgit.ahh.PvZ.model.Tile;
import ir.hamgit.ahh.PvZ.model.entities.Plant;

public class GameController {
    private final Board board;

    public GameController(Board board) {
        this.board = board;
    }

    public Result plant(Plant plant, int row, int col){
        if (board.getSunAmount() < plant.getDef().getSunCost()){
            return new Result(false, "not enough sun to plant.");
        }
        Tile tile = board.getToile(row, col);
        Result planting = TileController.getInstance().plantOnTile(plant, tile);
        if (planting.isSuccessful()){
            tile.plantHere(plant);
        }
        return planting;
    }
}
