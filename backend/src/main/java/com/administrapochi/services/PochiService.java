package com.administrapochi.services;

import com.administrapochi.dtos.pochis.CreatePochiRequest;
import com.administrapochi.dtos.pochis.PochiResponse;
import com.administrapochi.dtos.pochis.UpdatePochiRequest;

import java.util.List;

public interface PochiService {

    List<PochiResponse> findAll();

    PochiResponse findById(Integer id);

    PochiResponse create(CreatePochiRequest request);

    PochiResponse update(Integer id, UpdatePochiRequest request);

    PochiResponse activate(Integer id);

    PochiResponse deactivate(Integer id);
}
