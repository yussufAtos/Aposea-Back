package com.rte_france.apogee.sea.server.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.time.Instant;
import java.util.*;

//squid:S3437 -> Make this value-based field transient so it is not included in the serialization of this class.
// suppress false positive for Instant fields database serialization
@SuppressWarnings("squid:S3437")
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class User implements UserDetails {

    static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "UsersSeq", sequenceName = "USERS_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "UsersSeq")
    @Column(nullable = false, unique = true)
    @Getter
    @Setter
    @JsonIgnore
    private Long id;

    @JsonView({Views.Public.class})
    @Column(nullable = false, unique = true)
    @Setter
    private String username;

    @JsonView({Views.Public.class})
    @Column(nullable = false)
    @Setter
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @JsonView({Views.Public.class})
    @Getter
    @Setter
    @Column(nullable = false)
    private boolean enabled;

    @JsonView({Views.Public.class})
    @Getter
    @Column(updatable = false)
    private Instant createdTime;

    @JsonView({Views.Public.class})
    @Getter
    @Column(insertable = false)
    private Instant updatedTime;

    @JsonView({Views.Public.class})
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "users_authorities", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "authority_id", referencedColumnName = "id"))
    @OrderBy
    @Setter
    private Collection<Authority> authorities;

    /**
     * The list of user types.
     */
    @JsonView({Views.Public.class})
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usertypes_users", joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "usertype_name", referencedColumnName = "name"))
    @OrderBy
    @ToString.Exclude
    @Getter
    @Setter
    private Set<Usertype> usertypes = new HashSet<>();

    /**
     * Link to the default user type  this user applies to.
     */
    @JsonView({Views.Public.class})
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_usertype_id")
    @Getter
    @Setter
    protected Usertype defaultUsertype;

    /**
     * Link to the actual user type this user applies to.
     */
    @JsonView({Views.Public.class})
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actual_usertype_id")
    @Getter
    @Setter
    protected Usertype actualUsertype;


    public User(String username, String password, boolean enabled, List<Authority> authorities) {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.authorities = new ArrayList<>(authorities);
    }

    @PrePersist
    protected void onCreate() {
        createdTime = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = Instant.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return new ArrayList<>(authorities);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
