package ir.hamgit.ahh.PvZ.model;

class CurrencyLedger {

    private static final double DROP_CHANCE = 0.10;
    private static final double DIAMOND_ROLL_CEILING = 0.34;
    private static final double COIN_ROLL_CEILING = 0.67;
    private static final int COIN_DROP_AMOUNT = 50;

    private int coinsEarned;
    private int diamondsEarned;
    private int potsEarned;

    void maybeDropCurrency() {
        if (Math.random() >= DROP_CHANCE) {
            return;
        }
        double roll = Math.random();
        if (roll < DIAMOND_ROLL_CEILING) {
            diamondsEarned += 1;
            System.out.println("A zombie dropeed a diamond; you have " + diamondsEarned + " diamonds now.");
        } else if (roll < COIN_ROLL_CEILING) {
            coinsEarned += COIN_DROP_AMOUNT;
            System.out.println("A zombie dropeed a coin; you have " + coinsEarned + " coins now.");
        } else {
            potsEarned++;
            System.out.println("A zombie dropped a greenhouse pot.");
        }
    }

    int drainCoinsEarned() {
        int c = coinsEarned;
        coinsEarned = 0;
        return c;
    }

    int drainDiamondsEarned() {
        int d = diamondsEarned;
        diamondsEarned = 0;
        return d;
    }

    int drainPotsEarned() {
        int result = potsEarned;
        potsEarned = 0;
        return result;
    }
}
