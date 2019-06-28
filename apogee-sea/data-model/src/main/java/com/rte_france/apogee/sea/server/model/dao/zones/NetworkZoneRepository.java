// CHECKSTYLE:OFF
package com.rte_france.apogee.sea.server.model.dao.zones;

import com.rte_france.apogee.sea.server.model.user.Usertype;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import com.rte_france.apogee.sea.server.repo.ExtendedRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface NetworkZoneRepository extends ExtendedRepository<NetworkZone, String> {
    public Optional<NetworkZone> findByObjectid(String objectid);

    // Default java rules normally excludes underscore from method name, but JPA is an exception
    // CHECKSTYLE:OFF:MethodName
    public Set<NetworkZone> findByNetworkVoltageLevels_Objectid(String voltageLevel); //NOSONAR
    // CHECKSTYLE:ON:MethodName

    Set<NetworkZone> findByUsertypes(Usertype usertype);

    @Query(value = "SELECT DISTINCT z FROM NetworkZone z INNER JOIN z.networkVoltageLevels vls WHERE vls.objectid IN (:objectids)")
    Set<NetworkZone> findZonesByNetworkVoltageLevels(@Param("objectids") List<String> objectids);
}
