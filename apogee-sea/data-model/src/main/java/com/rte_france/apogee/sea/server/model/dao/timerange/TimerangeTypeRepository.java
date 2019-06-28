package com.rte_france.apogee.sea.server.model.dao.timerange;

import com.rte_france.apogee.sea.server.model.timerange.TimerangeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimerangeTypeRepository extends JpaRepository<TimerangeType, String> {

    List<TimerangeType> findByOpfabEnabledTrue();
}


