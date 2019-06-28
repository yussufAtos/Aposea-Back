package com.rte_france.apogee.sea.server.remedials;

import com.rte_france.apogee.sea.server.services.config.AbstractRestServiceConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;


@Component(value = "repasProperties")
@PropertySource("classpath:apogee.remedials.properties")
@ConfigurationProperties("remedials.sea")
public class RemedialsRepositoryConfig extends AbstractRestServiceConfig {

    public static final String REMEDIALS_QUERYING = "remedials.querying";
    public static final String IAL_CODE_REMEDIALS = "ial.code.remedials";
    public static final String IAL_CODE_REMEDIALS_WITHTESTONCONSTRAINTS = "ial.code.remedials.withtestonconstraints";
    public static final String REMEDIALS_QUERYING_UPDATESTATISTICS = "remedials.querying.updatestatistics";

}


