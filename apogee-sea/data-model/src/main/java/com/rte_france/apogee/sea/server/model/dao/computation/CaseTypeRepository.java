package com.rte_france.apogee.sea.server.model.dao.computation;

import com.rte_france.apogee.sea.server.model.computation.CaseCategory;
import com.rte_france.apogee.sea.server.model.computation.CaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CaseTypeRepository extends JpaRepository<CaseType, String> {

    List<CaseType> findByCaseCategoryAndEnabledTrue(CaseCategory caseCategory);

    // CHECKSTYLE:OFF:MethodName
    List<CaseType> findByEnabledTrueAndOpfabEnabledTrueOrderByCaseCategory_DisplayPriority();
}

