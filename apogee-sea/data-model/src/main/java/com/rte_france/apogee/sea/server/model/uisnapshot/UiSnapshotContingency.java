package com.rte_france.apogee.sea.server.model.uisnapshot;

import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.computation.NetworkLimitViolation;
import com.rte_france.apogee.sea.server.model.zones.NetworkVoltageLevel;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Entity
@NoArgsConstructor
@Table(indexes = {@Index(name = "i_uisnapshotcontingency_uisnapshot", columnList = "uiSnapshot_id"),
        @Index(name = "i_uisnapshotcontingency_networkcontingency", columnList = "networkContingency_id")})
public class UiSnapshotContingency implements Serializable {

    @Id
    @SequenceGenerator(name = "UiSnapshotContingencySeq", sequenceName = "UI_SNAPSHOT_CONTINGENCY_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "UiSnapshotContingencySeq")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "uiSnapshot_id")
    private UiSnapshot uiSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "networkContingency_id")
    @OrderBy
    private NetworkContingency networkContingency;

    @OneToMany(mappedBy = "uiSnapshotContingency", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<UiSnapshotContingencyContext> uiSnapshotContingencyContextList;

    /**
     * Set of NetworkZone impacted by the contingency, obtained from iTesla
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(
            name = "ui_snapshot_contingency_zone",
            joinColumns = {@JoinColumn(name = "uisnapshotcontingency_id")},
            inverseJoinColumns = {@JoinColumn(name = "networkzone_id")})
    @Getter
    @Setter
    private Set<NetworkZone> networkZones = new HashSet<>();


    public UiSnapshotContingency(NetworkContingency networkContingency) {
        this.networkContingency = networkContingency;
        if (this.uiSnapshotContingencyContextList == null) {
            this.uiSnapshotContingencyContextList = new ArrayList<>();
        }
    }

    public List<NetworkVoltageLevel> fetchVoltageLevelsFromLimitViolations() {
        return this.getNetworkContingency().getPostContingencyResults().get(0).getNetworkLimitViolationsResult().getNetworkLimitViolationList()
                .stream()
                .map(NetworkLimitViolation::getNetworkVoltageLevels)
                .flatMap(Set::stream)
                .collect(Collectors.toList());
    }
}
