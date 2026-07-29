package fr.natsystem.tp_adresse_test.api.utils;

import org.mapstruct.Mapper;

import fr.natsystem.tp_adresse_test.api.dto.AddressDto;
import fr.natsystem.tp_adresse_test.api.entity.Address;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    AddressDto toDto(Address address);
    Address toEntity(AddressDto addressDto);
}
