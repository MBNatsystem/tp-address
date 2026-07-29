package fr.natsystem.tp_adresse_test.api.service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fr.natsystem.tp_adresse_test.api.dto.AddressDto;
import fr.natsystem.tp_adresse_test.api.dto.TarifCommuneResponse;
import fr.natsystem.tp_adresse_test.api.entity.Address;
import fr.natsystem.tp_adresse_test.api.parameters.AddressParameters;
import fr.natsystem.tp_adresse_test.api.repository.AddressRepository;
import fr.natsystem.tp_adresse_test.api.specification.AddressSpecification;
import fr.natsystem.tp_adresse_test.api.utils.AddressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressService {
    
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    public Page<AddressDto> getAllBySearchParam(String codePostal, String nomCommune, String codeInsee, String nomVoie, Pageable pageable) {
        return addressRepository.findAll(
            AddressSpecification.globalSearch(codePostal, nomCommune, codeInsee, nomVoie), 
            pageable
        )
        .map(addressMapper::toDto);
    }

    public AddressDto getAllByAddressParam(Integer numero, String nomVoie, String rep, String nomCommune, String codePostal) {
        Pageable pageable = PageRequest.of(0, 1, Sort.by("id").ascending());
        Page<Address> addressPage = addressRepository.findAll(AddressSpecification.addressSearch(numero, nomVoie, rep, nomCommune, codePostal), pageable);
        return addressPage
                .getContent()
                .stream()
                .findFirst()
                .map(addressMapper::toDto)
                .orElse(null);
    }

    public AddressDto getAddressByCoordinates(Double lat, Double lon) {

        double rayon = 100;
        Optional<Address> address = addressRepository.findNearestAddress(lon, lat, rayon);
        while(!address.isPresent()||rayon<=10000000){
            rayon*=4;
            address = addressRepository.findNearestAddress(lon, lat, rayon);
        }
        
        return addressMapper.toDto(address.get());
    }

    public List<AddressDto> getByAddressParam(String param) {

        AddressParameters params = parseParam(param);

        List<Address> addressPage = null;
        
        if(params.fts()==null || params.fts().trim().isEmpty()){
             addressPage = addressRepository.findNumberAndCodePostal(params.numero(), params.codePostal());
        }
        else{
            addressPage = addressRepository.findFts(params.numero(), params.codePostal(), params.fts());
        }
        
        return addressPage
                .stream()
                .map(addressMapper::toDto)
                .toList();
    }

    private AddressParameters parseParam(String param) {

        Pattern codePostalPattern = Pattern.compile("\\b\\d{5}\\b");
        Matcher codePostalMatcher = codePostalPattern.matcher(param);
        String codePostal = codePostalMatcher.find() ? codePostalMatcher.group():null;
        param = param.replaceFirst("\\b\\d{5}\\b", "");

        Pattern numeroPattern = Pattern.compile("\\d+");
        Matcher numeroMatcher = numeroPattern.matcher(param);
        Integer numero = numeroMatcher.find() ? Integer.parseInt(numeroMatcher.group()):null;
        param = param.replaceFirst("\\d+", "");

        param = normalize(param);

        return new AddressParameters(numero, codePostal, param);
    }

    private String normalize(String value){
        if(value==null || value.trim().isEmpty()){
            return "";
        }

        return value.trim().toUpperCase()+"*";
            
    }

    public TarifCommuneResponse getTarif(String codeInsee) {
        
        return addressRepository.getTarifByCodeInsee(codeInsee);
    }

    public String getCommunesGeoJson(String departement){
        return addressRepository.findAllAsGeoJson(departement);
    }
}
