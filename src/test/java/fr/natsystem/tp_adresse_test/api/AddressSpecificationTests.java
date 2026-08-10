package fr.natsystem.tp_adresse_test.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import fr.natsystem.tp_adresse_test.api.entity.Address;
import fr.natsystem.tp_adresse_test.api.repository.AddressRepository;
import fr.natsystem.tp_adresse_test.api.specification.AddressSpecification;

@SpringBootTest
@ActiveProfiles("test")
class AddressSpecificationTests {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AddressRepository addressRepository;

    @BeforeEach
    void setUp(){
        jdbcTemplate.execute("TRUNCATE TABLE ban_address_final;");
        importCsv("api/specification-test-predata.csv");
    }

    public long importCsv(String file) {
        return jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            CopyManager copyManager = new CopyManager(
                    connection.unwrap(BaseConnection.class)
            );

            ClassPathResource resource =
                    new ClassPathResource(file);

            String copySql = """
                    COPY ban_address_final
                    FROM STDIN
                    WITH (
                        FORMAT CSV,
                        HEADER TRUE,
                        DELIMITER ';',
                        ENCODING 'UTF8'
                    )
                    """;

            try (
                    InputStream inputStream = resource.getInputStream();
                    Reader reader = new InputStreamReader(
                            inputStream,
                            StandardCharsets.UTF_8
                    )
            ) {
                return copyManager.copyIn(copySql, reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/api/specification-test.csv", delimiter = ';', numLinesToSkip = 1, nullValues = "NULL")
    void shouldFilterAddresses(
        String typeRecherche,
        String codePostal,
        String nomCommune,
        String codeInsee,
        String nomVoie,
        Integer numero,         
        String rep,
        Integer expectedCount,
        String expectedId
    ){
        Specification<Address> specification = switch (typeRecherche) {
            case "CODE_POSTAL"-> AddressSpecification.hasCodePostal(codePostal);
            case "COMMUNE" -> AddressSpecification.hasNomCommune(nomCommune);
            case "CODE_INSEE" -> AddressSpecification.hasCodeInsee(codeInsee);
            case "VOIE" -> AddressSpecification.hasNomVoie(nomVoie);
            case "NUMERO" -> AddressSpecification.hasNumero(numero);
            case "REP" -> AddressSpecification.hasRep(rep);
            case "GLOBAL" -> AddressSpecification.globalSearch(codePostal, nomCommune, codeInsee, nomVoie);
            case "ADDRESS" -> AddressSpecification.addressSearch(numero, nomVoie, rep, nomCommune, codePostal);
        
            default->
            throw new IllegalStateException("Type de recherche inconnu: " + typeRecherche);
        };

        List<Address> result = addressRepository.findAll(specification);

        assertEquals(expectedCount, result.size());
        if(expectedId != null){
            assertEquals(expectedId, result.getFirst().getId());
        }

    }
}
