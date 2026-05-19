package ru.finuniversity.advance.reference.client;

import ru.finuniversity.advance.reference.dto.TripDto;

import java.util.List;
import java.util.UUID;

public interface KisClient {

    List<TripDto> getActiveTrips(UUID driverId);
}
