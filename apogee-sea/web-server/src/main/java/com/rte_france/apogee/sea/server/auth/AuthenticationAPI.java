package com.rte_france.apogee.sea.server.auth;

import com.rte_france.apogee.sea.server.model.user.UserTokenSession;
import com.rte_france.apogee.sea.server.services.UserTokenSessionService;
import com.rte_france.apogee.sea.server.services.impl.UserTokenSessionServiceImpl;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Objects;

/**
 * Authenticate user using authorization token.
 */
@RestController
@RequestMapping("/oauth")
@Api(value = "Authentication API")
public class AuthenticationAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationAPI.class);

    private static final String MESSAGE = "Message";

    private static final String SETCOOKIE = "Set-Cookie";

    @Value("${config.oauth2.tokenTimeout}")
    private long tokenExpiryTime;

    private UserTokenSessionServiceImpl userTokenSessionService;

    @Autowired
    public AuthenticationAPI(UserTokenSessionServiceImpl userTokenSessionService) {
        this.userTokenSessionService = userTokenSessionService;
    }

    @ApiOperation(value = "Authenticated User Login", response = UserTokenSession.class)
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = UserTokenSession.class),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Failure")})
    public ResponseEntity<UserTokenSession> login(@RequestHeader HttpHeaders httpHeaders, Principal principal, HttpServletRequest httpServletRequest) {

        String username = principal.getName();
        UserTokenSession userTokenSession = buildUserTokenSession(principal, httpHeaders);
        userTokenSession = userTokenSessionService.saveUserTokenSessionMapping(userTokenSession);

        LOGGER.info("User {} successfully logged in. User, Token and Session mapping stored: {}", username, userTokenSession);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(MESSAGE, "Success");
        responseHeaders.add(SETCOOKIE, userTokenSession.getSessionId());

        return new ResponseEntity(userTokenSession, responseHeaders, HttpStatus.OK);
    }

    @ApiOperation(value = "Validate the given token", response = UserTokenSession.class)
    @PostMapping(value = "/validateToken", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = UserTokenSession.class),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Failure")})
    public ResponseEntity<UserTokenSession> validateToken(@RequestHeader HttpHeaders httpHeaders, Principal principal, HttpServletRequest httpServletRequest) {


        String username = principal.getName();
        UserTokenSession userTokenSession = buildUserTokenSession(principal, httpHeaders);

        ResponseEntity<UserTokenSession> responseEntity;
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(SETCOOKIE, userTokenSession.getSessionId());

        UserTokenSessionService.ValidMappingResponse validMappingResponse = userTokenSessionService.isValidUserTokenSessionMapping(userTokenSession);
        if (validMappingResponse.isValid()) {

            LOGGER.info("User {} has valid token: {}", username, validMappingResponse.getUserTokenSession());
            responseHeaders.add(MESSAGE, "Valid Token");
            responseEntity = new ResponseEntity<>(validMappingResponse.getUserTokenSession(), responseHeaders, HttpStatus.OK);

        } else {

            LOGGER.info("User {} has invalid token.", username);
            responseHeaders.add(MESSAGE, "Invalid Token");
            responseEntity = new ResponseEntity<>(userTokenSession, responseHeaders, HttpStatus.UNAUTHORIZED);
        }

        return responseEntity;
    }

    /**
     * Build Token session using {@link Principal} and {@link HttpHeaders}
     *
     * @param principal
     * @param httpHeaders
     * @return TokenSession
     */
    private UserTokenSession buildUserTokenSession(Principal principal, HttpHeaders httpHeaders) {

        OAuth2AuthenticationDetails oAuth2AuthenticationDetails = (OAuth2AuthenticationDetails) ((OAuth2Authentication) principal).getDetails();
        String tokenValue = oAuth2AuthenticationDetails.getTokenValue();
        String username = principal.getName();
        String[] sessionId = new String[1];

        if (Objects.nonNull(httpHeaders.get("cookie"))) {
            sessionId = httpHeaders.get("cookie").get(0).split(";");
        } else {
            LOGGER.info("User {} cookie not found. JSessionId not set.", username);

            /**
             * Swagger2 does not support cookie for that putting default sessssion id.
             */
            sessionId[0] = "JSEESION-ID";
        }

        return new UserTokenSession(username, tokenValue, sessionId[0], tokenExpiryTime);
    }


}
