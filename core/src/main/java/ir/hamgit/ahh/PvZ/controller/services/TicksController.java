package ir.hamgit.ahh.PvZ.controller.services;

import ir.hamgit.ahh.PvZ.model.Board;

public class TicksController {
    private static TicksController instance;

    private TicksController(){}

    public static TicksController getInstance(){
        if(instance == null){
            instance = new TicksController();
        }
        return instance;
    }

    public void advanceTime(Board board, int ticks){
        for (int i= 0; i <ticks; i++){
            processSingleTick(board);
        }
    }

    public void processSingleTick(Board board){
        board.incrementTickCount();
    }

}
