package com.rte_france.apogee.sea.server.model.dao.computation;

import com.google.common.collect.Iterables;
import com.rte_france.apogee.sea.server.model.computation.CaseCategory;
import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.SnapshotRepository;
import com.rte_france.apogee.sea.server.model.timerange.TimerangeFilterDate;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@Getter
public class NetworkRepository {

    private CaseTypeRepository caseTypeRepository;

    private NetworkContextRepository networkContextRepository;

    private ComputationResultRepository computationResultRepository;

    private CaseCategoryRepository caseCategoryRepository;

    private SnapshotRepository snapshotRepository;

    @Autowired
    public NetworkRepository(CaseTypeRepository caseTypeRepository, NetworkContextRepository networkContextRepository,
                             ComputationResultRepository computationResultRepository, CaseCategoryRepository caseCategoryRepository, SnapshotRepository snapshotRepository) {
        this.caseTypeRepository = caseTypeRepository;
        this.networkContextRepository = networkContextRepository;
        this.computationResultRepository = computationResultRepository;
        this.caseCategoryRepository = caseCategoryRepository;
        this.snapshotRepository = snapshotRepository;

    }

    public List<NetworkContext> fetchLastNetworkContextsWithPriority() {
        final List<NetworkContext> networkContexts = new ArrayList<>();
        Set<Instant> instants = new HashSet<>();
        caseCategoryRepository.findByDisplayPriorityGreaterThanOrderByDisplayPriorityAsc(0).forEach(caseCategory ->
                caseTypeRepository.findByCaseCategoryAndEnabledTrue(caseCategory).forEach(caseType -> {
                    final List<NetworkContext> networkContextList = new ArrayList<>();

                    networkContextRepository.findLatestByCaseType(caseType).forEach(networkContext -> {

                        if (!instants.contains(networkContext.getNetworkDate())
                                // do not include the network context at the secondary caseCategory priority between others having the first caseCategory priority
                                && (networkContexts.isEmpty() || Iterables.getLast(networkContexts).getNetworkDate().isBefore(networkContext.getNetworkDate()))) {

                            instants.add(networkContext.getNetworkDate());
                            networkContextList.add(networkContext);
                        }

                    });
                    // treat display limit defined by unit of minutes
                    networkContextList.sort(Comparator.comparing(NetworkContext::getNetworkDate));

                    networkContexts.addAll(filterNetworkContextsByDisplayLimit(networkContextList, caseCategory));
                })
        );
        return networkContexts;
    }

    private Collection<NetworkContext> filterNetworkContextsByDisplayLimit(List<NetworkContext> networkContextList, CaseCategory caseCategory) {
        List<NetworkContext> result = new ArrayList<>();

        if (caseCategory.getDisplayLimit() == null) {
            return networkContextList;
        }

        Instant now = Instant.now();

        networkContextList.forEach(networkContext -> {
            if (ChronoUnit.MINUTES.between(networkContext.getNetworkDate(), now) <= caseCategory.getDisplayLimit()) {
                result.add(networkContext);
            }
        });

        return result;
    }


    public List<NetworkContext> fetchLastNetworkContextsWithPriorityByUiSnapshot(Long uiSnapshotId, @NonNull TimerangeFilterDate timerangeFilterDate) {
        if (timerangeFilterDate.getStartDate() != null && timerangeFilterDate.getEndDate() != null) {
            return snapshotRepository.getUiSnapshotContextRepository().findLatestContextByUiSnapshotIdAndFilterByTimerange(uiSnapshotId, timerangeFilterDate.getStartDate(), timerangeFilterDate.getEndDate());
        }
        return snapshotRepository.getUiSnapshotContextRepository().findLatestContextByUiSnapshotId(uiSnapshotId);
    }


}
