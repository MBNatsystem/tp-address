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

        //Suppression des index pour améliorer les performances de l'import
        jdbcTemplate.execute("""
                DROP INDEX IF EXISTS idx_code_postal;
                """);
        
        jdbcTemplate.execute("""
                DROP INDEX IF EXISTS idx_insee;
                """);
        
        jdbcTemplate.execute("""
                DROP INDEX IF EXISTS idx_commune;
                """);
        
        jdbcTemplate.execute("""
                DROP INDEX IF EXISTS idx_voie;
                """);

        //Insertion des nouvelles adresses
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
                line_hash,
                created_at,
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
                s.line_hash,
                datetime('now'),
                datetime('now')
            FROM address_sync_plan p
            JOIN address_staging s
                ON s.stage_id = p.stage_id
            WHERE p.action = 'INSERT';
        """);

        //Mise à jour des adresses existantes
        jdbcTemplate.execute("""
                UPDATE ban_address_final
                SET
                    id_fantoir = s.id_fantoir,
                    numero = s.numero,
                    rep = s.rep,
                    nom_voie = s.nom_voie,
                    code_postal = s.code_postal,
                    code_insee = s.code_insee,
                    nom_commune = s.nom_commune,
                    code_insee_ancienne_commune = s.code_insee_ancienne_commune,
                    nom_ancienne_commune = s.nom_ancienne_commune,
                    x = s.x,
                    y = s.y,
                    lon = s.lon,
                    lat = s.lat,
                    type_position = s.type_position,
                    alias = s.alias,
                    nom_ld = s.nom_ld,
                    libelle_acheminement = s.libelle_acheminement,
                    nom_afnor = s.nom_afnor,
                    source_position = s.source_position,
                    source_nom_voie = s.source_nom_voie,
                    certification_commune = s.certification_commune,
                    cad_parcelles = s.cad_parcelles,
                    line_hash = s.line_hash,
                    updated_at = datetime('now')
                FROM address_sync_plan p
                JOIN address_staging s
                    ON s.stage_id = p.stage_id
                WHERE p.action = 'UPDATE'
                AND ban_address_final.id = s.id;
                """);
        
                //Suppression des adresses supprimées
        jdbcTemplate.execute("""
                DELETE FROM ban_address_final
                WHERE id IN (
                    SELECT id
                    FROM address_sync_plan
                    WHERE action = 'DELETE'
                );
                """);
        
        //Recréation des index pour améliorer les performances des requêtes de l'api
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_code_postal ON ban_address_final(code_postal);
                """);
        
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_insee ON ban_address_final(code_insee);
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_commune ON ban_address_final(nom_commune);
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_voie ON ban_address_final(nom_voie);
                """);
        
        jdbcTemplate.execute("PRAGMA optimize");

        return RepeatStatus.FINISHED;

    }
}