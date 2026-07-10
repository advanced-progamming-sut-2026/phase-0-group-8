package ir.hamgit.ahh.PvZ.model;

public class Board {
    private Tile[][] tiles;
    private int sunAmount;
    private int tickCount;

    public Tile getToile(int row, int col){
        return tiles[row][col];
    }

    public int getSunAmount() {
        return sunAmount;
    }
    public void decreaseSunAmount (int cost){
        sunAmount -= cost;
    }

    public void incrementTickCount(){
        tickCount++;
    }

    public void explodeRadioactiveSun(int x, int lane) {
    }
}
