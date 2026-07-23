package fr.natsystem.tp_adresse_test.api.DTO;

public record BatchLaunchResponse(
    Long jobExecutionId,
    String status
) {}
