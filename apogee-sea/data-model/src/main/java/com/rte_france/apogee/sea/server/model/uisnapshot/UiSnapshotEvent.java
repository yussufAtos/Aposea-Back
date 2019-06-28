package com.rte_france.apogee.sea.server.model.uisnapshot;

import org.springframework.context.ApplicationEvent;

public class UiSnapshotEvent extends ApplicationEvent {

    public UiSnapshotEvent(Object source) {
        super(source);
    }
}
