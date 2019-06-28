package com.rte_france.apogee.sea.server.model.dao.computation;

import com.rte_france.apogee.sea.server.model.computation.CaseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CaseCategoryRepository extends JpaRepository<CaseCategory, String> {

    List<CaseCategory> findByDisplayPriorityGreaterThanOrderByDisplayPriorityAsc(Integer value);
}
