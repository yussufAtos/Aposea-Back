package com.rte_france.apogee.sea.server.model.computation;


import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Table(indexes = {@Index(name = "i_networksecurityanalysisresult_limitViolationsResult", columnList = "limitViolationsResult_id"),
        @Index(name = "i_computationresult_networkcontext", columnList = "networkcontext_id")})
public class NetworkSecurityAnalysisResult extends AbstractComputationResult {

    /**
     * The preContingencyResult. (for N-K (contingency) state the list limitviolation is nul).
     */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "limitViolationsResult_id")
    @ToString.Exclude
    @Getter
    @Setter
    @NonNull
    @JsonView({Views.BasecaseResults.class})
    private NetworkLimitViolationsResult preContingencyResult;

    /**
     * The list of postContingencyResults for N-K (contingency) state.
     */
    @OneToMany(mappedBy = "computationResult", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @ToString.Exclude
    @Getter
    @Setter
    @JsonView({Views.PostContingencyResults.class})
    private List<NetworkPostContingencyResult> postContingencyResults = new ArrayList<>();


    public NetworkSecurityAnalysisResult(NetworkLimitViolationsResult preContingencyResult,
                                         List<NetworkPostContingencyResult> postContingencyResults, NetworkContext networkContext) {
        this.preContingencyResult = preContingencyResult;
        this.postContingencyResults = Objects.requireNonNull(postContingencyResults);
        this.networkContext = Objects.requireNonNull(networkContext);
    }

    public NetworkSecurityAnalysisResult(List<NetworkPostContingencyResult> postContingencyResults, NetworkContext networkContext) {
        this.preContingencyResult = new NetworkLimitViolationsResult(true, new ArrayList<>());
        this.postContingencyResults = Objects.requireNonNull(postContingencyResults);
        this.networkContext = Objects.requireNonNull(networkContext);
    }

    public NetworkSecurityAnalysisResult(NetworkLimitViolationsResult preContingencyResult, NetworkContext networkContext) {
        this(preContingencyResult, new ArrayList<>(), networkContext);
    }

}

