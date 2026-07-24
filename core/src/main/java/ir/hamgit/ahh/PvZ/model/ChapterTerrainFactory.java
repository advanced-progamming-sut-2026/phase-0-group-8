package ir.hamgit.ahh.PvZ.model;

import ir.hamgit.ahh.PvZ.model.enums.ChapterType;
import ir.hamgit.ahh.PvZ.model.enums.TileType;

public final class ChapterTerrainFactory {

    private static final double GRAVE_CHANCE_EGYPT = 0.10;
    private static final double SLIPPERY_CHANCE_FROSTBITE = 0.08;
    private static final double FROZEN_GROUND_CHANCE = 0.12;
    private static final double NECROMANCY_CHANCE_DARK_AGES = 0.12;
    static final int NECROMANCY_MIN_COLUMN = 3;

    private ChapterTerrainFactory() {
    }

    public static Tile[][] buildTiles(ChapterType chapter, int rows, int columns, int difficulty) {
        Tile[][] tiles = new Tile[rows][columns];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                tiles[r][c] = new Tile(TileType.NORMAL);
            }
        }
        applyChapterFeatures(tiles, chapter, rows, columns);
        return tiles;
    }

    private static void applyChapterFeatures(Tile[][] tiles, ChapterType chapter, int rows, int columns) {
        switch (chapter) {
            case ANCIENT_EGYPT:
                scatterGraves(tiles, rows, columns, false);
                break;
            case FROSTBITE_CAVES:
                scatterSlipperyTiles(tiles, rows, columns);
                markIcyGround(tiles, rows, columns);
                break;
            case BIG_WAVE_BEACH:
                floodRightColumns(tiles, rows, columns);
                break;
            case DARK_AGES:
                scatterGraves(tiles, rows, columns, true);
                markNecromancyTiles(tiles, rows, columns);
                break;
            default:
                break;
        }
    }

    private static void scatterGraves(Tile[][] tiles, int rows, int columns, boolean darkAges) {
        int firstGraveColumn = Math.min(NECROMANCY_MIN_COLUMN, columns);
        for (int r = 0; r < rows; r++) {
            for (int c = firstGraveColumn; c < columns; c++) {
                if (Math.random() < GRAVE_CHANCE_EGYPT) {
                    tiles[r][c] = new Tile(TileType.GRAVE);
                    if (darkAges) {
                        tiles[r][c].rollDarkAgesReward();
                    }
                }
            }
        }
    }

    private static void scatterSlipperyTiles(Tile[][] tiles, int rows, int columns) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (Math.random() < SLIPPERY_CHANCE_FROSTBITE) {
                    boolean up = Math.random() < 0.5;
                    tiles[r][c] = new Tile(up ? TileType.SLIPPERY_UP : TileType.SLIPPERY_DOWN);
                }
            }
        }
    }

    private static void markIcyGround(Tile[][] tiles, int rows, int columns) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (tiles[r][c].getType() == TileType.NORMAL && Math.random() < FROZEN_GROUND_CHANCE) {
                    tiles[r][c].setType(TileType.ICY_GROUND);
                }
            }
        }
    }

    
    private static void floodRightColumns(Tile[][] tiles, int rows, int columns) {
        int waterStart = columns - Math.max(1, columns / 3);
        for (int r = 0; r < rows; r++) {
            for (int c = waterStart; c < columns; c++) {
                tiles[r][c] = new Tile(TileType.WATER);
            }
        }
    }

    private static void markNecromancyTiles(Tile[][] tiles, int rows, int columns) {
        for (int r = 0; r < rows; r++) {
            for (int c = NECROMANCY_MIN_COLUMN; c < columns; c++) {
                if (Math.random() < NECROMANCY_CHANCE_DARK_AGES) {
                    tiles[r][c].setHasNecromancy(true);
                }
            }
        }
    }
}
