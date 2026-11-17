package com.administrapochi.repositories;

import com.administrapochi.models.entities.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseTypeRepository extends JpaRepository<ExpenseType, Integer> {

    boolean existsByCode(String code);

    boolean existsByName(String name);

    boolean existsByCodeAndIdNot(String code, Integer id);

    boolean existsByNameAndIdNot(String name, Integer id);

    List<ExpenseType> findByActive(Boolean active);
}
