package com.rte_france.apogee.sea.server.opfab.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Contains all the properties of the OpFab service
 */

@Getter
@Component
public class OpFabProperties {

    /**
     * the {@link} is to enable/disable OpFab service
     */
    @Value("${opfab.service.enable}")
    private Boolean serviceEnable;

    @Value("${opfab.card.publisher.name}")
    private String publisherName;

    @Value("${opfab.card.publisher.version}")
    private String publisherVersion;

    @Value("${opfab.card.template.name}")
    private String templateName;

    @Value("${opfab.card.template.tab.name}")
    private String templateTabName;

    @Value("${opfab.card.style.name}")
    private String styleName;

    @Value("${opfab.card.template.tab.max}")
    private Integer maxTabNumber;

    @Value("${opfab.card.title.key}")
    private String titleKey;

    @Value("#{'${opfab.card.title.parameters}'.split(',')}")
    private List<String> titleParameters;

    @Value("${opfab.card.summary.key}")
    private String summaryKey;

    @Value("#{'${opfab.card.summary.parameters}'.split(',')}")
    private List<String> summaryParameters;

    @Value("${opfab.card.contingencies}")
    private String summaryStatic;

    @Value("${opfab.card.tags}")
    private List<String> tags;

    @Value("${opfab.card.details.generic.key}")
    private String detailsGenericKey;

    @Value("${opfab.card.details.pf.key}")
    private String detailsPfKey;

    @Value("#{'${opfab.card.details.pf.parameters}'.split(',')}")
    private List<String> detailsPfParameters;
}
