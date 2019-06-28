package com.rte_france.apogee.sea.server.model.uisnapshot;

import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;


@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
@Table(indexes = {@Index(name = "i_uisnapshotcontext_networkContext", columnList = "networkContext_id"),
        @Index(name = "i_uisnapshotcontext_uisnapshot", columnList = "uiSnapshot_id")})
public class UiSnapshotContext implements Serializable {

    @Id
    @SequenceGenerator(name = "UiSnapshotContextSeq", sequenceName = "UI_SNAPSHOT_CONTEXT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "UiSnapshotContextSeq")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uiSnapshot_id")
    private UiSnapshot uiSnapshot;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "networkContext_id")
    @NonNull
    private NetworkContext networkContext;

}
