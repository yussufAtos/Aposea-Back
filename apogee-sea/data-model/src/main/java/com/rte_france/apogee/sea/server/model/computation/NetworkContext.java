package com.rte_france.apogee.sea.server.model.computation;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

//squid:S3437 -> Make this value-based field transient so it is not included in the serialization of this class.
// suppress false positive for Instant fields database serialization
@SuppressWarnings("squid:S3437")
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"caseType_id ", "computationDate", "networkDate"}))
@ToString
@RequiredArgsConstructor()
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class NetworkContext implements Serializable {

    @Id
    @SequenceGenerator(name = "NetworkContextSeq", sequenceName = "NETWORK_CONTEXT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "NetworkContextSeq")
    @ToString.Exclude
    @Getter
    @JsonView({Views.Public.class})
    private Long id;

    /**
     * Link to the case type this network context applies to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caseType_id")
    @NonNull
    @Getter
    @Setter
    @JsonView({Views.Public.class})
    protected CaseType caseType;

    /**
     * The network computation date.
     * For snapshots this will be equal to networkDate.
     * For forecasts this will be earlier than networkDate.
     */
    @NonNull
    @Getter
    @JsonView({Views.Public.class})
    private Instant computationDate;

    /**
     * The network date.
     */
    @NonNull
    @Getter
    @JsonView({Views.Public.class})
    private Instant networkDate;

    /**
     * The AFS node ID of the ImportedCase.
     */
    @NonNull
    @Getter
    @JsonView({Views.Public.class})
    private String idAfsImportedCase;

    /**
     * The date at which the network situation was inserted into / seen by apogee-sea.
     * (after computationDate due to transmission delays)
     */
    @NonNull
    @Getter
    @JsonView({Views.Public.class})
    private Instant insertionDate;


    /**
     * The list of computation results.
     */
    @OneToMany(mappedBy = "networkContext", cascade = {CascadeType.ALL}, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Getter
    @Setter
    @JsonView({Views.ComputationResults.class, Views.NetworkContext.class, Views.UiSnapshot.class})
    private List<AbstractComputationResult> computationResultList;

    public NetworkContext(CaseType caseType, Instant computationDate, Instant networkDate, String idAfsImportedCase, Instant insertionDate, List<AbstractComputationResult> computationResultList) {
        this.caseType = caseType;
        this.computationDate = computationDate;
        this.networkDate = networkDate;
        this.idAfsImportedCase = idAfsImportedCase;
        this.insertionDate = insertionDate;
        this.computationResultList = Objects.requireNonNull(computationResultList);
    }
}
