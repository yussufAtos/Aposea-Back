package com.rte_france.apogee.sea.server.model.computation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.powsybl.contingency.ContingencyElementType;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;


@Entity
@ToString
@RequiredArgsConstructor()
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Table(indexes = {@Index(name = "i_networkContingency", columnList = "networkContingency_id")})
@Setter
@Getter
public class NetworkContingencyElement implements Serializable {

    @Id
    @SequenceGenerator(name = "NetworkContingencyElementSeq", sequenceName = "NETWORK_CONTINGENCY_ELEMENT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "NetworkContingencyElementSeq")
    @JsonView({Views.PostContingencyResults.class, Views.ComputationResults.class})
    private Long id;

    @NonNull
    @JsonView({Views.PostContingencyResults.class, Views.ComputationResults.class, Views.UiSnapshot.class})
    String equipmentName;

    @Enumerated(value = EnumType.STRING)
    @NonNull
    @JsonView({Views.PostContingencyResults.class, Views.ComputationResults.class})
    ContingencyElementType contingencyElementType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "networkContingency_id")
    @NonNull
    @JsonIgnore
    private NetworkContingency networkContingency;

}

