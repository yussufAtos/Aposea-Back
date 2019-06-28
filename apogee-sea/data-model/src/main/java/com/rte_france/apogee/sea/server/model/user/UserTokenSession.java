package com.rte_france.apogee.sea.server.model.user;

import lombok.*;
import javax.persistence.*;
import java.time.Instant;

@Entity
@ToString
@EqualsAndHashCode()
@NoArgsConstructor
public class UserTokenSession {

    static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @ToString.Exclude
    @Getter
    @Column(nullable = false, updatable = false)
    private Long id;

    @Getter
    @Column(nullable = false, unique = true)
    private String username;

    @Getter
    @Column(nullable = false, unique = true)
    private String token;

    @Getter
    @Column(nullable = false, unique = true)
    private String sessionId;

    @Getter
    @Column(nullable = false)
    private Long expiryTime;

    @Getter
    @EqualsAndHashCode.Exclude
    @Column(updatable = false)
    private Instant createdTime;

    @Getter
    @EqualsAndHashCode.Exclude
    @Column(insertable = false)
    private Instant updatedTime;

    @PrePersist
    protected void onCreate() {
        createdTime = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = Instant.now();
    }

    public UserTokenSession(String username, String token, String sessionId, Long expiryTime) {
        this.username = username;
        this.token = token;
        this.sessionId = sessionId;
        this.expiryTime = expiryTime;
    }
}
