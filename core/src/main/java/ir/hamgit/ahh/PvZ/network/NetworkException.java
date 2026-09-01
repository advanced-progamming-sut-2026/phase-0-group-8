package ir.hamgit.ahh.PvZ.network;

 
public final class NetworkException extends RuntimeException {
    private final int statusCode;

    public NetworkException(String message) {
        this(0, message);
    }

    public NetworkException(int statusCode, String message) {
        super(message == null || message.isBlank() ? "Server request failed." : message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
