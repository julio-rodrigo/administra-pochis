package com.administrapochi.services;

import com.administrapochi.models.dtos.pochis.CreatePochiRequest;
import com.administrapochi.models.dtos.pochis.PochiResponse;
import com.administrapochi.models.dtos.pochis.UpdatePochiRequest;
import com.administrapochi.exceptions.DuplicateEconomicNumberException;
import com.administrapochi.exceptions.DuplicateLicensePlateException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.exceptions.UserNotFoundException;
import com.administrapochi.models.mappers.PochiMapper;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.User;
import com.administrapochi.repositories.PochiRepository;
import com.administrapochi.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PochiServiceImpl implements PochiService {

    private static final String DEFAULT_UNION_NAME = "UNTRAC";

    private final PochiRepository pochiRepository;
    private final PochiMapper pochiMapper;
    private final UserRepository userRepository;

    @Override
    public List<PochiResponse> findAll() {
        return pochiMapper.toResponseList(pochiRepository.findAll());
    }

    @Override
    public PochiResponse findById(Integer id) {
        return pochiMapper.toResponse(findPochi(id));
    }

    @Override
    @Transactional
    public PochiResponse create(CreatePochiRequest request) {
        String economicNumber = normalizeRequired(request.getEconomicNumber());
        String licensePlate = normalizeOptional(request.getLicensePlate());
        String unionName = normalizeOptional(request.getUnionName());

        validateUniqueValues(economicNumber, licensePlate);
        User owner = findOwner(request.getOwnerId());

        Pochi pochi = pochiMapper.toEntity(request);
        pochi.setEconomicNumber(economicNumber);
        pochi.setLicensePlate(licensePlate);
        pochi.setUnionName(unionName == null ? DEFAULT_UNION_NAME : unionName);
        pochi.setActive(true);
        pochi.setOwner(owner);

        return pochiMapper.toResponse(pochiRepository.save(pochi));
    }

    @Override
    @Transactional
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
        User owner = findOwner(request.getOwnerId());

        pochiMapper.updateEntity(request, pochi);
        pochi.setEconomicNumber(economicNumber);
        pochi.setLicensePlate(licensePlate);
        pochi.setOwner(owner);

        return pochiMapper.toResponse(pochiRepository.save(pochi));
    }

    @Override
    @Transactional
    public PochiResponse activate(Integer id) {
        Pochi pochi = findPochi(id);
        if (!Boolean.TRUE.equals(pochi.getActive())) {
            pochi.setActive(true);
            pochi = pochiRepository.save(pochi);
        }
        return pochiMapper.toResponse(pochi);
    }

    @Override
    @Transactional
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

    private User findOwner(Integer ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(ownerId));
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
