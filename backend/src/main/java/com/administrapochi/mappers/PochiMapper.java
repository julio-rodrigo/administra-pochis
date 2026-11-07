package com.administrapochi.mappers;

import com.administrapochi.dtos.pochis.CreatePochiRequest;
import com.administrapochi.dtos.pochis.PochiResponse;
import com.administrapochi.dtos.pochis.UpdatePochiRequest;
import com.administrapochi.models.Pochi;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PochiMapper {

    PochiResponse toResponse(Pochi pochi);

    List<PochiResponse> toResponseList(List<Pochi> pochis);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Pochi toEntity(CreatePochiRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "unionName", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdatePochiRequest request, @MappingTarget Pochi pochi);
}
