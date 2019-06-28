package com.rte_france.apogee.sea.server.wrapper;

import com.rte_france.apogee.sea.server.model.user.Usertype;
import lombok.Data;

import java.util.List;

@Data
public class UsertypeWrapper {
    private Usertype defaultUsertype;
    private List<Usertype> usertypes;
}
