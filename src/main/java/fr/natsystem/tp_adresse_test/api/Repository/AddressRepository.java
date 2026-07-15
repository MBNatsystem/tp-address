package fr.natsystem.tp_adresse_test.api.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.natsystem.tp_adresse_test.api.Entity.Address;

public interface AddressRepository extends 
            JpaRepository<Address, String>, 
            JpaSpecificationExecutor<Address> {
    
    Page<Address> findAll(Specification<Address> spec, Pageable pageable);

    @Query(value = """
        SELECT *
        FROM ban_address_final
        WHERE lat BETWEEN :minLat AND :maxLat
        AND lon BETWEEN :minLon AND :maxLon
        ORDER BY
            ((lat - :lat) * (lat - :lat) +
            (lon - :lon) * (lon - :lon))
        LIMIT 1;
        """, nativeQuery = true)
    Address findNearestAddress(
        double lat,
        double minLat,
        double maxLat,
        double lon,
        double minLon,
        double maxLon
    );

    @Query(value = """
        SELECT baf.*
        FROM ban_address_final baf
        WHERE baf.search_vector @@ websearch_to_tsquery('simple', :fts)
        AND (:numero IS NULL OR baf.numero = :numero)
        AND (:codePostal IS NULL OR baf.code_postal = :codePostal)
        LIMIT 10
        """, nativeQuery = true)
    List<Address> findFts(
        @Param("numero") Integer numero,
        @Param("codePostal") String codePostal,
        @Param("fts") String fts
    );

    @Query(value = """
            SELECT baf.*
            FROM ban_address_final baf
            WHERE (:numero IS NULL OR baf.numero = :numero)
            AND (:codePostal IS NULL OR baf.code_postal = :codePostal)
            LIMIT 10
            """, nativeQuery = true)
    List<Address> find(
        @Param("numero") Integer numero, 
        @Param("codePostal") String codePostal);
}
