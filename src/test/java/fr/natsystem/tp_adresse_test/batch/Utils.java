package fr.natsystem.tp_adresse_test.batch;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;


public class Utils {

    public static long importCsv(ClassPathResource resource, String copySql, JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            CopyManager copyManager = new CopyManager(
                    connection.unwrap(BaseConnection.class)
            );

            try (
                    InputStream inputStream = resource.getInputStream();
                    Reader reader = new InputStreamReader(
                            inputStream,
                            StandardCharsets.UTF_8
                    )
            ) {
                return copyManager.copyIn(copySql, reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });
    }
}
