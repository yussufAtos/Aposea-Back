package com.rte_france.apogee.sea.server.model.zones;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A collection of equipment at one common system voltage forming a switchgear.
 * The equipment typically consist of breakers, busbars, instrumentation, control, regulation and protection devices as well as assemblies of all these.
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(callSuper = true)
public class NetworkVoltageLevel extends AbstractNetworkObject implements Serializable {

    /**
     * The base voltage used for all equipment within the voltage level.
     */
    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "basevoltage")
    @JsonView({Views.NetworkVoltageLevel.class})
    private NetworkBaseVoltage baseVoltage;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "network_zone_voltagelevel", joinColumns = @JoinColumn(name = "voltagelevel_id", referencedColumnName = "uid"),
            inverseJoinColumns = @JoinColumn(name = "zone_id", referencedColumnName = "uid"))
    @Setter
    @Getter
    @OrderBy
    @EqualsAndHashCode.Exclude
    private Set<NetworkZone> networkZones = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "network_contingency_voltagelevel",
            joinColumns = @JoinColumn(name = "voltagelevel_id", referencedColumnName = "uid"),
            inverseJoinColumns = @JoinColumn(name = "networkcontingency_id", referencedColumnName = "id"))
    @Getter
    @Setter
    @JsonIgnore
    @OrderBy
    @EqualsAndHashCode.Exclude
    private Set<NetworkContingency> networkContingencies = new HashSet<>();

    /**
     * Constructor
     *
     * @param objectid:
     * @param name:
     * @param baseVoltage:
     */
    @Builder(toBuilder = true)
    public NetworkVoltageLevel(String objectid, String name, NetworkBaseVoltage baseVoltage) {
        this.objectid = objectid;
        this.name = name;
        this.baseVoltage = baseVoltage;
    }
}
