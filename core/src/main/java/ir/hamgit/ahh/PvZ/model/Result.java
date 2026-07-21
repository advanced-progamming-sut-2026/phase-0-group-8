package ir.hamgit.ahh.PvZ.model;

public class Result {
    private final boolean isSuccessful;
    private final String result;

    public Result(boolean isSuccessful, String result) {
        this.isSuccessful = isSuccessful;
        this.result = result;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public String getResult() {
        return result;
    }

    @Override
    public String toString() {
        return result;
    }
}
