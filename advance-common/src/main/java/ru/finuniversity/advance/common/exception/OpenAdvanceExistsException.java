package ru.finuniversity.advance.common.exception;

public class OpenAdvanceExistsException extends AdvanceException {

    public OpenAdvanceExistsException(String driverId, String openAdvanceId) {
        super(String.format(
                "У водителя %s уже есть открытая заявка: %s",
                driverId, openAdvanceId),
                409, "OPEN_ADVANCE_EXISTS");
    }
}
