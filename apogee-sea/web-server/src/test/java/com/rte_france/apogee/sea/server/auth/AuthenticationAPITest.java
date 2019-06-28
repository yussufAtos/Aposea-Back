package com.rte_france.apogee.sea.server.auth;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.rte_france.apogee.sea.server.model.user.UserTokenSession;
import com.rte_france.apogee.sea.server.services.UserTokenSessionService;
import com.rte_france.apogee.sea.server.services.impl.UserTokenSessionServiceImpl;
import com.rte_france.powsybl.hades2.Hades2Factory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ReflectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.nio.file.FileSystem;

import static org.mockito.Matchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class AuthenticationAPITest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationAPITest.class);

    static {
        // Load the module load-flow-action-simulator in memory instead of the itools config file config.xml
        try {
            FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
            InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
            MapModuleConfig config = platformConfig.createModuleConfig("load-flow-action-simulator");
            config.setClassProperty("load-flow-factory", Hades2Factory.class);
            config.setStringProperty("max-iterations", "3");
            config.setStringProperty("ignore-pre-contingency-violations", "true");
            PlatformConfig.setDefaultConfig(platformConfig);
            fileSystem.close();
        } catch (Exception e) {
            LOGGER.error("Error during initialization of Module load-flow-action-simulator");
        }
    }

    @InjectMocks
    private AuthenticationAPI authenticationAPI;

    private MockMvc mockMvc;

    private UserTokenSessionServiceImpl userTokenSessionService;

    @Value("${config.oauth2.tokenTimeout}")
    private String tokenExpiryTime;


    @Autowired
    private OAuthHelper oAuthHelper;

    private RequestPostProcessor requestPostProcessor;
    private UserTokenSession userTokenSession;
    private HttpHeaders httpHeaders;
    private OAuth2Authentication oAuth2Authentication;

    @BeforeEach
    public void setup() {

        MockitoAnnotations.initMocks(this);
        userTokenSessionService = Mockito.mock(UserTokenSessionServiceImpl.class);

        Field field = ReflectionUtils.findField(AuthenticationAPI.class, "userTokenSessionService");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, authenticationAPI, userTokenSessionService);

        this.mockMvc = MockMvcBuilders.standaloneSetup(authenticationAPI).build();

        String username = "user";
        String password = "password";

        requestPostProcessor = oAuthHelper.addBearerToken(username, password);
        Authentication authentication = new UsernamePasswordAuthenticationToken(username, password);
        OAuth2Request oAuth2Request = oAuthHelper.createOAuth2Request(username, password);
        oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);
        HttpServletRequest httpServletRequest = oAuthHelper.buildMockHttpServletRequest(username, password);
        OAuth2AuthenticationDetails oAuth2AuthenticationDetails = new OAuth2AuthenticationDetails(httpServletRequest);
        oAuth2Authentication.setDetails(oAuth2AuthenticationDetails);

        userTokenSession = new UserTokenSession(username, httpServletRequest.getHeader("Authorization"), "sessionid=123", Long.valueOf(tokenExpiryTime));

        httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", httpServletRequest.getHeader("Authorization"));
        httpHeaders.add("cookie", "sessionid=123;domain=test.com");
    }


    @Test
    public void testValidateToken() throws Exception {

        UserTokenSessionService.ValidMappingResponse expectedValidMappingResponse = new UserTokenSessionService.ValidMappingResponse(true, userTokenSession);
        Mockito.when(userTokenSessionService.isValidUserTokenSessionMapping(any())).thenReturn(expectedValidMappingResponse);

        this.mockMvc.perform(post("/oauth/validateToken").with(requestPostProcessor).principal(oAuth2Authentication).headers(httpHeaders))
                .andExpect(status().is(200))
                .equals(userTokenSession);
    }

    @Test
    public void testLogin() throws Exception {

        Mockito.when(userTokenSessionService.saveUserTokenSessionMapping(any())).thenReturn(userTokenSession);

        this.mockMvc.perform(post("/oauth/login").with(requestPostProcessor).principal(oAuth2Authentication).headers(httpHeaders))
                .andExpect(status().is(200))
                .equals(userTokenSession);
    }


}
