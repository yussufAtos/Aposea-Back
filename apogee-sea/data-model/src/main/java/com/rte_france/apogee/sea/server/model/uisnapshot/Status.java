package com.rte_france.apogee.sea.server.model.uisnapshot;

/**
 *
 * C_CMP_NOK // contingency computation not OK
 * V_R_CMP_NOK // violations present, all remedials computation not OK for the contingency
 * NO_V // contingency does not create any limitViolation
 * V_R1_EFF // violations present and the first prioritized remedial is efficient
 * V_RX_EFF // violations present, an efficient prioritized remedial exists but is not the first prioritized remedial in the list
 * V_R_AV // violations present, no efficient remedials, remedials are available and should be prioritized by the user
 * V_NO_R_AV // violations present, no efficient remedials, no remedials available for priorization by the user
 */
public enum Status {
    C_CMP_NOK,
    V_R_CMP_NOK,
    NO_V,
    V_R1_EFF,
    V_RX_EFF,
    V_R_AV,
    V_NO_R_AV
}
