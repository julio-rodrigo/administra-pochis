package com.administrapochi.services;

import com.administrapochi.dtos.pochis.CreatePochiRequest;
import com.administrapochi.dtos.pochis.PochiResponse;
import com.administrapochi.dtos.pochis.UpdatePochiRequest;
import com.administrapochi.exceptions.DuplicateEconomicNumberException;
import com.administrapochi.exceptions.DuplicateLicensePlateException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.mappers.PochiMapper;
import com.administrapochi.models.Pochi;
import com.administrapochi.repositories.PochiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PochiServiceImpl implements PochiService {

    private static final String DEFAULT_UNION_NAME = "UNTRAC";

    private final PochiRepository pochiRepository;
    private final PochiMapper pochiMapper;

    @Override
    public List<PochiResponse> findAll() {
        return pochiMapper.toResponseList(pochiRepository.findAll());
    }

    @Override
    public PochiResponse findById(Integer id) {
        return pochiMapper.toResponse(findPochi(id));
    }

    @Override
    public PochiResponse create(CreatePochiRequest request) {
        String economicNumber = normalizeRequired(request.getEconomicNumber());
        String licensePlate = normalizeOptional(request.getLicensePlate());
        String unionName = normalizeOptional(request.getUnionName());

        validateUniqueValues(economicNumber, licensePlate);

        Pochi pochi = pochiMapper.toEntity(request);
        pochi.setEconomicNumber(economicNumber);
        pochi.setLicensePlate(licensePlate);
        pochi.setUnionName(unionName == null ? DEFAULT_UNION_NAME : unionName);
        pochi.setActive(true);

        return pochiMapper.toResponse(pochiRepository.save(pochi));
    }

    @Override
    public PochiResponse update(Integer id, UpdatePochiRequest request) {
        Pochi pochi = findPochi(id);
        String economicNumber = normalizeRequired(request.getEconomicNumber());
        String licensePlate = normalizeOptional(request.getLicensePlate());

        if (pochiRepository.existsByEconomicNumberAndIdNot(economicNumber, id)) {
            throw new DuplicateEconomicNumberException(economicNumber);
        }
        if (licensePlate != null && pochiRepository.existsByLicensePlateAndIdNot(licensePlate, id)) {
            throw new DuplicateLicensePlateException(licensePlate);
        }

        pochiMapper.updateEntity(request, pochi);
        pochi.setEconomicNumber(economicNumber);
        pochi.setLicensePlate(licensePlate);

        return pochiMapper.toResponse(pochiRepository.save(pochi));
    }

    @Override
    public PochiResponse activate(Integer id) {
        Pochi pochi = findPochi(id);
        if (!Boolean.TRUE.equals(pochi.getActive())) {
            pochi.setActive(true);
            pochi = pochiRepository.save(pochi);
        }
        return pochiMapper.toResponse(pochi);
    }

    @Override
    public PochiResponse deactivate(Integer id) {
        Pochi pochi = findPochi(id);
        if (!Boolean.FALSE.equals(pochi.getActive())) {
            pochi.setActive(false);
            pochi = pochiRepository.save(pochi);
        }
        return pochiMapper.toResponse(pochi);
    }

    private void validateUniqueValues(String economicNumber, String licensePlate) {
        if (pochiRepository.existsByEconomicNumber(economicNumber)) {
            throw new DuplicateEconomicNumberException(economicNumber);
        }
        if (licensePlate != null && pochiRepository.existsByLicensePlate(licensePlate)) {
            throw new DuplicateLicensePlateException(licensePlate);
        }
    }

    private Pochi findPochi(Integer id) {
        return pochiRepository.findById(id)
                .orElseThrow(() -> new PochiNotFoundException(id));
    }

    private String normalizeRequired(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
