package com.rte_france.apogee.sea.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import java.security.KeyPair;


@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    @Qualifier("userDetailsService")
    private UserDetailsService userDetailsService;

    private ClientDetailsService clientDetailsService;

    private AuthenticationManager authenticationManager;

    @Value("${config.oauth2.tokenTimeout}")
    private int expiration;

    @Value("${config.oauth2.tokenTimeout.opfab}")
    private int expirationOpFab;

    @Value("${config.oauth2.apogee.clientID}")
    private String clientApogeeId;

    @Value("${config.oauth2.opfab.clientID}")
    private String clientOpFabId;

    @Value("${config.oauth2.clientSecretBCrypt}")
    private String clientSecretBCrypt;

    @Value("${config.oauth2.resource.id}")
    private String resourceId;

    private KeyPair keyPair;

    @Autowired
    public AuthorizationServerConfig(UserDetailsService userDetailsService, ClientDetailsService clientDetailsService, AuthenticationManager authenticationManager, KeyPair keyPair) {
        this.userDetailsService = userDetailsService;
        this.clientDetailsService = clientDetailsService;
        this.authenticationManager = authenticationManager;
        this.keyPair = keyPair;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void configure(
            AuthorizationServerSecurityConfigurer oauthServer) {
        oauthServer
                .tokenKeyAccess("permitAll()")
                .checkTokenAccess("isAuthenticated()");
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {

        clients
                .inMemory()
                .withClient("clientIdPassword")
                .secret(clientSecretBCrypt)
                .authorizedGrantTypes(
                        "password", "authorization_code", "refresh_token")
                .scopes("read", "user_info")
                .autoApprove(true)
                .accessTokenValiditySeconds(expirationOpFab)
                .refreshTokenValiditySeconds(expirationOpFab)


                .and()

                .withClient(clientApogeeId)
                .authorizedGrantTypes("client_credentials", "password", "refresh_token", "authorization_code")
                .scopes("read", "write", "trust")
                .resourceIds(resourceId)
                .accessTokenValiditySeconds(expiration)
                .refreshTokenValiditySeconds(expiration)
                .secret(clientSecretBCrypt);
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {

        LOGGER.info("Initializing JWT with key: {}", keyPair.getPublic());

        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setKeyPair(keyPair);

        DefaultAccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
        DefaultUserAuthenticationConverter userTokenConverter = new SubjectAttributeUserTokenConverter();
        userTokenConverter.setUserDetailsService(userDetailsService);
        accessTokenConverter.setUserTokenConverter(userTokenConverter);

        converter.setAccessTokenConverter(accessTokenConverter);

        return converter;
    }

    @Bean
    public JwtTokenStore tokenStore() {
        return new JwtTokenStore(accessTokenConverter());
    }

    @Bean
    @Primary
    public DefaultTokenServices tokenServices() {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(tokenStore());
        defaultTokenServices.setClientDetailsService(clientDetailsService);
        defaultTokenServices.setSupportRefreshToken(true);
        defaultTokenServices.setTokenEnhancer(accessTokenConverter());
        return defaultTokenServices;
    }

    /**
     * Defines the authorization and token endpoints and the token services
     *
     * @param authorizationServerEndpointsConfigurer Configure the properties and enhanced functionality of the Authorization Server endpoints.
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer authorizationServerEndpointsConfigurer) {

        authorizationServerEndpointsConfigurer
                .authenticationManager(authenticationManager)
                .userDetailsService(userDetailsService)
                .allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST)
                .tokenStore(tokenStore())
                .tokenServices(tokenServices())
                .accessTokenConverter(accessTokenConverter());
    }

}
