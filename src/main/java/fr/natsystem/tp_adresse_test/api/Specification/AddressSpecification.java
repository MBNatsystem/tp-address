package fr.natsystem.tp_adresse_test.api.Specification;

import java.util.stream.Stream;

import org.springframework.data.jpa.domain.Specification;

import fr.natsystem.tp_adresse_test.api.Entity.Address;

public class AddressSpecification {

    public static Specification<Address> globalSearch(String codePostal, String nomCommune, String codeInsee, String nomVoie) {
        return Specification.allOf(
            Stream.of(
                hasCodePostal(codePostal),
                hasNomCommune(nomCommune),
                hasCodeInsee(codeInsee),
                hasNomVoie(nomVoie)
            )
            .filter(spec -> spec != null)
            .toList()
        );
    }

    public static Specification<Address> addressSearch(Integer numero, String nomVoie, String rep, String nomCommune, String codePostal) {
        return Specification.allOf(
            Stream.of(
                hasNumero(numero),
                hasNomVoie(nomVoie),
                hasRep(rep),
                hasNomCommune(nomCommune),
                hasCodePostal(codePostal)
            )
            .filter(spec -> spec != null)
            .toList()
        );
    }
    
    public static Specification<Address> hasCodePostal(String codePostal) {
        if(codePostal == null || codePostal.isEmpty()) {
            return null;
        }
        return (r, q, cb) -> cb.equal(r.get("codePostal"), codePostal);
    }

    public static Specification<Address> hasNomCommune(String nomCommune) {
        if(nomCommune == null || nomCommune.isEmpty()) {
            return null;
        }
        return (r, q, cb) -> cb.like(r.get("libelleAcheminement"), nomCommune.toUpperCase() + "%");
    }

    public static Specification<Address> hasCodeInsee(String codeInsee) {
        if(codeInsee == null || codeInsee.isEmpty()) {
            return null;
        }
        return (r, q, cb) -> cb.equal(cb.lower(r.get("codeInsee")), codeInsee.toLowerCase());
    }

    public static Specification<Address> hasNomVoie(String nomVoie) {
        if(nomVoie == null || nomVoie.isEmpty()) {
            return null;
        }
        return (r, q, cb) -> cb.like(r.get("nomAfnor"), nomVoie.toUpperCase() + "%");
    }

    public static Specification<Address> hasNumero(Integer numero) {
        if(numero == null) {
            return null;
        }
        return (r, q, cb) -> cb.equal(r.get("numero"), numero);
    }

    public static Specification<Address> hasRep(String rep) {
        if(rep == null || rep.isEmpty()) {
            return null;
        }
        return (r, q, cb) -> cb.equal(r.get("rep"), rep);
    }
}
