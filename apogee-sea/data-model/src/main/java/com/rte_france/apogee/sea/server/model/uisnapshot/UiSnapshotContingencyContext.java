package com.rte_france.apogee.sea.server.model.uisnapshot;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@Table(indexes = {@Index(name = "i_uisnapshotcontingencycontext_uisnapshotcontingency", columnList = "uiSnapshotContingency_id"),
        @Index(name = "i_uisnapshotcontingencycontext_uisnapshotcontext", columnList = "uiSnapshotContext_id")})
public class UiSnapshotContingencyContext implements Serializable, Comparable<UiSnapshotContingencyContext> {

    @Id
    @SequenceGenerator(name = "UiSnapshotContingencyContextSeq", sequenceName = "UI_SNAPSHOT_CONTINGENCY_CONTEXT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "UiSnapshotContingencyContextSeq")
    Long id;

    @Enumerated(value = EnumType.STRING)
    Status status;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "uiSnapshotContingency_id")
    private UiSnapshotContingency uiSnapshotContingency;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "uiSnapshotContext_id")
    private UiSnapshotContext uiSnapshotContext;

    @ElementCollection
    @CollectionTable(name = "remedialsCandidates", joinColumns = @JoinColumn(name = "sContingencyContext_id"))
    @Column(name = "rCandidate")
    private Set<String> remedialsCandidates;

    @ElementCollection
    @CollectionTable(name = "remedialsComputed", joinColumns = @JoinColumn(name = "sContingencyContext_id"))
    @Column(name = "rComputed")
    private Set<String> remedialsComputed;

    @ElementCollection
    @CollectionTable(name = "remedialsEfficient", joinColumns = @JoinColumn(name = "sContingencyContext_id"))
    @Column(name = "rEfficient")
    private Set<String> remedialsEfficient;

    @Override
    public int compareTo(UiSnapshotContingencyContext o) {
        return this.uiSnapshotContext.getNetworkContext().getNetworkDate().compareTo(o.uiSnapshotContext.getNetworkContext().getNetworkDate());
    }
}
