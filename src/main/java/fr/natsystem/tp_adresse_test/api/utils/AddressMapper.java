package fr.natsystem.tp_adresse_test.api.utils;

import org.mapstruct.Mapper;

import fr.natsystem.tp_adresse_test.api.DTO.AddressDto;
import fr.natsystem.tp_adresse_test.api.Entity.Address;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    AddressDto toDto(Address address);
    Address toEntity(AddressDto addressDto);
}
