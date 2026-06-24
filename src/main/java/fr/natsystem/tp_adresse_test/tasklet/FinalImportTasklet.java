package fr.natsystem.tp_adresse_test.tasklet;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;

@Component("finalImportTasklet")
@AllArgsConstructor
public class FinalImportTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        jdbcTemplate.update("""
            INSERT INTO ban_address_final (
                id,
                id_fantoir,
                numero,
                rep,
                nom_voie,
                code_postal,
                code_insee,
                nom_commune,
                code_insee_ancienne_commune,
                nom_ancienne_commune,
                x,
                y,
                lon,
                lat,
                type_position,
                alias,
                nom_ld,
                libelle_acheminement,
                nom_afnor,
                source_position,
                source_nom_voie,
                certification_commune,
                cad_parcelles,
                updated_at
            )
            SELECT
                s.id,
                s.id_fantoir,
                s.numero,
                s.rep,
                s.nom_voie,
                s.code_postal,
                s.code_insee,
                s.nom_commune,
                s.code_insee_ancienne_commune,
                s.nom_ancienne_commune,
                s.x,
                s.y,
                s.lon,
                s.lat,
                s.type_position,
                s.alias,
                s.nom_ld,
                s.libelle_acheminement,
                s.nom_afnor,
                s.source_position,
                s.source_nom_voie,
                s.certification_commune,
                s.cad_parcelles,
                datetime('now')
            FROM address_staging s
            WHERE s.stage_id IN (
                SELECT stage_id
                FROM address_to_insert
            )
            ON CONFLICT(id) DO UPDATE SET
                id_fantoir = excluded.id_fantoir,
                numero = excluded.numero,
                rep = excluded.rep,
                nom_voie = excluded.nom_voie,
                code_postal = excluded.code_postal,
                code_insee = excluded.code_insee,
                nom_commune = excluded.nom_commune,
                code_insee_ancienne_commune = excluded.code_insee_ancienne_commune,
                nom_ancienne_commune = excluded.nom_ancienne_commune,
                x = excluded.x,
                y = excluded.y,
                lon = excluded.lon,
                lat = excluded.lat,
                type_position = excluded.type_position,
                alias = excluded.alias,
                nom_ld = excluded.nom_ld,
                libelle_acheminement = excluded.libelle_acheminement,
                nom_afnor = excluded.nom_afnor,
                source_position = excluded.source_position,
                source_nom_voie = excluded.source_nom_voie,
                certification_commune = excluded.certification_commune,
                cad_parcelles = excluded.cad_parcelles,
                updated_at = datetime('now');
        """);

        return RepeatStatus.FINISHED;

    }
}