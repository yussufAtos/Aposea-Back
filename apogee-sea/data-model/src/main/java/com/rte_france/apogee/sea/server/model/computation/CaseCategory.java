package com.rte_france.apogee.sea.server.model.computation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@RequiredArgsConstructor
@NoArgsConstructor
public class CaseCategory implements Serializable {
    @Id
    @Getter
    @Setter
    @NonNull
    @JsonView({Views.Public.class})
    private String name;

    @Getter
    @Setter
    private String description;

    @Getter
    @Setter
    private Integer displayPriority;

    @Getter
    @Setter
    private Integer displayLimit;

    @Getter
    @Setter
    @Column(name = "trigger_ui_snapshot")
    private boolean triggerUiSnapshot;

    @Getter
    @JsonIgnore
    @OneToMany(mappedBy = "caseCategory", fetch = FetchType.EAGER)
    private List<CaseType> caseTypes = new ArrayList<>();
}
