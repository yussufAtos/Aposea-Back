package com.rte_france.apogee.sea.server.model.computation.variant;

import com.rte_france.apogee.sea.server.model.computation.NetworkLimitViolationsResult;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@Table(indexes = {@Index(name = "i_networkactionresult_networklimitviolationsresult", columnList = "networkLimitViolationsResult_id"),
        @Index(name = "i_networkactionresult_variantresult", columnList = "variantResult_id"),
        @Index(name = "i_networkactionresult_remedial", columnList = "remedial_id")})
public class NetworkActionResult implements Serializable {
    @Id
    @Column(name = "ACTION_RESULT_ID")
    @SequenceGenerator(name = "NetworkActionResultSeq", sequenceName = "NETWORK_ACTION_RESULT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "NetworkActionResultSeq")
    private Long id;

    private boolean actionEfficient;

    @ManyToOne
    @JoinColumn(name = "networkLimitViolationsResult_id")
    private NetworkLimitViolationsResult networkLimitViolationsResult;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "variantResult_id")
    private NetworkLimitViolationsResult variantResult;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remedial_id")
    private Remedial remedial;
}
