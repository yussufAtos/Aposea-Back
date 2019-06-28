package com.rte_france.apogee.sea.server.model.zones;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshotContingency;
import com.rte_france.apogee.sea.server.model.user.Usertype;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(callSuper = true)
public class NetworkZone extends AbstractNetworkObject implements Serializable {

    /**
     * Set of NetworkVoltageLevel in the NetworkZone (user definition)
     */
    @JsonView({Views.NetworkZone.class})
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "network_zone_voltagelevel", joinColumns = @JoinColumn(name = "zone_id", referencedColumnName = "uid"),
            inverseJoinColumns = @JoinColumn(name = "voltagelevel_id", referencedColumnName = "uid"))
    @Getter
    @Setter
    @OrderBy
    @EqualsAndHashCode.Exclude
    private Set<NetworkVoltageLevel> networkVoltageLevels = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usertypes_network_zones",
            joinColumns = @JoinColumn(name = "network_zone_id",
                    referencedColumnName = "uid"),
            inverseJoinColumns = @JoinColumn(name = "usertype_name", referencedColumnName = "name"))
    @OrderBy
    @Setter
    @Getter
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private Set<Usertype> usertypes = new HashSet<>();


    @ManyToMany(mappedBy = "networkZones")
    @EqualsAndHashCode.Exclude
    private List<UiSnapshotContingency> uiSnapshotContingencies;

    /**
     * constructor
     *
     * @param objectid:
     */
    @Builder
    public NetworkZone(String objectid, String name, Set<NetworkVoltageLevel> networkVoltageLevels) {
        this.objectid = objectid;
        this.name = name;
        this.networkVoltageLevels = new HashSet<>(networkVoltageLevels);
    }


    /**
     * Add a Set of NetworkVoltageLevels to the NetworkZone
     *
     * @param networkVoltageLevels the NetworkVoltageLevel to be added
     * @return True if this NetworkZone changed as a result of the call
     */
    public boolean addVoltageLevels(Set<NetworkVoltageLevel> networkVoltageLevels) {
        return this.networkVoltageLevels.addAll(networkVoltageLevels);
    }

    /**
     * Removes a set of NetworkVoltageLevels from the NetworkZone
     *
     * @param networkVoltageLevels the NetworkVoltageLevel to be removed
     * @return True if this NetworkZone changed as a result of the call
     */
    public boolean removeVoltageLevels(Set<NetworkVoltageLevel> networkVoltageLevels) {
        return this.networkVoltageLevels.removeAll(networkVoltageLevels);
    }

}
