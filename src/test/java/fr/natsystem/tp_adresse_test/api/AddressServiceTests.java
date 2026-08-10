package fr.natsystem.tp_adresse_test.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.natsystem.tp_adresse_test.api.dto.TarifCommuneResponse;
import fr.natsystem.tp_adresse_test.api.repository.AddressRepository;
import fr.natsystem.tp_adresse_test.api.service.AddressService;
import fr.natsystem.tp_adresse_test.api.utils.AddressMapper;

@ExtendWith(MockitoExtension.class)
class AddressServiceTests {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressService addressService;

    @Test
    void getTarif_shouldCallRepository(){

        //Given
        String codeInsee = "79001";
        BigDecimal tarif = new BigDecimal(1);

        TarifCommuneResponse expected = new TarifCommuneResponse(codeInsee, tarif, tarif, tarif, tarif, tarif, tarif, 1L, 1L, tarif);

        when(addressRepository.getTarifByCodeInsee(codeInsee)).thenReturn(expected);

        //When
        TarifCommuneResponse result = addressService.getTarif(codeInsee);

        //Then
        assertEquals(expected, result);
        verify(addressRepository).getTarifByCodeInsee(codeInsee);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/api/address-service-test.csv", delimiter = ',', numLinesToSkip = 1, nullValues = "NULL")
    void getByAddressParam_should(
        String parameters,
        Integer numero,
        String codePostal,
        String fts

    ){
        //Given

        //When
        addressService.getByAddressParam(parameters);

        //Then
        if(fts==null){
            verify(addressRepository).findNumberAndCodePostal(numero, codePostal);
        }
        else{
            verify(addressRepository).findFts(numero, codePostal, fts);
        }
        
    }
}
