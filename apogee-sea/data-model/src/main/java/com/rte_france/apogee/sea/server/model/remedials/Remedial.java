package com.rte_france.apogee.sea.server.model.remedials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.computation.NetworkPostContingencyResult;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class Remedial implements Serializable {

    @Id
    @Column(name = "ID_REMEDIAL_REPOSITORY")
    @NonNull
    @JsonView({Views.Public.class})
    private String idRemedialRepository;

    @JsonView({Views.Public.class})
    private String idLogicContext;

    @NonNull
    @JsonView({Views.Public.class})
    private String shortDescription;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "remedials")
    private List<NetworkPostContingencyResult> networkPostContingencyResults;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "allRemedials", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<NetworkContingency> networkContingencies;

    public Remedial(String idRemedialRepository, String shortDescription, List<NetworkPostContingencyResult> networkPostContingencyResults) {
        this.idRemedialRepository = idRemedialRepository;
        this.shortDescription = shortDescription;
        this.networkPostContingencyResults = Objects.requireNonNull(networkPostContingencyResults);
    }

    public Remedial(String idRemedialRepository, String shortDescription) {
        this(idRemedialRepository, shortDescription, new ArrayList<>());
    }
}


