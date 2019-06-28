package com.rte_france.apogee.sea.server.model.zones;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.Entity;
import java.io.Serializable;

/**
 * Defines a system base voltage which is referenced.
 */
@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EqualsAndHashCode(callSuper = true)
public class NetworkBaseVoltage extends AbstractNetworkObject implements Serializable {

    /**
     * The power system resource's base voltage.
     */
    @NonNull
    @Getter
    @JsonView({Views.BaseVoltage.class})
    private double nominalVoltage;

    @Builder
    public NetworkBaseVoltage(String objectid, String name, double nominalVoltage) {
        this.objectid = objectid;
        this.name = name;
        this.nominalVoltage = nominalVoltage;
    }
}
