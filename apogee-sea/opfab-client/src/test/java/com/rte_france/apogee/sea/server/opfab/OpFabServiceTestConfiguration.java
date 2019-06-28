package com.rte_france.apogee.sea.server.opfab;

import com.rte_france.apogee.sea.server.opfab.client.web.api.CardsApiClient;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

public class OpFabServiceTestConfiguration {

    @Value("${cardManagement.url}")
    String uri;

    @Bean
    public OpFabService autowiredOpFabService() {
        CardsApiClient cardsApiClient = Feign.builder()
                .contract(new SpringMvcContract())
                .decoder(new ResponseEntityDecoder(new JacksonDecoder()))
                .encoder(new JacksonEncoder())
                .target(CardsApiClient.class, uri);

        return new OpFabService(cardsApiClient);
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster
                = new SimpleApplicationEventMulticaster();

        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return eventMulticaster;
    }
}
