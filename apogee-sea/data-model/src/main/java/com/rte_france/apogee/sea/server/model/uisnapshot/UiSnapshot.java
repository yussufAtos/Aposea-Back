package com.rte_france.apogee.sea.server.model.uisnapshot;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

//squid:S3437 -> Make this value-based field transient so it is not included in the serialization of this class.
// suppress false positive for Instant fields database serialization
@SuppressWarnings("squid:S3437")
@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
public class UiSnapshot implements Serializable {

    @JsonView(Views.Public.class)
    @Id
    @SequenceGenerator(name = "UiSnapshotSeq", sequenceName = "UI_SNAPSHOT_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "UiSnapshotSeq")
    Long id;

    @NonNull
    protected Instant startedDate;

    @JsonView(Views.Public.class)
    protected Instant createdDate;

    @OneToMany(mappedBy = "uiSnapshot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<UiSnapshotContingency> uiSnapshotContingencyList;


    @OneToMany(mappedBy = "uiSnapshot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<UiSnapshotContext> uiSnapshotContexts;
}
