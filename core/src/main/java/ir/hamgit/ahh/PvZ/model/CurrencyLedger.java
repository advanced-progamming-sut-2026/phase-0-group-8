package ir.hamgit.ahh.PvZ.model;

/**
 * Tracks coins/diamonds earned during a level session (10% zombie-death drop
 * chance) until {@code GameController} drains them into the persistent
 * {@code User} wallet.
 */
class CurrencyLedger {

    private static final double DROP_CHANCE = 0.10;
    private static final double DIAMOND_ROLL_CEILING = 0.34;
    private static final double COIN_ROLL_CEILING = 0.67;
    private static final int COIN_DROP_AMOUNT = 50;

    private int coinsEarned;
    private int diamondsEarned;

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
            System.out.println("A zombie dropeed a pot; you have a new greenhouse pot now.");
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
}
