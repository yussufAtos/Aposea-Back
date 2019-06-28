package com.rte_france.apogee.sea.server.web.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rte_france.apogee.sea.server.exceptions.AlreadyExistsException;
import com.rte_france.apogee.sea.server.exceptions.DataDeleteViolationException;
import com.rte_france.apogee.sea.server.exceptions.DataRetrievalException;
import com.rte_france.apogee.sea.server.exceptions.ResourceNotFoundException;
import com.rte_france.apogee.sea.server.model.dao.user.UserRepository;
import com.rte_france.apogee.sea.server.model.dao.user.UsertypeRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkZoneRepository;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.user.User;
import com.rte_france.apogee.sea.server.model.user.Usertype;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-04-02T16:17:49.751+02:00")

@Controller
public class UsertypesApiController implements UsertypesApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsertypesApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    private UsertypeRepository usertypeRepository;

    private UserRepository userRepository;

    private NetworkZoneRepository networkZoneRepository;

    private static final String ACCEPT = "Accept";

    private static final String APPLICATION_JSON = "application/json";

    private static final String COULD_NOT_ADD_NETWORK_ZONE = "Could not add networkZone";

    private static final String COULD_NOT_DELETE_NETWORK_ZONE = "Could not delete networkZone";

    private static final String USERTYPE_NOT_EXIST = "The usertype does not exist";

    private static final String USER_NOT_EXIST = "The user does not exist.";

    private static final String NETWORKZONE_NOT_EXIST = "The networkZone does not exist";

    private static final String COULD_NOT_DELETE_USERTYPE = "Could not delete user type";

    private static final String COULD_NOT_GET_USERTYPE = "Could not get user type";

    private static final String COULD_NOT_UPDATE_USERTYPE = "Could not update user type";


    @org.springframework.beans.factory.annotation.Autowired
    public UsertypesApiController(ObjectMapper objectMapper, HttpServletRequest request, UsertypeRepository usertypeRepository, UserRepository userRepository, NetworkZoneRepository networkZoneRepository) {
        this.objectMapper = objectMapper;
        this.request = request;
        this.usertypeRepository = usertypeRepository;
        this.userRepository = userRepository;
        this.networkZoneRepository = networkZoneRepository;
    }

    /**
     * <p> Gets all user types </p>
     *
     * @return List<Usertype> : List all the existing user types
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('READ')")
    @GetMapping(value = "/usertypes",
            produces = {APPLICATION_JSON})
    public ResponseEntity<List<Usertype>> getUsertypes() {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                List<Usertype> usertypes = usertypeRepository.findAll();
                return new ResponseEntity<>(usertypes, HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_GET_USERTYPE, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Creates a new user type </p>
     *
     * @param usertype The user type you want to post
     * @return usertype : User type created
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Usertype> addUsertype(@Valid @RequestBody Usertype usertype) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                if (usertypeRepository.findByName(usertype.getName()).isPresent()) {
                    throw new AlreadyExistsException(new Throwable("The user type already exists."));
                }

                //fetch networkZones
                Set<NetworkZone> networkZones = new HashSet<>();

                usertype.getNetworkZones().forEach(networkZone -> {
                    Optional<NetworkZone> optionalNetworkZone = networkZoneRepository.findByObjectid(networkZone.getObjectid());
                    if (!optionalNetworkZone.isPresent()) {
                        throw new ResourceNotFoundException(new Throwable("networkZone not found should be created first - name: " + networkZone.getObjectid()));
                    }
                    networkZones.add(optionalNetworkZone.get());
                });
                usertype.setNetworkZones(networkZones);

                if (usertype.getExcludeZone() != null) {
                    Optional<NetworkZone> optionalNetworkZone = networkZoneRepository.findByObjectid(usertype.getExcludeZone().getObjectid());
                    if (!optionalNetworkZone.isPresent()) {
                        throw new ResourceNotFoundException(new Throwable("exclude networkZone not found should be created first - name: " + usertype.getExcludeZone().getObjectid()));
                    }
                    usertype.setExcludeZone(optionalNetworkZone.get());
                }

                return new ResponseEntity<>(usertypeRepository.save(usertype), HttpStatus.OK);
            } catch (AlreadyExistsException | ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Couldn't add new user type", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Find user type </p>
     *
     * @param name : Name of user type to find
     * @return Usertype : A user type with the specified name was found
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('READ')")
    @GetMapping(value = "/usertypes/{name}",
            produces = {APPLICATION_JSON})
    public ResponseEntity<Usertype> getUsertype(@PathVariable("name") String name) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                Usertype usertype = validateUsertype(name);
                return new ResponseEntity<>(usertype, HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                LOGGER.error(e.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_GET_USERTYPE, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Update user type </p>
     *
     * @param name     : Update user type
     * @param usertype : The user type you want to post
     * @return usertype : A user type with the specified name was updated
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping(value = "/usertypes/{name}",
            produces = {APPLICATION_JSON})
    public ResponseEntity<Usertype> updateUsertype(@PathVariable("name") String name, @Valid @RequestBody Usertype usertype) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                Usertype usertypeOrigin = validateUsertype(name);

                if (!usertype.getName().equals(name)) {
                    usertype.setName(name);
                }

                //fetch networkZones
                Set<NetworkZone> networkZones = new HashSet<>();

                usertype.getNetworkZones().forEach(networkZone -> {
                    Optional<NetworkZone> optionalNetworkZone = networkZoneRepository.findByObjectid(networkZone.getObjectid());
                    if (!optionalNetworkZone.isPresent()) {
                        throw new ResourceNotFoundException(new Throwable("networkZone not found should be created first - name: " + networkZone.getObjectid()));
                    }
                    networkZones.add(optionalNetworkZone.get());
                });

                usertype.setName(usertypeOrigin.getName());
                usertype.setNetworkZones(networkZones);

                if (usertype.getExcludeZone() != null) {
                    Optional<NetworkZone> optionalNetworkZone = networkZoneRepository.findByObjectid(usertype.getExcludeZone().getObjectid());
                    if (!optionalNetworkZone.isPresent()) {
                        throw new ResourceNotFoundException(new Throwable("exclude networkZone not found should be created first - name: " + usertype.getExcludeZone().getObjectid()));
                    }
                    usertype.setExcludeZone(optionalNetworkZone.get());
                }

                return new ResponseEntity<>(usertypeRepository.save(usertype), HttpStatus.OK);
            } catch (ResourceNotFoundException | DataRetrievalException e) {
                LOGGER.error(e.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_UPDATE_USERTYPE, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Delete user type </p>
     *
     * @param name : Name of user type to deleted
     * @return usertype : A user type with the specified name was deleted
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping(value = "/usertypes/{name}",
            produces = {APPLICATION_JSON})
    public ResponseEntity<Usertype> deleteUsertype(@PathVariable("name") String name) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                Usertype usertype = validateUsertype(name);
                List<User> users = userRepository.findUsersByDefaultUsertypeOrActualUsertype(name);

                if (!users.isEmpty()) {
                    throw new DataDeleteViolationException("Usertype can't be deleted, it is used by other users - name: " + name);
                }

                usertypeRepository.delete(usertype);
                return new ResponseEntity<>(HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                LOGGER.error(e.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_DELETE_USERTYPE, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Find actual user type </p>
     *
     * @param username : Username of user to find
     * @return Usertype : A actual user type by username as query parameter
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('READ')")
    @GetMapping(value = "/usertypes/actualusertypes",
            produces = {"application/json"})
    public ResponseEntity<Usertype> getActualUsertype(@ApiParam(value = "username of user to find.") @Valid @RequestParam(value = "username", required = false) String username) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                Usertype usertype;
                if ((username != null) && !"".equals(username)) {
                    if ("admin".equals(request.getUserPrincipal().getName()) || username.equals(request.getUserPrincipal().getName())) {
                        Optional<User> user = userRepository.findOneByUsername(username);
                        if (!user.isPresent()) {
                            throw new ResourceNotFoundException(new Throwable(USER_NOT_EXIST));
                        }
                        usertype = user.get().getActualUsertype();
                    } else {
                        throw new ResourceNotFoundException(new Throwable("You must be admin to do this operation"));
                    }
                } else {
                    String principalName = request.getUserPrincipal().getName();
                    Optional<User> user = userRepository.findOneByUsername(principalName);
                    if (!user.isPresent()) {
                        throw new ResourceNotFoundException(new Throwable(USER_NOT_EXIST));
                    }
                    usertype = user.get().getActualUsertype();
                }
                return new ResponseEntity<>(usertype, HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Usertype>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<Usertype>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Update actual user type </p>
     *
     * @param name     : Name of actual user type to update
     * @param username : Username of user to update
     * @return Usertype : A actual user type with the specified username was updated
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('WRITE')")
    @PutMapping(value = "/usertypes/actualusertypes/{name}",
            produces = {"application/json"})
    public ResponseEntity<Usertype> updateActualUsertype(@ApiParam(value = "name of actual user type to update.", required = true) @PathVariable("name") String name,
                                                         @ApiParam(value = "username of user to update.") @Valid @RequestParam(value = "username", required = false) String username) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                Usertype usertype;
                User user;
                if ((username != null) && !"".equals(username)) {
                    if ("admin".equals(request.getUserPrincipal().getName()) || username.equals(request.getUserPrincipal().getName())) {
                        Optional<User> userOptional = userRepository.findOneByUsername(username);
                        if (!userOptional.isPresent()) {
                            throw new ResourceNotFoundException(new Throwable(USER_NOT_EXIST));
                        }
                        user = userOptional.get();

                    } else {
                        throw new ResourceNotFoundException(new Throwable("You must be admin to do this operation"));
                    }
                } else {
                    Optional<User> userOptional = userRepository.findOneByUsername(request.getUserPrincipal().getName());
                    if (!userOptional.isPresent()) {
                        throw new ResourceNotFoundException(new Throwable(USER_NOT_EXIST));
                    }
                    user = userOptional.get();
                }
                usertype = validateUsertype(name);
                user.setActualUsertype(usertype);
                userRepository.save(user);
                return new ResponseEntity<>(usertype, HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<Usertype>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<Usertype>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Gets all networkZones associated to user type </p>
     *
     * @param name : Name of user type to find
     * @return List<NetworkZone> : A list of network Zones with the specified name was found
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('READ')")
    @GetMapping(value = "/usertypes/{name}/networkZones",
            produces = {"application/json"})
    public ResponseEntity<List<NetworkZone>> getNetworkZonesByUsertype(@ApiParam(value = "name of usertype to find.", required = true) @PathVariable("name") String name) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                Optional<Usertype> usertype = usertypeRepository.findByName(name);

                if (!usertype.isPresent()) {
                    throw new ResourceNotFoundException(new Throwable(USERTYPE_NOT_EXIST));
                }

                Set<NetworkZone> networkZones = networkZoneRepository.findByUsertypes(usertype.get());

                return new ResponseEntity<>(new ArrayList<>(networkZones), HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error(NETWORKZONE_NOT_EXIST, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Creates a networkZone associated to user type </p>
     *
     * @param name        : Name of user type to updated
     * @param networkZone : The network Zone you want to post
     * @return Usertype : A user type with the specified name was updated.
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping(value = "/usertypes/{name}/networkZones",
            produces = {"application/json"})
    public ResponseEntity<Usertype> addNetworkZoneByUsertype(@ApiParam(value = "name of usertype to updated.", required = true) @PathVariable("name") String name,
                                                             @ApiParam(value = "The networkZone JSON you want to post") @Valid @RequestBody NetworkZone networkZone) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                Usertype usertype = validateUsertype(name);

                Optional<NetworkZone> networkZoneOrigin = networkZoneRepository.findByObjectid(networkZone.getObjectid());

                if (!networkZoneOrigin.isPresent()) {
                    throw new DataRetrievalException(new Throwable("NetworkZone not found should be created first - objectid: " + networkZone.getObjectid()));
                }

                Set<NetworkZone> networkZones = networkZoneRepository.findByUsertypes(usertype);

                if (!networkZones.contains(networkZoneOrigin.get())) {
                    networkZones.add(networkZoneOrigin.get());
                    usertype.setNetworkZones(networkZones);
                    usertype = usertypeRepository.save(usertype);
                }

                return new ResponseEntity<>(usertype, HttpStatus.OK);
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
     * <p> Delete networkZone by user type </p>
     *
     * @param name     : Name of user type to update
     * @param objectid : ID of networkZone to delete
     * @return Usertype : A user type with the specified name was updated.
     */
    @JsonView({Views.Public.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping(value = "/usertypes/{name}/networkZones/{objectid}",
            produces = {"application/json"})
    public ResponseEntity<Usertype> deleteNetworkZoneByUsertype(@ApiParam(value = "name of usertype to update.", required = true) @PathVariable("name") String name,
                                                                @ApiParam(value = "ID of networkZone to delete.", required = true) @PathVariable("objectid") String objectid) {
        String accept = request.getHeader(ACCEPT);
        if (accept != null && accept.contains(APPLICATION_JSON)) {
            try {
                Usertype usertype = validateUsertype(name);

                NetworkZone networkZoneOrigin = validateNetworkZone(objectid);

                Set<NetworkZone> networkZones = networkZoneRepository.findByUsertypes(usertype);

                if (!networkZones.contains(networkZoneOrigin)) {
                    throw new ResourceNotFoundException(new Throwable("The NetworkZone does not exist."));
                }

                Set<NetworkZone> newSet = new HashSet<>();
                newSet.addAll(networkZones);

                newSet.remove(networkZoneOrigin);
                usertype.setNetworkZones(newSet);
                usertypeRepository.save(usertype);

                return new ResponseEntity<>(usertype, HttpStatus.OK);
            } catch (ResourceNotFoundException | DataRetrievalException e) {
                LOGGER.error(e.getMessage());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                LOGGER.error(COULD_NOT_DELETE_NETWORK_ZONE, e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    private Usertype validateUsertype(String name) {
        Optional<Usertype> usertype = usertypeRepository.findByName(name);
        if (!usertype.isPresent()) {
            throw new ResourceNotFoundException(new Throwable(USERTYPE_NOT_EXIST));
        }
        return usertype.get();
    }

    private NetworkZone validateNetworkZone(String objectid) {
        Optional<NetworkZone> networkZone = networkZoneRepository.findByObjectid(objectid);
        if (!networkZone.isPresent()) {
            throw new DataRetrievalException(new Throwable("NetworkZone not found should be exist - objectid: " + objectid));
        }
        return networkZone.get();
    }

}

