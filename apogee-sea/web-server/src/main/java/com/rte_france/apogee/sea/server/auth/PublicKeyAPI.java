package com.rte_france.apogee.sea.server.auth;


import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.annotations.Api;
import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyPair;
import java.security.Principal;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

/**
 * Exposes public key.
 */
@RestController
@RequestMapping("/oauth")
@Api(value = "PublicKey API")
@FrameworkEndpoint
public class PublicKeyAPI {

    private KeyPair keyPair;

    public PublicKeyAPI(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    /**
     * Exposes public key
     *
     * @param principal connected user principal
     * @return public key
     */
    @GetMapping("/jwks.json")
    @ResponseBody
    public Map<String, Object> getKey(Principal principal) {
        RSAPublicKey publicKey = (RSAPublicKey) this.keyPair.getPublic();
        RSAKey key = new RSAKey.Builder(publicKey).build();
        return new JWKSet(key).toJSONObject();
    }
}
