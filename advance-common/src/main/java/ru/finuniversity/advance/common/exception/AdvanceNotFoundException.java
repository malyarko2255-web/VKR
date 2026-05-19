package ru.finuniversity.advance.common.exception;

public class AdvanceNotFoundException extends AdvanceException {

    public AdvanceNotFoundException(String advanceId) {
        super(String.format("Заявка на аванс не найдена: %s", advanceId),
                404, "ADVANCE_NOT_FOUND");
    }
}
