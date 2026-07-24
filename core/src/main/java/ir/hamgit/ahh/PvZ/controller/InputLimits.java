package ir.hamgit.ahh.PvZ.controller;


final class InputLimits {

    static final int MAX_COMMAND_LENGTH = 4_096;
    static final int MAX_TICKS_PER_COMMAND = 10_000;

    private InputLimits() {
    }

    static boolean isSafeTickCount(int ticks) {
        return ticks > 0 && ticks <= MAX_TICKS_PER_COMMAND;
    }
}
