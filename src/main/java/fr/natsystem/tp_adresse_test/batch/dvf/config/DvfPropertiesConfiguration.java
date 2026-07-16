package fr.natsystem.tp_adresse_test.batch.dvf.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "batch.dvf")
public class DvfPropertiesConfiguration {
    private Path extractFileName;
}
