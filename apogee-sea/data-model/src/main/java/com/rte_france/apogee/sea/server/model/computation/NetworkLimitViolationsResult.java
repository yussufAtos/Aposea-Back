package com.rte_france.apogee.sea.server.model.computation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.computation.variant.NetworkActionResult;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class NetworkLimitViolationsResult implements Serializable {

    @Id
    @SequenceGenerator(name = "NetworkLimitViolationsResultSeq", sequenceName = "NETWORK_LIMIT_VIOLATIONS_RESULT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "NetworkLimitViolationsResultSeq")
    private Long id;


    @Getter
    @Setter
    @JsonView({Views.Public.class})
    private boolean computationOk;

    /**
     * The list of limit violations for this state (N state or N-K (contingency) state)
     */
    @OneToMany(mappedBy = "networkLimitViolationsResult", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Getter
    @Setter
    @ToString.Exclude
    @JsonView({Views.PostContingencyResults.class, Views.BasecaseResults.class})
    private List<NetworkLimitViolation> networkLimitViolationList = new ArrayList<>();


    /**
     * Link to the Post Contingency Result for N-K (contingency) state.
     * Can be null for N state
     */
    @OneToOne(mappedBy = "networkLimitViolationsResult", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Getter
    @Setter
    @ToString.Exclude
    private NetworkPostContingencyResult networkPostContingencyResult;


    /**
     * Link to the Security Analysis result for N state.
     * Can be null for N-K (contingency) state.
     */
    @JsonIgnore
    @OneToOne(mappedBy = "preContingencyResult", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Getter
    @Setter
    private NetworkSecurityAnalysisResult networkSecurityAnalysisResultList;


    @OneToMany(mappedBy = "networkLimitViolationsResult", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @ToString.Exclude
    @Getter
    @Setter
    private List<NetworkActionResult> actionsResults = new ArrayList<>();


    public NetworkLimitViolationsResult(boolean computationOk, List<NetworkLimitViolation> networkLimitViolations) {
        this.computationOk = computationOk;
        this.networkLimitViolationList = Objects.requireNonNull(networkLimitViolations);
    }
}
