package com.rte_france.apogee.sea.server.model.computation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Table(indexes = {@Index(name = "i_networkpostcontingencyresult_networksecurityanalysisresult", columnList = "computationresult_id"),
        @Index(name = "i_networkpostcontingencyresult_limitViolationsResult", columnList = "limitViolationsResult_id"),
        @Index(name = "i_networkpostcontingencyresult_networkcontingency", columnList = "networkcontingency_id")})
public class NetworkPostContingencyResult implements Serializable {

    @Id
    @Column(name = "ID")
    @SequenceGenerator(name = "NetworkPostContingencyResultSeq", sequenceName = "NETWORK_POST_CONTINGENCY_RESULT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "NetworkPostContingencyResultSeq")
    @Getter
    private Long id;

    /**
     * The contingency ID. This is null for N state violations.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "networkcontingency_id")
    @Getter
    @Setter
    @ToString.Exclude
    @JsonView({Views.PostContingencyResults.class, Views.ComputationResults.class})
    private NetworkContingency networkContingency;

    /**
     * The limit violation result for N-K (contingency) state.
     */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "limitViolationsResult_id")
    @NonNull
    @Getter
    @Setter
    @ToString.Exclude
    @JsonView({Views.PostContingencyResults.class})
    private NetworkLimitViolationsResult networkLimitViolationsResult;

    /**
     * Link to the Security Analysis result this Post Contingency Result applies to.
     */
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "computationresult_id")
    @Getter
    @Setter
    private NetworkSecurityAnalysisResult computationResult;


    /**
     * The list of remedials for this state (N-K (contingency) state)
     */
    @Getter
    @Setter
    @JsonView({Views.PostContingencyResults.class, Views.ComputationResults.class})
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "REMEDIALS_POST_CONTINGENCY_RESULT",
            joinColumns = @JoinColumn(name = "POST_CONTINGENCY_RESULT_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "REMEDIAL_ID", referencedColumnName = "ID_REMEDIAL_REPOSITORY"),
            indexes = {
                @Index(name = "idx_remedials_post_contingency_result", columnList = "POST_CONTINGENCY_RESULT_ID")
            })
    private List<Remedial> remedials;


    public NetworkPostContingencyResult(NetworkContingency contingency, NetworkLimitViolationsResult networkLimitViolationsResult) {
        this.networkContingency = Objects.requireNonNull(contingency);
        this.networkLimitViolationsResult = Objects.requireNonNull(networkLimitViolationsResult);

    }

    public NetworkPostContingencyResult(NetworkContingency contingency, boolean computationOk, List<NetworkLimitViolation> limitViolations) {
        this(contingency, new NetworkLimitViolationsResult(computationOk, limitViolations));
    }

}
