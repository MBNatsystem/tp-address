CREATE OR REPLACE VIEW vue_statistiques_dvf_commune AS

WITH statistiques AS (
    SELECT
        code_commune,

        AVG(valeur_fonciere) FILTER (
            WHERE date_mutation >= CURRENT_DATE - INTERVAL '1 year'
        ) AS prix_moyen_12_mois,

        AVG(valeur_fonciere) FILTER (
            WHERE date_mutation >= CURRENT_DATE - INTERVAL '2 years'
              AND date_mutation < CURRENT_DATE - INTERVAL '1 year'
        ) AS prix_moyen_periode_precedente,


        PERCENTILE_CONT(0.5) WITHIN GROUP (
            ORDER BY valeur_fonciere::double precision
        ) FILTER (
            WHERE date_mutation >= CURRENT_DATE - INTERVAL '1 year'
        ) AS mediane_12_mois,

        PERCENTILE_CONT(0.5) WITHIN GROUP (
            ORDER BY valeur_fonciere::double precision
        ) FILTER (
            WHERE date_mutation >= CURRENT_DATE - INTERVAL '2 years'
              AND date_mutation < CURRENT_DATE - INTERVAL '1 year'
        ) AS mediane_periode_precedente,



        AVG(
            valeur_fonciere / NULLIF(surface_terrain, 0)
        ) FILTER (
            WHERE date_mutation >= CURRENT_DATE - INTERVAL '1 year'
              AND surface_terrain > 0
        ) AS moyenne_m2_12_mois,

        AVG(
            valeur_fonciere / NULLIF(surface_terrain, 0)
        ) FILTER (
            WHERE date_mutation >= CURRENT_DATE - INTERVAL '2 years'
              AND date_mutation < CURRENT_DATE - INTERVAL '1 year'
              AND surface_terrain > 0
        ) AS moyenne_m2_periode_precedente,


        COUNT(*) FILTER (
            WHERE date_mutation >= CURRENT_DATE - INTERVAL '1 year'
        ) AS nombre_transactions_12_mois,

        COUNT(*) FILTER (
            WHERE date_mutation >= CURRENT_DATE - INTERVAL '2 years'
              AND date_mutation < CURRENT_DATE - INTERVAL '1 year'
        ) AS nombre_transactions_periode_precedente


    FROM address_dvf

    WHERE date_mutation >= CURRENT_DATE - INTERVAL '2 years'

    GROUP BY code_commune
)

SELECT
    code_commune,

    ROUND(prix_moyen_12_mois, 2)
        AS prix_moyen_12_mois,

    ROUND(prix_moyen_periode_precedente, 2)
        AS prix_moyen_periode_precedente,

    ROUND(mediane_12_mois::numeric, 2)
        AS mediane_12_mois,

    ROUND(mediane_periode_precedente::numeric, 2)
        AS mediane_periode_precedente,

    ROUND(moyenne_m2_12_mois, 2)
        AS moyenne_m2_12_mois,

    ROUND(moyenne_m2_periode_precedente, 2)
        AS moyenne_m2_periode_precedente,

    nombre_transactions_12_mois,

    nombre_transactions_periode_precedente,


    --Variations en pourcentage :
    ROUND(
        (
            (
                moyenne_m2_12_mois
                - moyenne_m2_periode_precedente
            )
            / NULLIF(moyenne_m2_periode_precedente, 0)
            * 100
        ),
        2
    ) AS variation_moyenne_m2

FROM statistiques;