package fr.natsystem.tp_adresse_test.api.Repository;

import java.util.List;
import java.util.Optional;

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
        SELECT baf.*
        FROM ban_address_final AS baf
        WHERE ST_DWithin(
            baf.position,
            ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
            :radiusMeters
        )
        ORDER BY baf.position <-> ST_SetSRID(
            ST_MakePoint(:lon, :lat),
            4326
        )::geography
        LIMIT 1
        """, nativeQuery = true)
    Optional<Address> findNearestAddress(
        @Param("lon") double lon,
        @Param("lat") double lat,
        @Param("radiusMeters") double radiusMeters
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
    List<Address> findNumberAndCodePostal(
        @Param("numero") Integer numero, 
        @Param("codePostal") String codePostal);
}
