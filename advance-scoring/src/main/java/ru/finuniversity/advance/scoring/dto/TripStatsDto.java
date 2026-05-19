package ru.finuniversity.advance.scoring.dto;

public record TripStatsDto(
        long totalTrips,
        long completedTrips,
        long onTimeTrips
) {}
