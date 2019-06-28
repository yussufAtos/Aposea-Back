package com.rte_france.apogee.sea.server.web.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.rte_france.apogee.sea.server.exceptions.AlreadyExistsException;
import com.rte_france.apogee.sea.server.exceptions.DataDeleteViolationException;
import com.rte_france.apogee.sea.server.exceptions.DataRetrievalException;
import com.rte_france.apogee.sea.server.exceptions.ResourceNotFoundException;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkLimitViolationRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkBaseVoltageRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkElementRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkVoltageLevelRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkZoneRepository;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.zones.NetworkBaseVoltage;
import com.rte_france.apogee.sea.server.model.zones.NetworkElement;
import com.rte_france.apogee.sea.server.model.zones.NetworkVoltageLevel;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-09-25T15:33:05.019Z")

@Controller
@Api(tags = {"zones"})
public class ZonesApiController implements ZonesApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZonesApiController.class);

    private final HttpServletRequest request;

    private NetworkBaseVoltageRepository networkBaseVoltageRepository;

    private NetworkVoltageLevelRepository networkVoltageLevelRepository;

    private NetworkZoneRepository networkZoneRepository;

    private NetworkElementRepository networkElementRepository;

    private NetworkLimitViolationRepository networkLimitViolationRepository;

    @Autowired
    public ZonesApiController(HttpServletRequest request, NetworkBaseVoltageRepository networkBaseVoltageRepository,
                              NetworkVoltageLevelRepository networkVoltageLevelRepository,
                              NetworkZoneRepository networkZoneRepository,
                              NetworkElementRepository networkElementRepository, NetworkLimitViolationRepository networkLimitViolationRepository) {
        this.request = request;
        this.networkBaseVoltageRepository = networkBaseVoltageRepository;
        this.networkVoltageLevelRepository = networkVoltageLevelRepository;
        this.networkZoneRepository = networkZoneRepository;
        this.networkElementRepository = networkElementRepository;
        this.networkLimitViolationRepository = networkLimitViolationRepository;
    }


    @JsonView({Views.BaseVoltage.class})
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<NetworkBaseVoltage>> getNetworkBaseVoltages() {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<NetworkBaseVoltage>>(networkBaseVoltageRepository.findAll(), HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkBaseVoltages", e);
                return new ResponseEntity<List<NetworkBaseVoltage>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<List<NetworkBaseVoltage>>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.BaseVoltage.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<NetworkBaseVoltage>> addNetworkBaseVoltages(
            @ApiParam(value = "The list of NetworkBaseVoltage objects to be created.") @Valid @RequestBody List<NetworkBaseVoltage> networkBaseVoltages) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            try {
                networkBaseVoltages.forEach(networkBaseVoltage -> {
                    if (networkBaseVoltageRepository.findByObjectid(networkBaseVoltage.getObjectid()).isPresent()) {
                        throw new AlreadyExistsException(new Throwable("The BaseVoltage already exists - objectid: " + networkBaseVoltage.getObjectid()));
                    }
                });

                return new ResponseEntity<>(networkBaseVoltageRepository.saveAll(networkBaseVoltages), HttpStatus.OK);
            } catch (AlreadyExistsException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Couldn't add new networkBaseVoltages", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.BaseVoltage.class})
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<NetworkBaseVoltage> getNetworkBaseVoltage(
            @ApiParam(value = "ID of the NetworkBaseVoltage.", required = true) @PathVariable("objectid") String objectid) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            Optional<NetworkBaseVoltage> networkBaseVoltage = networkBaseVoltageRepository.findByObjectid(objectid);

            if (!networkBaseVoltage.isPresent()) {
                LOGGER.error("The BaseVoltage does not exist.");
                return new ResponseEntity<NetworkBaseVoltage>(HttpStatus.NOT_FOUND);
            }

            try {
                return new ResponseEntity<NetworkBaseVoltage>(networkBaseVoltage.get(), HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkBaseVoltage", e);
                return new ResponseEntity<NetworkBaseVoltage>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<NetworkBaseVoltage>(HttpStatus.NOT_IMPLEMENTED);
    }

    @JsonView({Views.BaseVoltage.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<NetworkBaseVoltage> updateNetworkBaseVoltage(
            @ApiParam(value = "ID of the NetworkBaseVoltage to be updated.", required = true) @PathVariable("objectid") String objectid,
            @ApiParam(value = "The updated NetworkBaseVoltage.") @Valid @RequestBody NetworkBaseVoltage networkBaseVoltage) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            Optional<NetworkBaseVoltage> existingNetworkBaseVoltage = networkBaseVoltageRepository.findByObjectid(objectid);
            if (!existingNetworkBaseVoltage.isPresent()) {
                return new ResponseEntity<NetworkBaseVoltage>(HttpStatus.NOT_FOUND);
            }

            if (!networkBaseVoltage.getObjectid().equals(objectid)) {
                //if modifying objectid, are we trying to overwrite an existing one ? (this is not supported)
                Optional<NetworkBaseVoltage> overwrittenNetworkBaseVoltage = networkBaseVoltageRepository.findByObjectid(networkBaseVoltage.getObjectid());
                if (overwrittenNetworkBaseVoltage.isPresent()) {
                    return new ResponseEntity<NetworkBaseVoltage>(HttpStatus.CONFLICT);
                }
            }

            try {
                networkBaseVoltage.setUid(existingNetworkBaseVoltage.get().getUid());
                return new ResponseEntity<NetworkBaseVoltage>(networkBaseVoltageRepository.save(networkBaseVoltage), HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkBaseVoltage", e);
                return new ResponseEntity<NetworkBaseVoltage>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<NetworkBaseVoltage>(HttpStatus.NOT_IMPLEMENTED);
    }

    @JsonView({Views.BaseVoltage.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<NetworkBaseVoltage> deleteNetworkBaseVoltage(
            @ApiParam(value = "ID of the NetworkBaseVoltage to be deleted.", required = true) @PathVariable("objectid") String objectid) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            Optional<NetworkBaseVoltage> networkBaseVoltage = networkBaseVoltageRepository.findByObjectid(objectid);

            if (!networkBaseVoltage.isPresent()) {
                return new ResponseEntity<NetworkBaseVoltage>(HttpStatus.NOT_FOUND);
            }

            try {
                networkBaseVoltageRepository.delete(networkBaseVoltage.get());
                return new ResponseEntity<NetworkBaseVoltage>(HttpStatus.OK);
            } catch (DataIntegrityViolationException e) {
                throw new DataDeleteViolationException(new Throwable("The BaseVoltage is still referenced by at least one VoltageLevel"));
            } catch (Exception e) {
                LOGGER.error("Couldn't delete networkBaseVoltage", e);
                return new ResponseEntity<NetworkBaseVoltage>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<NetworkBaseVoltage>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.NetworkVoltageLevel.class})
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<NetworkVoltageLevel>> getNetworkVoltageLevels() {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<NetworkVoltageLevel>>(networkVoltageLevelRepository.findAll(), HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkVoltageLevels", e);
                return new ResponseEntity<List<NetworkVoltageLevel>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<List<NetworkVoltageLevel>>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.NetworkVoltageLevel.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<NetworkVoltageLevel>> addNetworkVoltageLevels(
            @ApiParam(value = "The list of NetworkVoltageLevel objects to be created.") @Valid @RequestBody List<NetworkVoltageLevel> networkVoltageLevels) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            try {
                for (NetworkVoltageLevel networkVoltageLevel : networkVoltageLevels) {
                    if (networkVoltageLevelRepository.findByObjectid(networkVoltageLevel.getObjectid()).isPresent()) {
                        throw new AlreadyExistsException(new Throwable("The VoltageLevel already exists - objectid: " + networkVoltageLevel.getObjectid()));
                    }

                    if (networkVoltageLevel.getBaseVoltage() != null) {
                        Optional<NetworkBaseVoltage> networkBaseVoltage = networkBaseVoltageRepository.findByObjectid(networkVoltageLevel.getBaseVoltage().getObjectid());
                        if (!networkBaseVoltage.isPresent()) {
                            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                        }

                        networkVoltageLevel.setBaseVoltage(networkBaseVoltage.get());
                    }
                }

                return new ResponseEntity<>(networkVoltageLevelRepository.saveAll(networkVoltageLevels), HttpStatus.OK);
            } catch (AlreadyExistsException | ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Couldn't add new networkVoltageLevels", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @JsonView({Views.NetworkVoltageLevel.class})
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<NetworkVoltageLevel> getNetworkVoltageLevel(
            @ApiParam(value = "ID of NetworkVoltageLevel.", required = true) @PathVariable("objectid") String objectid) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            try {
                Optional<NetworkVoltageLevel> networkVoltageLevel = validateNetworkVoltageLevel(objectid);
                return new ResponseEntity<NetworkVoltageLevel>(networkVoltageLevel.get(), HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                LOGGER.error(e.getMessage());
                return new ResponseEntity<NetworkVoltageLevel>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkVoltageLevel", e);
                return new ResponseEntity<NetworkVoltageLevel>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<NetworkVoltageLevel>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.NetworkVoltageLevel.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<NetworkVoltageLevel> updateNetworkVoltageLevel(
            @ApiParam(value = "ID of NetworkVoltageLevel to be updated.", required = true) @PathVariable("objectid") String objectid,
            @ApiParam(value = "The updated NetworkVoltageLevel.") @Valid @RequestBody NetworkVoltageLevel networkVoltageLevel) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            try {
                //fetch the existing VoltageLevel
                Optional<NetworkVoltageLevel> networkVoltageLevelOrigin = validateNetworkVoltageLevel(String.valueOf(objectid));

                //fetch NetworkBaseVoltage associated
                if (networkVoltageLevel.getBaseVoltage() != null) {
                    Optional<NetworkBaseVoltage> networkBaseVoltage = networkBaseVoltageRepository.findByObjectid(networkVoltageLevel.getBaseVoltage().getObjectid());
                    if (!networkBaseVoltage.isPresent()) {
                        LOGGER.error("The BaseVoltage does not exist.");
                        return new ResponseEntity<NetworkVoltageLevel>(HttpStatus.NOT_FOUND);
                    }
                    networkVoltageLevel.setBaseVoltage(networkBaseVoltage.get());
                }

                if (!networkVoltageLevel.getObjectid().equals(objectid)) {
                    //if modifying objectid, are we trying to overwrite an existing one ? (this is not supported)
                    Optional<NetworkVoltageLevel> overwrittenNetworkVoltageLevel = networkVoltageLevelRepository.findByObjectid(networkVoltageLevel.getObjectid());
                    if (overwrittenNetworkVoltageLevel.isPresent()) {
                        LOGGER.error("The VoltageLevel already exists.");
                        return new ResponseEntity<NetworkVoltageLevel>(HttpStatus.CONFLICT);
                    }
                }

                networkVoltageLevel.setUid(networkVoltageLevelOrigin.get().getUid());
                return new ResponseEntity<NetworkVoltageLevel>(networkVoltageLevelRepository.save(networkVoltageLevel), HttpStatus.OK);
            } catch (ResourceNotFoundException | AlreadyExistsException e) {
                LOGGER.error(e.getMessage());
                return new ResponseEntity<NetworkVoltageLevel>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkVoltageLevel", e);
                return new ResponseEntity<NetworkVoltageLevel>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<NetworkVoltageLevel>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.NetworkVoltageLevel.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<NetworkVoltageLevel> deleteNetworkVoltageLevel(
            @ApiParam(value = "ID of NetworkVoltageLevel to be deleted.", required = true) @PathVariable("objectid") String objectid) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            try {
                Optional<NetworkVoltageLevel> networkVoltageLevel = validateNetworkVoltageLevel(objectid);
                networkVoltageLevelRepository.delete(networkVoltageLevel.get());
                return new ResponseEntity<NetworkVoltageLevel>(HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                LOGGER.error(e.getMessage());
                return new ResponseEntity<NetworkVoltageLevel>(HttpStatus.NOT_FOUND);
            } catch (DataIntegrityViolationException e) {
                throw new DataDeleteViolationException(new Throwable("The VoltageLevel is still referenced by at least one NetworkZone"));
            } catch (Exception e) {
                LOGGER.error("Couldn't delete networkVoltageLevel", e);
                return new ResponseEntity<NetworkVoltageLevel>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<NetworkVoltageLevel>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Get all NetworkVoltageLevels of all the constraining elements correspond to the contexts and contingency to pass in the params </p>
     *
     * @param networkcontextId : The context Id
     * @param contingencyId    : The contingency id
     * @return
     */
    @JsonView({Views.NetworkVoltageLevel.class})
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<NetworkVoltageLevel>> getNetworkVoltageLevelsByContextAndContingency(@NotNull @ApiParam(value = "ID of network context.", required = true)
                                                                                                    @Valid @RequestParam(value = "networkcontextId",
            required = true) String networkcontextId, @NotNull @ApiParam(value = "ID of contingency.",
            required = true) @Valid @RequestParam(value = "contingencyId", required = true) String contingencyId) {

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                Set<NetworkVoltageLevel> voltageLevels = networkLimitViolationRepository.findVoltageLevelsByNetworkContextAndNetworkContingency(Long.parseLong(networkcontextId), contingencyId);
                if (voltageLevels.isEmpty()) {
                    LOGGER.error("The NetworkVoltageLevel does not exist: ");
                    return new ResponseEntity<List<NetworkVoltageLevel>>(HttpStatus.NOT_FOUND);
                }
                List<NetworkVoltageLevel> voltageLevelsList = new ArrayList<>();
                voltageLevelsList.addAll(voltageLevels);
                return new ResponseEntity<List<NetworkVoltageLevel>>(voltageLevelsList, HttpStatus.OK);

            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkVoltageLevels", e);
                return new ResponseEntity<List<NetworkVoltageLevel>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<List<NetworkVoltageLevel>>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.NetworkZone.class})
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<NetworkZone>> getNetworkZones() {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<NetworkZone>>(networkZoneRepository.findAll(), HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkZones", e);
                return new ResponseEntity<List<NetworkZone>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<List<NetworkZone>>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.NetworkZone.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<NetworkZone>> addNetworkZones(
            @ApiParam(value = "The list of NetworkZone objects to be created") @Valid @RequestBody List<NetworkZone> networkZones) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            try {
                networkZones.forEach(networkZone -> {
                    if (networkZoneRepository.findByObjectid(networkZone.getObjectid()).isPresent()) {
                        throw new AlreadyExistsException(new Throwable("The NetworkZone already exists - objectid: " + networkZone.getObjectid()));
                    }

                    Set<NetworkVoltageLevel> networkVoltageLevels = new HashSet<>();
                    networkZone.getNetworkVoltageLevels().forEach(networkVoltageLevel -> {
                        Optional<NetworkVoltageLevel> optionalNetworkVoltageLevel = validateNetworkVoltageLevel(networkVoltageLevel.getObjectid());
                        networkVoltageLevels.add(optionalNetworkVoltageLevel.get());
                    });

                    networkZone.setNetworkVoltageLevels(networkVoltageLevels);
                });

                return new ResponseEntity<>(networkZoneRepository.saveAll(networkZones), HttpStatus.OK);
            } catch (AlreadyExistsException | DataRetrievalException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Couldn't add new networkZone", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.NetworkZone.class})
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<NetworkZone> getNetworkZone(
            @ApiParam(value = "ID of NetworkZone.", required = true) @PathVariable("objectid") String objectid) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            try {
                Optional<NetworkZone> networkZone = validateNetworkZone(objectid);
                return new ResponseEntity<NetworkZone>(networkZone.get(), HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                return new ResponseEntity<NetworkZone>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkZone", e);
                return new ResponseEntity<NetworkZone>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<NetworkZone>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.NetworkZone.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<NetworkZone> updateNetworkZone(
            @ApiParam(value = "ID of NetworkZone to be updated.", required = true) @PathVariable("objectid") String objectid,
            @ApiParam(value = "The updated NetworkZone object") @Valid @RequestBody NetworkZone networkZone) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            try {
                Optional<NetworkZone> networkZoneOrigin = validateNetworkZone(String.valueOf(objectid));

                //fetch NetworkVoltageLevels associated
                Set<NetworkVoltageLevel> networkVoltageLevels = new HashSet<>();
                networkZone.getNetworkVoltageLevels().forEach(networkVoltageLevel -> {
                    Optional<NetworkVoltageLevel> optionalNetworkVoltageLevel = validateNetworkVoltageLevel(networkVoltageLevel.getObjectid());
                    networkVoltageLevels.add(optionalNetworkVoltageLevel.get());
                });

                if (!networkZone.getObjectid().equals(objectid)) {
                    //if modifying objectid, are we trying to overwrite an existing one ? (this is not supported)
                    Optional<NetworkZone> overwrittenNetworkZone = networkZoneRepository.findByObjectid(networkZone.getObjectid());
                    if (overwrittenNetworkZone.isPresent()) {
                        LOGGER.error("The NetworkZone already exists.");
                        return new ResponseEntity<NetworkZone>(HttpStatus.CONFLICT);
                    }
                }
                networkZone.setUid(networkZoneOrigin.get().getUid());
                networkZone.setNetworkVoltageLevels(networkVoltageLevels);
                return new ResponseEntity<NetworkZone>(networkZoneRepository.save(networkZone), HttpStatus.OK);
            } catch (DataRetrievalException | AlreadyExistsException | ResourceNotFoundException e) {
                LOGGER.error(e.getMessage());
                return new ResponseEntity<NetworkZone>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkZone", e);
                return new ResponseEntity<NetworkZone>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<NetworkZone>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.NetworkZone.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<NetworkZone> deleteNetworkZone(
            @ApiParam(value = "ID of NetworkZone to be deleted.", required = true) @PathVariable("objectid") String objectid) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            try {
                Optional<NetworkZone> networkZone = validateNetworkZone(objectid);
                networkZoneRepository.delete(networkZone.get());
                return new ResponseEntity<NetworkZone>(HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                LOGGER.error(e.getMessage());
                return new ResponseEntity<NetworkZone>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                LOGGER.error("Couldn't delete networkZone", e);
                return new ResponseEntity<NetworkZone>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<NetworkZone>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.NetworkVoltageLevel.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<NetworkVoltageLevel>> addNetworkVoltageLevelsToNetworkZone(
            @ApiParam(value = "ID of the NetworkZone the NetworkVoltageLevels should be added to.", required = true)
            @PathVariable("objectid") String objectid,
            @ApiParam(value = "if provided, all NetworkVoltageLevels from this other NetworkZone will be added to the NetworkZone.")
            @Valid @RequestParam(value = "otherNetworkZoneObjectId", required = false) String otherNetworkZoneObjectId,
            @ApiParam(value = "The NetworkVoltageLevel objects to be added to the NetworkZone")
            @Valid @RequestBody List<NetworkVoltageLevel> networkVoltageLevels) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                Optional<NetworkZone> networkZone = validateNetworkZone(objectid);

                networkVoltageLevels.forEach(networkVoltageLevel -> {
                    Optional<NetworkVoltageLevel> optionalNetworkVoltageLevel = validateNetworkVoltageLevel(networkVoltageLevel.getObjectid());
                    networkVoltageLevel.setUid(optionalNetworkVoltageLevel.get().getUid());
                });
                networkZone.get().addVoltageLevels(new HashSet<NetworkVoltageLevel>(networkVoltageLevels));

                if (otherNetworkZoneObjectId != null) {
                    Optional<NetworkZone> networkZoneSource = validateNetworkZone(otherNetworkZoneObjectId);
                    networkZone.get().addVoltageLevels(networkZoneSource.get().getNetworkVoltageLevels());
                }

                networkZoneRepository.save(networkZone.get());

                return new ResponseEntity<>(new ArrayList<>(networkZone.get().getNetworkVoltageLevels()), HttpStatus.OK);
            } catch (ResourceNotFoundException | DataRetrievalException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkZone", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.NetworkVoltageLevel.class})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<NetworkVoltageLevel>> deleteNetworkVoltageLevelsFromNetworkZone(
            @ApiParam(value = "ID of the NetworkZone the NetworkVoltageLevels should be removed from.", required = true)
            @PathVariable("objectid") String objectid,
            @ApiParam(value = "if provided, all NetworkVoltageLevels from this other NetworkZone will be removed from the NetworkZone.")
            @Valid @RequestParam(value = "otherNetworkZoneObjectId", required = false) String otherNetworkZoneObjectId,
            @ApiParam(value = "The NetworkVoltageLevel objects to be removed from the NetworkZone")
            @Valid @RequestBody List<NetworkVoltageLevel> networkVoltageLevels) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                Optional<NetworkZone> networkZone = validateNetworkZone(objectid);

                networkVoltageLevels.forEach(networkVoltageLevel -> {
                    Optional<NetworkVoltageLevel> optionalNetworkVoltageLevel = validateNetworkVoltageLevel(networkVoltageLevel.getObjectid());
                    networkZone.get().removeVoltageLevels(Collections.singleton(optionalNetworkVoltageLevel.get()));
                });

                if (otherNetworkZoneObjectId != null) {

                    Optional<NetworkZone> networkZoneSource = validateNetworkZone(otherNetworkZoneObjectId);
                    networkZone.get().removeVoltageLevels(networkZoneSource.get().getNetworkVoltageLevels());
                }

                networkZoneRepository.save(networkZone.get());

                return new ResponseEntity<>(new ArrayList<>(networkZone.get().getNetworkVoltageLevels()), HttpStatus.OK);
            } catch (ResourceNotFoundException | DataRetrievalException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkZone", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }


    @JsonView({Views.NetworkZone.class})
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<NetworkZone>> getNetworkZonesByNetworkVoltageLevel(
            @ApiParam(value = "ID of NetworkVoltageLevel.", required = true) @PathVariable("objectid") String objectid) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                Optional<NetworkVoltageLevel> networkVoltageLevel = validateNetworkVoltageLevel(objectid);
                Set<NetworkZone> networkZones = networkZoneRepository.findByNetworkVoltageLevels_Objectid(objectid);
                return new ResponseEntity<>(new ArrayList<>(networkZones), HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve networkVoltageLevel", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }


    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> zonesNetworkUploadPost(@ApiParam(value = "The network file to upload (must be PowSyBl IIDM gzipped format, for instance network.xiidm.gz).") @Valid @RequestPart("file") MultipartFile upfile) {
        String accept = request.getHeader("Accept");
        Map<String, NetworkVoltageLevel> newNetworkVoltageLevels = new HashMap<>();
        try {
            LOGGER.info("Deleting all existing elements ...");
            networkElementRepository.deleteAll();
            LOGGER.info("Deleting all existing elements completed");
            LOGGER.info("Loading {} ...", upfile.getOriginalFilename());

            //starting with powsybl 2.3.0, powsybl doesnt manage decompression anymore
            // -> we have to manage decompression ourselves
            // -> do not provide .xiidm.gz extension but .xiidm
            InputStream is = new GZIPInputStream(upfile.getInputStream());
            Network net = Importers.loadNetwork("dummy.xiidm", is);

            //get and insert any missing voltage level
            net.getVoltageLevelStream().forEach(vl -> {

                final String vlId = vl.getId();
                if (!networkVoltageLevelRepository.findByObjectid(vlId).isPresent() && !newNetworkVoltageLevels.containsKey(vlId)) {
                    newNetworkVoltageLevels.put(vlId, NetworkVoltageLevel.builder().objectid(vlId).build());
                }
            });
            networkVoltageLevelRepository.saveAll(newNetworkVoltageLevels.values());

            List<NetworkVoltageLevel> allVL = networkVoltageLevelRepository.findAll();
            List<NetworkElement> newNetElems = new ArrayList<>();
            Map<String, NetworkVoltageLevel> mapVL = allVL.stream().collect(Collectors.toMap(NetworkVoltageLevel::getObjectid,
                    Function.identity()));
            //get branches
            LOGGER.info("Loading {} ... branches ...", upfile.getOriginalFilename());
            net.getBranchStream().forEach(branch -> {
                String id1 = branch.getTerminal1().getVoltageLevel().getId();
                String id2 = branch.getTerminal2().getVoltageLevel().getId();
                newNetElems.add(NetworkElement.builder().objectid(branch.getId())
                        .name(branch.getName())
                        .networkVoltageLevels(new HashSet<>(Arrays.asList(mapVL.get(id1), mapVL.get(id2))))
                        .build());
            });
            //get busbars
            LOGGER.info("Loading {} ... busbars ...", upfile.getOriginalFilename());
            net.getBusbarSectionStream().forEach(busbar -> {
                newNetElems.add(NetworkElement.builder().objectid(busbar.getId())
                        .name(busbar.getTerminal().getVoltageLevel() + " " + busbar.getName())
                        .networkVoltageLevels(Collections.singleton(mapVL.get(busbar.getTerminal().getVoltageLevel().getId())))
                        .build());
            });
            //get generators
            LOGGER.info("Loading {} ... generators ...", upfile.getOriginalFilename());
            net.getGeneratorStream().forEach(generator -> {
                newNetElems.add(NetworkElement.builder().objectid(generator.getId())
                        .name(generator.getTerminal().getVoltageLevel() + " " + generator.getName())
                        .networkVoltageLevels(Collections.singleton(mapVL.get(generator.getTerminal().getVoltageLevel().getId())))
                        .build());
            });

            LOGGER.info("Loading {} ... saving ...", upfile.getOriginalFilename());
            networkElementRepository.saveAll(newNetElems);
            LOGGER.info("Loading {} completed", upfile.getOriginalFilename());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error("Couldn't upload file", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    private Optional<NetworkZone> validateNetworkZone(String objectid) {
        Optional<NetworkZone> networkZone = networkZoneRepository.findByObjectid(objectid);
        if (!networkZone.isPresent()) {
            throw new ResourceNotFoundException(new Throwable("The NetworkZone does not exist: " + objectid));
        }
        return networkZone;
    }

    private Optional<NetworkVoltageLevel> validateNetworkVoltageLevel(String objectid) {
        Optional<NetworkVoltageLevel> networkVoltageLevel = networkVoltageLevelRepository.findByObjectid(objectid);
        if (!networkVoltageLevel.isPresent()) {
            throw new ResourceNotFoundException(new Throwable("The NetworkVoltageLevel does not exist: " + objectid));
        }
        return networkVoltageLevel;
    }
}
