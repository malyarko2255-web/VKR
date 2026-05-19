package ru.finuniversity.advance.common.exception;

public class TripCompletedException extends AdvanceException {

    public TripCompletedException(String tripId) {
        super(String.format("Рейс %s уже завершён, авансирование невозможно", tripId),
                409, "TRIP_COMPLETED");
    }
}
