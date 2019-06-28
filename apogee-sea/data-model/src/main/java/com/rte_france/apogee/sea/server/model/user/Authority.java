package com.rte_france.apogee.sea.server.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import javax.persistence.*;

@Entity
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Authority implements GrantedAuthority {

    @Id
    @SequenceGenerator(name = "AuthoritiesSeq", sequenceName = "AUTHORITIES_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AuthoritiesSeq")
    @Column(nullable = false, unique = true)
    @Getter
    @Setter
    @JsonIgnore
    private Long id;

    @JsonView({Views.Public.class})
    @Getter
    @Column(nullable = false, unique = true)
    private String name;

    public Authority(String name) {
        this.name = name;
    }

    @Override
    public String getAuthority() {
        return getName();
    }
}
