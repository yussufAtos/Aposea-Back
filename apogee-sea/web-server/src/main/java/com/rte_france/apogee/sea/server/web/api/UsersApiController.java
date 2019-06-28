package com.rte_france.apogee.sea.server.web.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.exceptions.AlreadyExistsException;
import com.rte_france.apogee.sea.server.exceptions.DataDeleteViolationException;
import com.rte_france.apogee.sea.server.exceptions.DataRetrievalException;
import com.rte_france.apogee.sea.server.exceptions.ResourceNotFoundException;
import com.rte_france.apogee.sea.server.model.dao.user.AuthorityRepository;
import com.rte_france.apogee.sea.server.model.dao.user.UserRepository;
import com.rte_france.apogee.sea.server.model.dao.user.UsertypeRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkZoneRepository;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.user.Authority;
import com.rte_france.apogee.sea.server.model.user.User;
import com.rte_france.apogee.sea.server.model.user.Usertype;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import com.rte_france.apogee.sea.server.wrapper.UsertypeWrapper;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

@Controller
@Api(tags = {"users"})
public class UsersApiController implements UsersApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersApiController.class);

    private final HttpServletRequest request;

    private UserRepository userRepository;

    private UsertypeRepository usertypeRepository;

    private AuthorityRepository authorityRepository;

    private NetworkZoneRepository networkZoneRepository;

    private static final String ACCEPT = "Accept";

    private static final String APPLICATION_JSON = "application/json";

    private static final String COULD_NOT_DELETE_USER = "Could not delete user";

    private static final String COULD_NOT_GET_USER = "Could not get user";

    private static final String COULD_NOT_UPDATE_USER = "Could not update user";

    private static final String COULD_NOT_ADD_NETWORK_ZONE = "Could not add networkZone";

    private static final String COULD_NOT_DELETE_NETWORK_ZONE = "Could not delete networkZone";

    private static final String USER_NOT_EXIST = "The user does not exist.";

    private static final String USERTYPE_NOT_EXIST = "The usertype does not exist.";

    @Autowired
    public UsersApiController(HttpServletRequest request, UserRepository userRepository, AuthorityRepository authorityRepository, UsertypeRepository usertypeRepository, NetworkZoneRepository networkZoneRepository) {
        this.request = request;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.usertypeRepository = usertypeRepository;
        this.networkZoneRepository = networkZoneRepository;
    }

    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<User> addUser(@Valid @RequestBody User user) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                if (userRepository.findOneByUsername(user.getUsername()).isPresent()) {
                    throw new AlreadyExistsException(new Throwable("The user already exists."));
                }

                //fetch authorities
                Collection<Authority> authorities = new ArrayList<>();

                user.getAuthorities().forEach(authority -> {
                    Optional<Authority> optionalAuthority = authorityRepository.findOneByName(authority.getAuthority());
                    if (!optionalAuthority.isPresent()) {
                        throw new ResourceNotFoundException(new Throwable("Authority not found should be created first - name: " + authority.getAuthority()));
                    }
                    authorities.add(optionalAuthority.get());
                });

                user.setAuthorities(authorities);

                return new ResponseEntity<>(userRepository.save(user), HttpStatus.OK);
            } catch (AlreadyExistsException | ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Couldn't add new use", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping(value = "/users/{username}",
            produces = {APPLICATION_JSON})
    public ResponseEntity<User> deleteUser(@PathVariable("username") String username) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                User user = validateUser(username);
                userRepository.delete(user);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                LOGGER.error(e.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_DELETE_USER, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('READ')")
    @GetMapping(value = "/users/{username}",
            produces = {APPLICATION_JSON})
    public ResponseEntity<User> getUser(@PathVariable("username") String username) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                User user = validateUser(username);
                return new ResponseEntity<>(user, HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                LOGGER.error(e.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_GET_USER, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('READ')")
    @GetMapping(value = "/users",
            produces = {APPLICATION_JSON})
    public ResponseEntity<List<User>> getUsers() {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                return new ResponseEntity<>(userRepository.findAll(), HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_GET_USER, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping(value = "/users/{username}",
            produces = {APPLICATION_JSON})
    public ResponseEntity<User> updateUser(@PathVariable("username") String username, @Valid @RequestBody User user) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                User userOrigin = validateUser(username);

                if (!user.getUsername().equals(username)) {
                    user.setUsername(username);
                }

                //fetch authorities
                Collection<Authority> authorities = new ArrayList<>();

                user.getAuthorities().forEach(o -> {
                    Optional<Authority> authority = authorityRepository.findOneByName(o.getAuthority());

                    if (!authority.isPresent()) {
                        throw new DataRetrievalException(new Throwable("Authority not found should be created first - authority: " + o.getAuthority()));
                    }

                    authorities.add(authority.get());
                });

                user.setId(userOrigin.getId());
                user.setAuthorities(authorities);

                return new ResponseEntity<>(userRepository.save(user), HttpStatus.OK);
            } catch (ResourceNotFoundException | DataRetrievalException e) {
                LOGGER.error(e.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_UPDATE_USER, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('READ')")
    @GetMapping(value = "/users/{username}/networkZones",
            produces = {"application/json"})
    public ResponseEntity<List<NetworkZone>> getNetworkZonesByUser(@PathVariable("username") String username) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                Optional<User> user = userRepository.findOneByUsername(username);

                if (!user.isPresent()) {
                    throw new ResourceNotFoundException(new Throwable(USER_NOT_EXIST));
                }

                Usertype usertype = user.get().getActualUsertype();
                if (usertype != null) {
                    Set<NetworkZone> networkZones = this.networkZoneRepository.findByUsertypes(usertype);
                    return new ResponseEntity<>(new ArrayList<>(networkZones), HttpStatus.OK);
                } else {
                    throw new ResourceNotFoundException(new Throwable("The actual usertype of user " + username + "not exist"));
                }
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_ADD_NETWORK_ZONE, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Gets all usertypes associated to user
     *
     * @param username : Username of user to find
     * @return A list of user types with the specified Username was found
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('READ')")
    @GetMapping(value = "/users/{username}/usertypes",
            produces = {"application/json"})
    public ResponseEntity<List<Usertype>> getUsertypesByUser(@PathVariable("username") String username) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                Optional<User> user = userRepository.findOneByUsername(username);

                if (!user.isPresent()) {
                    throw new ResourceNotFoundException(new Throwable(USER_NOT_EXIST));
                }

                Set<Usertype> usertypes = user.get().getUsertypes();
                return new ResponseEntity<>(new ArrayList<>(usertypes), HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_ADD_NETWORK_ZONE, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * Creates a usertype associated to user
     *
     * @param username        : Username of user to update
     * @param usertypeWrapper : The usertype wrapper you want to post
     * @return A user with the specified user type name was updated
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(value = "/users/{username}/usertypes",
            produces = {"application/json"})
    public ResponseEntity<User> addUsertypeByUser(@PathVariable("username") String username, @Valid @RequestBody UsertypeWrapper usertypeWrapper) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                User user = validateUser(username);
                Usertype defaultUsertype = usertypeWrapper.getDefaultUsertype();
                List<Usertype> usertypes = usertypeWrapper.getUsertypes();

                if (defaultUsertype == null || usertypes == null) {
                    throw new DataRetrievalException(new Throwable("Default usertype and a list usertypes must not be null"));
                }

                Optional<Usertype> defaultUsertypeOrigin = usertypeRepository.findByName(defaultUsertype.getName());

                if (!defaultUsertypeOrigin.isPresent()) {
                    throw new DataRetrievalException(new Throwable("Default usertype not found should be created first - name: " + defaultUsertype.getName()));
                }
                validateUsertypes(usertypes);
                user.setDefaultUsertype(defaultUsertypeOrigin.get());
                user.setActualUsertype(defaultUsertypeOrigin.get());

                if (!usertypes.contains(defaultUsertypeOrigin.get())) {
                    usertypes.add(defaultUsertypeOrigin.get());
                }
                user.getUsertypes().addAll(usertypes);
                userRepository.save(user);

                return new ResponseEntity<>(user, HttpStatus.OK);
            } catch (ResourceNotFoundException | DataRetrievalException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_ADD_NETWORK_ZONE, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Delete usertype by user </p>
     *
     * @param username : Username of user to update
     * @param name     : Name of user type to delete
     * @return A user with the specified user type name was updated
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping(value = "/users/{username}/usertypes/{name}",
            produces = {"application/json"})
    public ResponseEntity<User> deleteUsertypesByUser(@PathVariable("username") String username, @PathVariable("name") String name) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                User user = validateUser(username);
                Usertype usertypeOrigin = validateUsertype(name);

                Set<Usertype> usertypes = user.getUsertypes();
                if (!usertypes.contains(usertypeOrigin)) {
                    throw new ResourceNotFoundException(new Throwable(USERTYPE_NOT_EXIST));
                }
                if (user.getDefaultUsertype().equals(usertypeOrigin) || user.getActualUsertype().equals(usertypeOrigin)) {
                    throw new DataDeleteViolationException("Default usertype or actual usertype can't be deleted - name: " + usertypeOrigin.getName());
                }
                usertypes.remove(usertypeOrigin);
                user.getUsertypes().remove(usertypeOrigin);
                userRepository.save(user);

                return new ResponseEntity<>(user, HttpStatus.OK);
            } catch (ResourceNotFoundException | DataRetrievalException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_DELETE_NETWORK_ZONE, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    private User validateUser(String username) {
        Optional<User> user = userRepository.findOneByUsername(username);
        if (!user.isPresent()) {
            throw new ResourceNotFoundException(new Throwable(USER_NOT_EXIST));
        }
        return user.get();
    }

    private Usertype validateUsertype(String name) {
        Optional<Usertype> usertype = usertypeRepository.findByName(name);
        if (!usertype.isPresent()) {
            throw new ResourceNotFoundException(new Throwable(USERTYPE_NOT_EXIST));
        }
        return usertype.get();
    }

    private void validateUsertypes(List<Usertype> usertypes) {
        for (Usertype usertype : usertypes) {
            try {
                this.validateUsertype(usertype.getName());
            } catch (ResourceNotFoundException e) {
                throw new ResourceNotFoundException(new Throwable(USERTYPE_NOT_EXIST));
            }

        }
    }
}
