package com.rte_france.apogee.sea.server.model.dao.zones;

import com.rte_france.apogee.sea.server.model.zones.NetworkVoltageLevel;
import com.rte_france.apogee.sea.server.repo.ExtendedRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NetworkVoltageLevelRepository extends ExtendedRepository<NetworkVoltageLevel, String> {
    Optional<NetworkVoltageLevel> findByObjectid(String objectid);
}
