package fr.natsystem.tp_adresse_test.batch.ban.config;

import java.net.URI;
import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "batch.address")
public class AddressBatchProperties {
    private Path inputDirectory;
    private Path archiveDirectory;
    private Boolean downloadEnabled;
    private URI downloadUrl;
    private String downloadFileName;
    private String extractFileName;
    private Path reportFile;
}
