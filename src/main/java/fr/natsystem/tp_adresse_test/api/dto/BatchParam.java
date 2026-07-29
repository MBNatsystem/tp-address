package fr.natsystem.tp_adresse_test.api.dto;

import java.net.URI;

public record BatchParam(
    String inputDirectory,
    String archiveDirectory,
    Boolean downloadEnabled,
    URI downloadUrl,
    String downloadFileName,
    String extractFileName,
    String reportFile

) {
    
}
