package com.rte_france.apogee.sea.server.model.computation;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

/**
 * pathInConvergence allows you to configure the convergence path
 * e.g
 * /2_Situation_Réseau/PointsFiges/recollement/enrichi/recollement-auto-yyyyMMdd-hhmm-enrichi
 * The path to open  for the date 03 June 2019 12:00 is
 * /2_Situation_Réseau/PointsFiges/recollement/enrichi/recollement-auto-20190603-1200-enrichi
 */
@Entity
@RequiredArgsConstructor
@NoArgsConstructor
public class CaseType implements Serializable {
    @Id
    @Getter
    @Setter
    @NonNull
    @JsonView({Views.Public.class})
    private String name;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "caseCategory_id")
    @JsonView({Views.Public.class})
    private CaseCategory caseCategory;

    @Getter
    @Setter
    private boolean enabled;

    @Getter
    @Setter
    private boolean opfabEnabled;

    /**
     * The tag name for OpFab cards
     */
    @NonNull
    @Getter
    @Setter
    private String cardTag = name;

    /**
     * Minutes to be added to the OpFab card start date
     */
    @Getter
    @Setter
    private Integer cardStartDateIncrement;

    /**
     * Minutes to be added to the OpFab card end date
     */
    @Getter
    @Setter
    private Integer cardEndDateIncrement;

    @Getter
    @Setter
    @JsonView({Views.UiSnapshot.class})
    private String pathInConvergence;
}
