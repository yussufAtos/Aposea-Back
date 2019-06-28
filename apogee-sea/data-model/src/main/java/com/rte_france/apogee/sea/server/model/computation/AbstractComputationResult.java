package com.rte_france.apogee.sea.server.model.computation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;

//squid:S3437 -> Make this value-based field transient so it is not included in the serialization of this class.
// suppress false positive for Instant fields database serialization
@SuppressWarnings("squid:S3437")
@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class AbstractComputationResult implements Serializable {
    @Id
    @Getter
    @Setter
    @SequenceGenerator(name = "ComputationResultSeq", sequenceName = "COMPUTATION_RESULT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ComputationResultSeq")
    @JsonView({Views.ComputationResults.class, Views.NetworkContext.class})
    protected Long id;

    @NonNull
    @Getter
    @Setter
    @JsonView({Views.ComputationResults.class, Views.NetworkContext.class})
    protected Instant startDate;


    @Getter
    @Setter
    @JsonView({Views.ComputationResults.class, Views.NetworkContext.class})
    protected Instant endDate;

    @Getter
    @Setter
    @JsonView({Views.ComputationResults.class, Views.NetworkContext.class})
    protected String name;

    @Getter
    @Setter
    @JsonView({Views.UiSnapshot.class, Views.ComputationResults.class, Views.NetworkContext.class})
    protected String idAfsRunner;

    /**
     * The network context type, e.g. SRJ, SRMixte, PF
     */

    @Getter
    @Setter
    @Enumerated(value = EnumType.STRING)
    @JsonView({Views.ComputationResults.class, Views.NetworkContext.class})
    protected ExecStatus execStatus;

    @Getter
    @Setter
    @JsonView({ Views.UiSnapshot.class})
    protected boolean computationWithRemedials;

    /**
     * Link to the network context this computation result applies to.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "networkcontext_id")
    @NonNull
    @Getter
    @Setter
    protected NetworkContext networkContext;
}
