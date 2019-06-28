package com.rte_france.apogee.sea.server.model.dao.computation;

import com.rte_france.apogee.sea.server.model.computation.CaseType;
import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkContextRepository extends JpaRepository<NetworkContext, Long> {

    @Query(value = "SELECT n FROM NetworkContext n WHERE n.caseType.name =:type and n.computationDate =:computationDate and n.networkDate =:networkDate")
    Optional<NetworkContext> findByCaseTypeAndComputationDateAndNetworkDate(@Param("type") String caseTypeName,
                                                                            @Param("computationDate") Instant computationDate,
                                                                            @Param("networkDate") Instant networkDate);

    Optional<NetworkContext> findByIdAfsImportedCase(String idAfsImportedCase);

    @Query(value = "SELECT n FROM NetworkContext n WHERE n.caseType.name =:type")
    List<NetworkContext> findByCaseType(@Param("type") String caseTypeName);


    @Query(value = "SELECT n0.* " +
            " FROM Network_Context n0 " +
            " INNER JOIN (SELECT n1.case_Type_id, n1.network_Date, max(n1.computation_Date) as computation_Date " +
            "  FROM Network_Context n1 " +
            "  GROUP BY n1.case_Type_id, n1.network_Date) AS n2 " +
            " ON n0.case_Type_id = n2.case_Type_id AND n0.network_Date = n2.network_Date " +
            " AND n0.computation_Date = n2.computation_Date ", nativeQuery = true)
    List<NetworkContext> findLatestNetworkContexts();

    @Query(value = "SELECT n0.* " +
            " FROM (SELECT nc0.* FROM Network_Context nc0 JOIN network_security_analysis_result nsar0 ON nsar0.networkcontext_id = nc0.id WHERE nsar0.exec_status='COMPLETED' OR  nsar0.exec_status='FAILED') as n0" +
            " INNER JOIN (SELECT n1.network_Date, max(n1.computation_Date) as computation_Date " +
            "  FROM (SELECT nc1.* FROM Network_Context nc1 JOIN network_security_analysis_result nsar1 ON nsar1.networkcontext_id = nc1.id WHERE nsar1.exec_status='COMPLETED' OR nsar1.exec_status='FAILED') as n1" +
            "  WHERE n1.case_Type_id = :type " +
            "  GROUP BY n1.network_Date) AS n2 " +
            " ON n0.network_Date = n2.network_Date AND n0.computation_Date = n2.computation_Date " +
            " WHERE n0.case_Type_id = :type", nativeQuery = true)
    List<NetworkContext> findLatestByCaseType(@Param("type") String networkContextType);

    default List<NetworkContext> findLatestByCaseType(CaseType caseType) {
        return findLatestByCaseType(caseType.getName());
    }

    @Transactional
    @Modifying
    @Query(value = "select n FROM NetworkContext n WHERE n.networkDate <:instant ")
    List<NetworkContext> findNetworkContextWithNetworkDateBeforeThan(@Param("instant") Instant instant);


    @Query(value = "SELECT n0.* " +
            " FROM Network_Context n0 " +
            " INNER JOIN ( " +
            "  SELECT * FROM " +
            "   (SELECT n1.case_Type_id, n1.network_Date, n1.computation_Date, " +
            "    rank() OVER (PARTITION BY n1.case_Type_id, n1.network_Date ORDER BY n1.computation_Date DESC) AS pos " +
            "     FROM Network_Context n1 " +
            "   ) AS aaa " +
            "  WHERE pos > :maxNumVersions ) as n2 " +
            " ON n0.case_Type_id = n2.case_Type_id AND n0.network_Date = n2.network_Date " +
            " AND n0.computation_Date = n2.computation_Date ", nativeQuery = true)
    List<NetworkContext> findOldestNetworkContextsWithmaxNumVersions(@Param("maxNumVersions") int maxNumVersions);

    @Query(value = "SELECT n FROM NetworkContext n ORDER BY n.caseType.name, n.networkDate, n.computationDate DESC")
    List<NetworkContext> findAllOrderByCaseTypeAndOrderByNetworkDateAndOrderByComputationDateDesc();




}
