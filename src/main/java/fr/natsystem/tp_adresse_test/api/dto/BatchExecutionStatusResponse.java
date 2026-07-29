package fr.natsystem.tp_adresse_test.api.dto;

public record BatchExecutionStatusResponse(
    Long jobExcutionId,
    String jobName,
    String status,
    String exitCode,
    String checksum,
    String message
) {}
