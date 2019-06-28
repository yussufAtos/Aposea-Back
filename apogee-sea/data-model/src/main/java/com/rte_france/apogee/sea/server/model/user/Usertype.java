package com.rte_france.apogee.sea.server.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usertypes")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Data
@EqualsAndHashCode
public class Usertype implements Serializable {

    @Id
    @JsonView({Views.Public.class})
    private String name;

    @Getter
    @Setter
    @JsonView({Views.Public.class})
    private boolean opfabEnabled;

    @JsonView({Views.Public.class})
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usertypes_network_zones", joinColumns = @JoinColumn(name = "usertype_name", referencedColumnName = "name"),
            inverseJoinColumns = @JoinColumn(name = "network_zone_id", referencedColumnName = "uid"))
    @OrderBy
    @EqualsAndHashCode.Exclude
    private Set<NetworkZone> networkZones = new HashSet<>();

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usertypes_users", joinColumns = @JoinColumn(name = "usertype_name", referencedColumnName = "name"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"))
    @OrderBy
    @EqualsAndHashCode.Exclude
    private Set<User> users = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "networkZone_id")
    private NetworkZone excludeZone;

    public Usertype(String name) {
        this.name = name;
    }
}
