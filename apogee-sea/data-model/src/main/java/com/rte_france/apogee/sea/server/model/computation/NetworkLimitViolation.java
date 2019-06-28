package com.rte_france.apogee.sea.server.model.computation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.zones.NetworkVoltageLevel;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@ToString
@RequiredArgsConstructor()
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Table(indexes = {@Index(name = "i_limitviolationsresult", columnList = "limitviolationsresult_id")})
@Getter
@Setter
public class NetworkLimitViolation implements Serializable {

    @Id
    @SequenceGenerator(name = "NetworkLimitViolationSeq", sequenceName = "NETWORK_LIMIT_VIOLATION_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "NetworkLimitViolationSeq")
    @JsonView({Views.Public.class})
    private Long id;

    @NonNull
    @JsonView({Views.Public.class,  Views.ComputationResults.class})
    private String subjectId;

    @JsonView({Views.Public.class})
    private String limitType;

    @Column(name = "\"limit\"") //required to avoid clash with SQL 'limit' keyword
    @NonNull
    @JsonView({Views.Public.class})
    private double limit;

    @JsonView({Views.Public.class})
    private String limitName;

    @JsonView({Views.Public.class})
    private Integer acceptableDuration;

    @NonNull
    private float limitReduction;

    @NonNull
    @JsonView({Views.Public.class})
    private double value;

    @JsonView({Views.Public.class})
    private String side;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonView({Views.Public.class})
    private Double valueMw;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonView({Views.Public.class})
    private Double preValue;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonView({Views.Public.class})
    private Double preValueMw;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "limitviolationsresult_id")
    private NetworkLimitViolationsResult networkLimitViolationsResult;

    /**
     * Set of NetworkVoltageLevel the subjectId is connected to, obtained from iTesla
     */

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "network_limitviolation_voltagelevel",
            joinColumns = @JoinColumn(name = "limitviolation_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "voltagelevel_id", referencedColumnName = "uid"))
    @Getter
    @Setter
    @ToString.Exclude
    @OrderBy
    @EqualsAndHashCode.Exclude
    @JsonView({Views.UiSnapshot.class})
    private Set<NetworkVoltageLevel> networkVoltageLevels = new HashSet<>();

    public NetworkLimitViolation(String subjectId, String limitType, double limit, String limitName, int acceptableDuration, float limitReduction, double value) {
        this.subjectId = subjectId;
        this.limitType = limitType;
        this.limit = limit;
        this.limitName = limitName;
        this.acceptableDuration = acceptableDuration;
        this.limitReduction = limitReduction;
        this.value = value;
    }
}
