package ru.finuniversity.advance.common.exception;

public class UnauthorizedActionException extends AdvanceException {

    public UnauthorizedActionException(String userId, String action) {
        super(String.format("Пользователь %s не имеет права выполнить действие: %s", userId, action),
                403, "UNAUTHORIZED_ACTION");
    }
}
