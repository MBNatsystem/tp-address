package fr.natsystem.tp_adresse_test.api.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fr.natsystem.tp_adresse_test.api.DTO.AddressDto;
import fr.natsystem.tp_adresse_test.api.Entity.Address;
import fr.natsystem.tp_adresse_test.api.Parameters.AddressParameters;
import fr.natsystem.tp_adresse_test.api.Repository.AddressRepository;
import fr.natsystem.tp_adresse_test.api.Specification.AddressSpecification;
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
        return addressRepository.findAll(AddressSpecification.globalSearch(codePostal, nomCommune, codeInsee, nomVoie), pageable)
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

        double RAYON = 1000;

        double delta_lat = RAYON / 111320;
        double minLat = lat - delta_lat;
        double maxLat = lat + delta_lat; 
        
        double delta_lon = RAYON / (111320 * Math.cos(Math.toRadians(lat)));
        double minLon = lon - delta_lon;
        double maxLon = lon + delta_lon;

        Address address = addressRepository.findNearestAddress(lat, minLat, maxLat, lon, minLon, maxLon);
        return addressMapper.toDto(address);
    }

    public List<AddressDto> getByAddressParam(String param) {
        AddressParameters params = parseParam(param);
        List<Address> addressPage = null;
        if(params.fts()==null || params.fts().trim().isEmpty()){
             addressPage = addressRepository.find(params.numero(), params.codePostal());
        }
        else{
            addressPage = addressRepository.findFts(params.numero(), params.codePostal(), params.fts());
        }
        
        List<AddressDto> addressList = addressPage
                .stream()
                .map(addressMapper::toDto)
                .toList();
        return addressList;

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
}
