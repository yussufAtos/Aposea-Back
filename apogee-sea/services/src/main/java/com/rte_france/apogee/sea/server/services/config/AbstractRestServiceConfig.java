package com.rte_france.apogee.sea.server.services.config;

import com.powsybl.commons.exceptions.UncheckedUriSyntaxException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
public abstract class AbstractRestServiceConfig {
    private String appName;
    private String hostName;
    private Integer port;
    private Boolean secure;
    private String username;
    private String password;

    @Getter(AccessLevel.PRIVATE)
    private Map<String, String> services = new HashMap<>();


    public Optional<String> getServicePath(String serviceName) {
        return Optional.ofNullable(services.get(serviceName));
    }


    public URI asUri() {
        Objects.requireNonNull(hostName);
        Objects.requireNonNull(port);

        try {
            return new URI(Boolean.TRUE.equals(getSecure()) ? "https" : "http", null, getHostName(), getPort(), null, null, null);
        } catch (URISyntaxException e) {
            throw new UncheckedUriSyntaxException(e);
        }
    }


    public String getBasicAuthentication() {
        Objects.requireNonNull(username);
        String pwd = (getPassword() != null) ? getPassword() : "";
        return Authentication.basicAuthHeader(username, pwd);
    }

}
