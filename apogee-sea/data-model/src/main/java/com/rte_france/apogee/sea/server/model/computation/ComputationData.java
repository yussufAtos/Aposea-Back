package com.rte_france.apogee.sea.server.model.computation;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;


@Entity
@RequiredArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ComputationData {

    @Id
    @NonNull
    private String idAfsSecurityAnalysisRunner;

    private Long networkContextId;

    private boolean computationWithCodeIal;

    @Enumerated(value = EnumType.STRING)
    private ExecStatus status;

}
