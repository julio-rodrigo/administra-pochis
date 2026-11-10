package com.administrapochi.mappers;

import com.administrapochi.dtos.drivers.DriverCreateRequest;
import com.administrapochi.dtos.drivers.DriverResponse;
import com.administrapochi.dtos.drivers.DriverUpdateRequest;
import com.administrapochi.models.Driver;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    @Mapping(target = "fullName", expression = "java(formatFullName(driver))")
    DriverResponse toResponse(Driver driver);

    List<DriverResponse> toResponseList(List<Driver> drivers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employmentStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Driver toEntity(DriverCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "curp", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(DriverUpdateRequest request, @MappingTarget Driver driver);

    default String formatFullName(Driver driver) {
        return Stream.of(driver.getFirstName(), driver.getFirstLastName(), driver.getSecondLastName())
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }
}
