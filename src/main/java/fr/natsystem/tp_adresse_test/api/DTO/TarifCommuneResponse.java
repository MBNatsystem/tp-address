package fr.natsystem.tp_adresse_test.api.DTO;

import java.math.BigDecimal;

public record TarifCommuneResponse(
        String codeInsee,
        BigDecimal prixMoyen12Mois,
        BigDecimal prixMoyenPeriodePrecedente,
        BigDecimal mediane12Mois,
        BigDecimal medianePeriodePrecedente,
        BigDecimal moyenneM2_12Mois,
        BigDecimal moyenneM2PeriodePrecedente,
        Long nombreTransactions12Mois,
        Long nombreTransactionsPeriodePrecedente,
        BigDecimal variationMoyenneM2Pct
) {
}