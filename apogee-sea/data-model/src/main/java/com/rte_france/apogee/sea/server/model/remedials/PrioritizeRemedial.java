package com.rte_france.apogee.sea.server.model.remedials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@RequiredArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@IdClass(PrioritizeRemedialId.class)
@Table(indexes = {@Index(name = "i_prioritize", columnList = "prioritize_id"),
        @Index(name = "i_remedial", columnList = "remedial_id")})
public class PrioritizeRemedial implements Serializable {

    @Id
    @NonNull
    @JsonView({Views.Prioritize.class})
    private int prioritizeValue;

    @Id
    @NonNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prioritize_id")
    @JsonIgnore
    private Prioritize prioritize;

    @JsonView({Views.Prioritize.class})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remedial_id")
    private Remedial remedial;
}

