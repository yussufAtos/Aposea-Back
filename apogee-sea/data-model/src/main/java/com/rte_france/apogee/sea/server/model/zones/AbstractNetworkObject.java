package com.rte_france.apogee.sea.server.model.zones;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

/**
 * This is a root class to provide common identification for all classes needing identification and naming attributes.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"objectid"}))
@EqualsAndHashCode
public abstract class AbstractNetworkObject implements Serializable {

    @Id
    @JsonIgnore
    @Getter
    @Setter
    @SequenceGenerator(name = "NetworkObjectSeq", sequenceName = "NETWORK_OBJECT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "NetworkObjectSeq")
    protected Long uid;

    /**
     * The objectid is unique for all objects of a given class
     */
    @Getter
    @Setter
    @NonNull
    @JsonView({Views.Public.class, Views.BaseVoltage.class, Views.NetworkVoltageLevel.class, Views.NetworkZone.class})
    protected String objectid;

    /**
     * The name is any free human readable and possibly non unique text naming the object.
     */
    @Getter
    @Setter
    @JsonView({Views.Public.class, Views.BaseVoltage.class, Views.NetworkVoltageLevel.class, Views.NetworkZone.class})
    protected String name;
}
