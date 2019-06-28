package com.rte_france.apogee.sea.server.afs;

import com.powsybl.afs.AppData;
import com.powsybl.afs.ws.server.utils.AppDataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@ComponentScan(basePackageClasses = AppDataBean.class)
@Lazy
public class AppDataBeanConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDataBeanConfig.class);

    //Note: spring is supposed to auto-detect the "close" method when destroying the bean
    @Bean
    public AppData getAppData(AppDataBean appDataBean) {
        LOGGER.info("Initializing AFS bean");
        return appDataBean.getAppData();
    }
}
