package fr.natsystem.tp_adresse_test.batch.dvf.tasklet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import fr.natsystem.tp_adresse_test.batch.dvf.config.DvfPropertiesConfiguration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("finalImportDvfTasklet")
@AllArgsConstructor
@Slf4j
public class FinalImportDvfTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    private final DvfPropertiesConfiguration properties;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        // Index techniques sur le plan de synchronisation
        
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_stats_id_stage
            ON address_stats (stage_id)
            """);

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_row_id_stage
            ON row_address_dvf (stage_id)
            """);

            jdbcTemplate.execute("DROP TABLE address_dvf CASCADE");

            jdbcTemplate.update("""
                CREATE TABLE address_dvf AS
                SELECT
                    s.id,
                    s.date_mutation,
                    s.numero_disposition,
                    s.nature_mutation,
                    s.valeur_fonciere,
                    s.adresse_numero,
                    s.adresse_suffixe,
                    s.adresse_code_voie,
                    s.adresse_nom_voie,
                    s.code_postal,
                    s.code_commune,
                    s.nom_commune,
                    s.ancien_code_commune,
                    s.ancien_nom_commune,
                    s.code_departement,
                    s.id_parcelle,
                    s.ancien_id_parcelle,
                    s.numero_volume,
                    s.lot1_numero,
                    s.lot1_surface_carrez,
                    s.lot2_numero,
                    s.lot2_surface_carrez,
                    s.lot3_numero,
                    s.lot3_surface_carrez,
                    s.lot4_numero,
                    s.lot4_surface_carrez,
                    s.lot5_numero,
                    s.lot5_surface_carrez,
                    s.nombre_lots,
                    s.code_type_local,
                    s.type_local,
                    s.surface_reelle_bati,
                    s.nombre_pieces_principales,
                    s.code_nature_culture,
                    s.nature_culture,
                    s.code_nature_culture_speciale,
                    s.nature_culture_speciale,
                    s.surface_terrain,
                    s.longitude,
                    s.latitude
                FROM row_address_dvf s
                JOIN address_stats i
                ON s.stage_id = i.stage_id;
                """);

        Resource sqlResource = properties.getVueDvfStatistique();
        
        if (sqlResource == null) {
            throw new IllegalStateException(
                    "La propriété app.vue-dvf-statistique n'est pas configurée"
            );
        }

        if (!sqlResource.exists()) {
            throw new IllegalStateException(
                    "Le fichier SQL n'existe pas : "
                    + sqlResource.getDescription()
            );
        }

        String sql = readSql(sqlResource);

        jdbcTemplate.execute(sql);

        
        return RepeatStatus.FINISHED;

    }

    private String readSql(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }catch(IOException e){
            e.printStackTrace();
            return "";
        }
    }
}