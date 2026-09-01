package ir.hamgit.ahh.PvZ.server;

 
final class ApiException extends RuntimeException {
    private final int status;

    ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    int status() { return status; }

    static ApiException badRequest(String message) { return new ApiException(400, message); }
    static ApiException unauthorized(String message) { return new ApiException(401, message); }
    static ApiException forbidden(String message) { return new ApiException(403, message); }
    static ApiException notFound(String message) { return new ApiException(404, message); }
    static ApiException conflict(String message) { return new ApiException(409, message); }
}
