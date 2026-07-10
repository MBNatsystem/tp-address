package fr.natsystem.tp_adresse_test.api.Entity;

import org.springframework.boot.context.properties.bind.Name;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "ban_address_final")
public class Address {
    @Id
    @Name("id")
    private String id;
    @Name("id_fantoir")
    private String idFantoir;
    @Name("numero")
    private Integer numero;
    @Name("rep")
    private String rep;
    @Name("nom_voie")
    private String nomVoie;
    @Name("code_postal")
    private String codePostal;
    @Name("code_insee")
    private String codeInsee;
    @Name("nom_commune")
    private String nomCommune;
    @Name("code_insee_ancienne_commune")
    private String codeInseeAncienneCommune;
    @Name("nom_ancienne_commune")
    private String nomAncienneCommune;
    @Name("x")
    private String x;
    @Name("y")
    private String y;
    @Name("lon")
    private String lon;
    @Name("lat")
    private String lat;
    @Name("type_position")
    private String typePosition;
    @Name("alias")
    private String alias;
    @Name("nom_ld")
    private String nomLd;
    @Name("libelle_acheminement")
    private String libelleAcheminement;
    @Name("nom_afnor")
    private String nomAfnor;
    @Name("source_position")
    private String sourcePosition;
    @Name("source_nom_voie")
    private String sourceNomVoie;
    @Name("certification_commune")
    private Integer certificationCommune;
    @Name("cad_parcelles")
    private String cadParcelles;
}
