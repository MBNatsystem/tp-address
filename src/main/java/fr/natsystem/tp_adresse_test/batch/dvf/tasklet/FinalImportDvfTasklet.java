package fr.natsystem.tp_adresse_test.batch.dvf.tasklet;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("finalImportDvfTasklet")
@AllArgsConstructor
@Slf4j
public class FinalImportDvfTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        // Index techniques sur le plan de synchronisation
        /*
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_sync_plan_delete_id
            ON address_sync_plan (id)
            WHERE action = 'DELETE';
            """);

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_sync_plan_insert_stage
            ON address_sync_plan (stage_id)
            WHERE action = 'INSERT';
            """);
        
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_sync_plan_update_stage
            ON address_sync_plan (stage_id)
            WHERE action = 'UPDATE';
            """);
        
        jdbcTemplate.execute("""
                ANALYZE row_address_dvf
                """);
        
        jdbcTemplate.execute("""
                ANALYZE address_sync_plan
                """);
        
        jdbcTemplate.execute("""
                ANALYZE address_dvf
                """);

        //Suppression des adresses supprimées
        jdbcTemplate.execute("""
                DELETE FROM address_dvf f
                USING address_sync_plan p
                WHERE p.action = 'DELETE'
                AND f.id = p.id;
                """);
        
        //Mise à jour des adresses existantes
        jdbcTemplate.execute("""
            UPDATE address_dvf
            SET
                date_mutation = s.date_mutation,
                numero_disposition = s.numero_disposition,
                nature_mutation = s.nature_mutation,
                valeur_fonciere = s.valeur_fonciere,
                adresse_numero = s.adresse_numero,
                adresse_suffixe = s.adresse_suffixe,
                adresse_code_voie = s.adresse_code_voie,
                adresse_nom_voie = s.adresse_nom_voie,
                code_postal = s.code_postal,
                code_commune = s.code_commune,
                nom_commune = s.nom_commune,
                ancien_code_commune = s.ancien_code_commune,
                ancien_nom_commune = s.ancien_nom_commune,
                code_departement = s.code_departement,
                id_parcelle = s.id_parcelle,
                ancien_id_parcelle = s.ancien_id_parcelle,
                numero_volume = s.numero_volume,
                lot1_numero = s.lot1_numero,
                lot1_surface_carrez = s.lot1_surface_carrez,
                lot2_numero = s.lot2_numero,
                lot2_surface_carrez = s.lot2_surface_carrez,
                lot3_numero = s.lot3_numero,
                lot3_surface_carrez = s.lot3_surface_carrez,
                lot4_numero = s.lot4_numero,
                lot4_surface_carrez = s.lot4_surface_carrez,
                lot5_numero = s.lot5_numero,
                lot5_surface_carrez = s.lot5_surface_carrez,
                nombre_lots = s.nombre_lots,
                code_type_local = s.code_type_local,
                type_local = s.type_local,
                surface_reelle_bati = s.surface_reelle_bati,
                nombre_pieces_principales = s.nombre_pieces_principales,
                code_nature_culture = s.code_nature_culture,
                nature_culture = s.nature_culture,
                code_nature_culture_speciale = s.code_nature_culture_speciale,
                nature_culture_speciale = s.nature_culture_speciale,
                surface_terrain = s.surface_terrain,
                longitude = s.longitude,
                latitude = s.latitude,
                line_hash = s.line_hash
            FROM address_sync_plan p
            JOIN row_address_dvf s
                ON s.stage_id = p.stage_id
            WHERE p.action = 'UPDATE'
            AND address_dvf.id = s.id;
            """);

        //Insertion des nouvelles adresses
        jdbcTemplate.update("""
            INSERT INTO address_dvf (
                id,
                date_mutation,
                numero_disposition,
                nature_mutation,
                valeur_fonciere,
                adresse_numero,
                adresse_suffixe,
                adresse_code_voie,
                adresse_nom_voie,
                code_postal,
                code_commune,
                nom_commune,
                ancien_code_commune,
                ancien_nom_commune,
                code_departement,
                id_parcelle,
                ancien_id_parcelle,
                numero_volume,
                lot1_numero,
                lot1_surface_carrez,
                lot2_numero,
                lot2_surface_carrez,
                lot3_numero,
                lot3_surface_carrez,
                lot4_numero,
                lot4_surface_carrez,
                lot5_numero,
                lot5_surface_carrez,
                nombre_lots,
                code_type_local,
                type_local,
                surface_reelle_bati,
                nombre_pieces_principales,
                code_nature_culture,
                nature_culture,
                code_nature_culture_speciale,
                nature_culture_speciale,
                surface_terrain,
                longitude,
                latitude
            )
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
            FROM address_sync_plan p
            JOIN row_address_dvf s
                ON s.stage_id = p.stage_id
            WHERE p.action = 'INSERT';
            """);*/

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
        
        jdbcTemplate.execute("""
            CREATE OR REPLACE VIEW vue_statistiques_communes AS
            SELECT
                code_commune,
                avg(valeur_fonciere) FILTER (
                    WHERE date_mutation >= (CURRENT_DATE - INTERVAL '1 year')
                ) AS prix_moyen_12_mois,

                avg(valeur_fonciere) FILTER (
                    WHERE date_mutation >= (CURRENT_DATE - INTERVAL '2 years')
                    AND date_mutation < (CURRENT_DATE - INTERVAL '1 year')
                ) AS prix_moyen_24_mois,

                percentile_cont(0.5) WITHIN GROUP (
                    ORDER BY valeur_fonciere::double precision
                ) FILTER (
                    WHERE date_mutation >= (CURRENT_DATE - INTERVAL '1 year')
                ) AS mediane_12_mois,

                percentile_cont(0.5) WITHIN GROUP (
                    ORDER BY valeur_fonciere::double precision
                ) FILTER (
                    WHERE date_mutation >= (CURRENT_DATE - INTERVAL '2 years')
                    AND date_mutation < (CURRENT_DATE - INTERVAL '1 year')
                ) AS mediane_24_mois,

                avg(valeur_fonciere / NULLIF(surface_terrain, 1::numeric)) FILTER (
                    WHERE date_mutation >= (CURRENT_DATE - INTERVAL '1 year')
                ) AS moyenne_m2_12_mois,

                avg(valeur_fonciere / NULLIF(surface_terrain, 1::numeric)) FILTER (
                    WHERE date_mutation >= (CURRENT_DATE - INTERVAL '2 years')
                    AND date_mutation < (CURRENT_DATE - INTERVAL '1 year')
                ) AS moyenne_m2_24_mois,

                count(*) FILTER (
                    WHERE date_mutation >= (CURRENT_DATE - INTERVAL '1 year')
                ) AS nombre_transactions_12_mois,

                count(*) FILTER (
                    WHERE date_mutation >= (CURRENT_DATE - INTERVAL '2 years')
                    AND date_mutation < (CURRENT_DATE - INTERVAL '1 year')
                ) AS nombre_transactions_24_mois

            FROM address_dvf
            WHERE date_mutation >= (CURRENT_DATE - INTERVAL '2 years')
            GROUP BY code_commune;
        """);
        
        return RepeatStatus.FINISHED;

    }
}