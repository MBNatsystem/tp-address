package fr.natsystem.tp_adresse_test.batch.dvf.processor;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import fr.natsystem.tp_adresse_test.batch.dvf.model.DvfStage;
import fr.natsystem.tp_adresse_test.batch.dvf.model.RowAddressDvf;
import fr.natsystem.tp_adresse_test.batch.utils.Hash;

public class DvfStageProcessor implements ItemProcessor<RowAddressDvf,DvfStage>{

    @Override
    public @Nullable DvfStage process(RowAddressDvf rowDvf) throws Exception {
        return setDvfStage(rowDvf);
    }
    
    private DvfStage setDvfStage(RowAddressDvf dvf) {

        DvfStage stage = new DvfStage();

        stage.setLineNumber(dvf.lineNumber());
        stage.setLineHash(buildHash(dvf));

        stage.setIdMutation(dvf.idMutation());
        stage.setDateMutation(dvf.dateMutation());
        stage.setNumeroDisposition(dvf.numeroDisposition());
        stage.setNatureMutation(dvf.natureMutation());
        stage.setValeurFonciere(dvf.valeurFonciere());

        stage.setAdresseNumero(dvf.adresseNumero());
        stage.setAdresseSuffixe(dvf.adresseSuffixe());
        stage.setAdresseCodeVoie(dvf.adresseCodeVoie());
        stage.setAdresseNomVoie(dvf.adresseNomVoie());

        stage.setCodePostal(dvf.codePostal());
        stage.setCodeCommune(dvf.codeCommune());
        stage.setNomCommune(dvf.nomCommune());
        stage.setAncienCodeCommune(dvf.ancienCodeCommune());
        stage.setAncienNomCommune(dvf.ancienNomCommune());
        stage.setCodeDepartement(dvf.codeDepartement());

        stage.setIdParcelle(dvf.idParcelle());
        stage.setAncienIdParcelle(dvf.ancienIdParcelle());
        stage.setNumeroVolume(dvf.numeroVolume());

        stage.setLot1Numero(dvf.lot1Numero());
        stage.setLot1SurfaceCarrez(dvf.lot1SurfaceCarrez());

        stage.setLot2Numero(dvf.lot2Numero());
        stage.setLot2SurfaceCarrez(dvf.lot2SurfaceCarrez());

        stage.setLot3Numero(dvf.lot3Numero());
        stage.setLot3SurfaceCarrez(dvf.lot3SurfaceCarrez());

        stage.setLot4Numero(dvf.lot4Numero());
        stage.setLot4SurfaceCarrez(dvf.lot4SurfaceCarrez());

        stage.setLot5Numero(dvf.lot5Numero());
        stage.setLot5SurfaceCarrez(dvf.lot5SurfaceCarrez());

        stage.setNombreLots(dvf.nombreLots());

        stage.setCodeTypeLocal(dvf.codeTypeLocal());
        stage.setTypeLocal(dvf.typeLocal());
        stage.setSurfaceReelleBati(dvf.surfaceReelleBati());
        stage.setNombrePiecesPrincipales(dvf.nombrePiecesPrincipales());

        stage.setCodeNatureCulture(dvf.codeNatureCulture());
        stage.setNatureCulture(dvf.natureCulture());
        stage.setCodeNatureCultureSpeciale(dvf.codeNatureCultureSpeciale());
        stage.setNatureCultureSpeciale(dvf.natureCultureSpeciale());
        stage.setSurfaceTerrain(dvf.surfaceTerrain());

        stage.setLongitude(dvf.longitude());
        stage.setLatitude(dvf.latitude());

        return stage;
    }

    private String buildHash(RowAddressDvf dvf) {
        Hash h = new Hash();
        return h.fastHash(dvf.rawLine());
    }
}
