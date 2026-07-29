package fr.natsystem.tp_adresse_test.api.dto;

public record BatchLaunchResponse(
    Long jobExecutionId,
    String status
) {}
