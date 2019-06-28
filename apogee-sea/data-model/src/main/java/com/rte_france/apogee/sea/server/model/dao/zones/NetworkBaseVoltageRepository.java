package com.rte_france.apogee.sea.server.model.dao.zones;

import com.rte_france.apogee.sea.server.model.zones.NetworkBaseVoltage;
import com.rte_france.apogee.sea.server.repo.ExtendedRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NetworkBaseVoltageRepository extends ExtendedRepository<NetworkBaseVoltage, String> {
    public Optional<NetworkBaseVoltage> findByObjectid(String objectid);
}
