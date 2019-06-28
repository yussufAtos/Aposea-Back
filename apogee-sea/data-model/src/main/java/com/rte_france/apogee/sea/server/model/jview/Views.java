package com.rte_france.apogee.sea.server.model.jview;

public final class Views {

    // show only public data
    public interface Public {
    }

    // show  ComputationResults data
    public interface ComputationResults extends BasecaseResults, PostContingencyResults {
    }

    // show only BasecaseResults data
    public interface BasecaseResults extends Public {
    }

    // show only PostContingencyResults data
    public interface PostContingencyResults extends Public {
    }

    // show only Prioritize data
    public interface Prioritize extends Public {
    }

    // show only NetworkContext data
    public interface NetworkContext extends Public {
    }

    // show only UiSnapshot data
    public interface UiSnapshot extends Public {
    }

    // show only BaseVoltage data
    public interface BaseVoltage extends Public {
    }

    // show only NetworkVoltageLevel data
    public interface NetworkVoltageLevel extends Public {
    }

    // show only NetworkZone data
    public interface NetworkZone extends Public {
    }

}
