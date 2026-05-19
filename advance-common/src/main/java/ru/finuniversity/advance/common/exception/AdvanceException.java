package ru.finuniversity.advance.common.exception;

public class AdvanceException extends RuntimeException {

    private final int httpStatus;
    private final String errorCode;

    public AdvanceException(String message, int httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public AdvanceException(String message, Throwable cause, int httpStatus, String errorCode) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public int getHttpStatus()  { return httpStatus; }
    public String getErrorCode() { return errorCode; }
}
