package com.rte_france.apogee.sea.server.model.remedials;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

//squid:S3437 -> Make this value-based field transient so it is not included in the serialization of this class.
// suppress false positive for Instant fields database serialization
@SuppressWarnings("squid:S3437")

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"network_contingency_id ", "prioritizeStartDate", "prioritizeEndDate"}),
        indexes = {@Index(name = "i_prioritize_networkContingency", columnList = "network_contingency_id")})
@RequiredArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Prioritize implements Serializable {
    @Id
    @SequenceGenerator(name = "PrioritizeSeq", sequenceName = "PRIORITIZE_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PrioritizeSeq")
    @ToString.Exclude
    @Getter
    Long id;

    @NonNull
    @JsonView({Views.Public.class})
    private Instant prioritizeStartDate;

    @JsonView({Views.Public.class})
    private Instant prioritizeEndDate;

    @JsonView({Views.Prioritize.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_contingency_id")
    private NetworkContingency networkContingency;

    @OneToMany(mappedBy = "prioritize", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonView({Views.Prioritize.class})
    @OrderBy(value = "prioritizeValue")
    private List<PrioritizeRemedial> prioritizeRemedialList;
}
