package com.rte_france.apogee.sea.server.afs;

import com.powsybl.afs.storage.events.NodeCreated;

public class NodeCreatedMock extends NodeCreated {

    protected NodeCreatedMock(String id, String parentId) {
        super(id, parentId);
    }
}
