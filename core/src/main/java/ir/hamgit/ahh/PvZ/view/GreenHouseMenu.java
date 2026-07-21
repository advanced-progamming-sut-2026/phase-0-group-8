package ir.hamgit.ahh.PvZ.view;

import ir.hamgit.ahh.PvZ.model.GreenHousePot;
import ir.hamgit.ahh.PvZ.model.Result;
import ir.hamgit.ahh.PvZ.model.User;

public final class GreenHouseMenu {
    private static final int ROWS = 4;
    private static final int COLUMNS = 5;

    public void showGreenhouse(User user) {
        System.out.println("Greenhouse (4 rows x 5 columns):");
        for (int y = 1; y <= ROWS; y++) {
            for (int x = 1; x <= COLUMNS; x++) {
                System.out.printf("%-24s", status(user.getGreenhousePot(x, y)));
            }
            System.out.println();
        }
    }

    private String status(GreenHousePot pot) {
        if (pot.isLocked()) {
            return "[LOCKED]";
        }
        if (pot.isEmpty()) {
            return "[EMPTY]";
        }
        if (pot.isReadyToCollect()) {
            return "[READY: " + pot.getPlantType() + "]";
        }
        return String.format("[%s: %.1fh left]", pot.getPlantType(), pot.hoursRemaining());
    }

    public void showResult(Result result) {
        System.out.println(result.getResult());
    }

    public void showError(String message) {
        System.out.println(message);
    }
}
