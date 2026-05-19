package ru.finuniversity.advance.reference.exception;

import ru.finuniversity.advance.common.exception.AdvanceException;

import java.util.UUID;

public class DriverNotFoundException extends AdvanceException {

    public DriverNotFoundException(UUID driverId) {
        super("Driver not found: " + driverId, 404, "DRIVER_NOT_FOUND");
    }
}
