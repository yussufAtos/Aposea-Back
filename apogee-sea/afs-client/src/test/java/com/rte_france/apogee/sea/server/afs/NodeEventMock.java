package com.rte_france.apogee.sea.server.afs;

import com.powsybl.afs.storage.events.NodeEvent;
import com.powsybl.afs.storage.events.NodeEventType;

public class NodeEventMock extends NodeEvent {

    protected NodeEventMock(String id, NodeEventType type) {
        super(id, type);
    }
}
