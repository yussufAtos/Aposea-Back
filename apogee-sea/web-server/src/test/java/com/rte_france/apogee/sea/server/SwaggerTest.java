package com.rte_france.apogee.sea.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {com.rte_france.apogee.sea.server.SwaggerTest.class})
@OverrideAutoConfiguration(enabled = true)
@EnableSwagger2
@ImportAutoConfiguration(value = {WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
public class SwaggerTest {
    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @Value("${springfox.documentation.swagger.v2.path:/v2/api-docs}")
    private String docPath;

    @BeforeEach
    public void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .build();
    }

    @Test
    public void swaggerDocControllerTest() {
        try {
            mvc.perform(get(docPath)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
