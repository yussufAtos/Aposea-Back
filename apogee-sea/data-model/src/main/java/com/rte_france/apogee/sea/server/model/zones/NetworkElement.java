package com.rte_france.apogee.sea.server.model.zones;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * temporary storage until data is provided by iTesla in the security analysis results
 * TODO: remove me when iTesla provides the data
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(callSuper = true)
public class NetworkElement extends AbstractNetworkObject implements Serializable {

    /**
     * Set of NetworkVoltageLevel the NetworkElement is connected to (user definition)
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "network_element_voltagelevel",
            joinColumns = {@JoinColumn(name = "element_id")},
            inverseJoinColumns = {@JoinColumn(name = "voltagelevel_id")})
    @Getter
    @Setter
    private Set<NetworkVoltageLevel> networkVoltageLevels = new HashSet<>();

    /**
     * constructor
     *
     * @param objectid:
     */
    @Builder
    public NetworkElement(String objectid, String name, Set<NetworkVoltageLevel> networkVoltageLevels) {
        this.objectid = objectid;
        this.name = name;
        this.networkVoltageLevels = new HashSet<>(networkVoltageLevels);
    }
}
