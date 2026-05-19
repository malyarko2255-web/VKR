package ru.finuniversity.advance.payment.exception;

public class SbpTimeoutException extends RuntimeException {

    public SbpTimeoutException(String message) {
        super(message);
    }
}
