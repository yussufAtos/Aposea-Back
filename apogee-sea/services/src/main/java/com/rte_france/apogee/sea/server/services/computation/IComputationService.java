package com.rte_france.apogee.sea.server.services.computation;


import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.computation.logic.LimitViolationByIdenfifierAndRemedials;
import com.rte_france.apogee.sea.server.model.computation.logic.SnapshotResult;
import com.rte_france.apogee.sea.server.model.user.Usertype;

import java.util.List;

public interface IComputationService {
    void insertDataSetInUiSnapshot();

    List<NetworkContext> fetchLastNetworkContextsWithPriority();

    SnapshotResult getMapNetworkContextByContingency(String username, int page, int size, String snapshotid, List<String> zones, String timerange, boolean exclude) throws ComputationServiceException;

    LimitViolationByIdenfifierAndRemedials getLimitViolationsByContingency(Usertype actualUsertype, String contingencyId, String snapshotVersion, List<String> contextsId, boolean exclude) throws ComputationServiceException;

    class ComputationServiceException extends java.lang.Exception {
        public ComputationServiceException(String message, Throwable e) {
            super(message, e);
        }
    }
}
