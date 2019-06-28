package com.rte_france.apogee.sea.server.model.computation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import com.rte_france.apogee.sea.server.model.zones.NetworkVoltageLevel;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

@Entity
@ToString
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode
public class NetworkContingency implements Serializable {

    @Id
    @NonNull
    @Getter
    @Setter
    @JsonView({Views.PostContingencyResults.class, Views.ComputationResults.class, Views.Prioritize.class, Views.UiSnapshot.class})
    private String id;


    @OneToMany(mappedBy = "networkContingency", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Getter
    @Setter
    @JsonView({Views.PostContingencyResults.class, Views.ComputationResults.class, Views.UiSnapshot.class})
    private List<NetworkContingencyElement> networkContingencyElementList;

    /**
     * Set of NetworkVoltageLevel impacted by the contingency, obtained from iTesla
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "network_contingency_voltagelevel",
            joinColumns = @JoinColumn(name = "networkcontingency_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "voltagelevel_id", referencedColumnName = "uid"))
    @Getter
    @Setter
    @OrderBy
    @EqualsAndHashCode.Exclude
    @JsonView(Views.UiSnapshot.class)
    private Set<NetworkVoltageLevel> networkVoltageLevels = new HashSet<>();

    /**
     * The list of limit violations for N-K (contingency) state
     */
    @OneToMany(mappedBy = "networkContingency", fetch = FetchType.LAZY)
    @ToString.Exclude
    @Getter
    @Setter
    @EqualsAndHashCode.Exclude
    private List<NetworkPostContingencyResult> postContingencyResults;


    /**
     * The list of remedials views)
     */
    @Getter
    @Setter
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "REMEDIALS_CONTINGENCY_ALL",
            joinColumns = @JoinColumn(name = "CONTINGENCY_VIEWS_ID", referencedColumnName = "ID"),
            inverseJoinColumns = @JoinColumn(name = "REMEDIAL_ID", referencedColumnName = "ID_REMEDIAL_REPOSITORY"),
            indexes = {
                    @Index(name = "idx_remedials_contingency_all", columnList = "CONTINGENCY_VIEWS_ID")
            })
    private Set<Remedial> allRemedials = new HashSet<>();


    public NetworkContingency(String id, List<NetworkContingencyElement> elements, List<NetworkPostContingencyResult> postContingencyResults) {
        this.id = Objects.requireNonNull(id);
        this.networkContingencyElementList = Objects.requireNonNull(elements);
        this.postContingencyResults = Objects.requireNonNull(postContingencyResults);
    }


    public NetworkContingency(String id, NetworkContingencyElement... elements) {
        this(id, Arrays.asList(elements), new ArrayList<>());
    }

    public NetworkContingency(String id, List<NetworkContingencyElement> elements) {
        this(id, elements, new ArrayList<>());
    }


    public NetworkContingency(String id) {
        this(id, new ArrayList<>(), new ArrayList<>());
    }

}

