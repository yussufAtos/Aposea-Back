package com.rte_france.apogee.sea.server.model.dao.remedials;

import com.rte_france.apogee.sea.server.model.remedials.Prioritize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrioritizeRepository extends JpaRepository<Prioritize, Long> {

    @Query(value = "SELECT p FROM Prioritize p WHERE p.networkContingency.id =:contingencyId and p.prioritizeStartDate =:startDate and (p.prioritizeEndDate IS NULL OR p.prioritizeEndDate =:endDate)")
    Optional<Prioritize> findByContingencyAndStartDateAndEndDate(@Param("contingencyId") String contingencyId,
                                                                 @Param("startDate") Instant startDate,
                                                                 @Param("endDate") Instant endDate);

    @Query(value = "SELECT p FROM Prioritize p WHERE p.networkContingency.id =:contingencyId")
    List<Prioritize> findByNetworkContingency(@Param("contingencyId") String contingencyId);

    @Query(value = "SELECT p FROM Prioritize p WHERE p.networkContingency.id=:contingencyId AND p.prioritizeStartDate<=:date " +
            " AND (p.prioritizeEndDate=null OR p.prioritizeEndDate>:date)")
    Optional<Prioritize> findLastPrioritizeRemedialByContingencyAndDate(@Param("date") Instant date, @Param("contingencyId") String contingencyId);


    @Query(value = "SELECT p FROM Prioritize p WHERE p.networkContingency.id=:contingencyId")
    List<Prioritize> findLastPrioritizeRemedialByContingency(@Param("contingencyId") String contingencyId);


    @Query(value = "SELECT p FROM Prioritize p WHERE p.prioritizeStartDate<=:date AND (p.prioritizeEndDate=null" +
            " OR p.prioritizeEndDate>:date)")
    List<Prioritize> findLastPrioritizeRemedial(@Param("date") Instant date);

}
