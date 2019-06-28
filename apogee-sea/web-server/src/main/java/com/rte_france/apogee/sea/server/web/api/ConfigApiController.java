package com.rte_france.apogee.sea.server.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rte_france.apogee.sea.server.exceptions.DataDeleteViolationException;
import com.rte_france.apogee.sea.server.exceptions.ResourceNotFoundException;
import com.rte_france.apogee.sea.server.logic.TimerangeLogic;
import com.rte_france.apogee.sea.server.model.computation.CaseCategory;
import com.rte_france.apogee.sea.server.model.computation.CaseType;
import com.rte_france.apogee.sea.server.model.dao.computation.CaseCategoryRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.CaseTypeRepository;
import com.rte_france.apogee.sea.server.model.dao.timerange.TimerangeTypeRepository;
import com.rte_france.apogee.sea.server.model.timerange.EndType;
import com.rte_france.apogee.sea.server.model.timerange.StartType;
import com.rte_france.apogee.sea.server.model.timerange.TimerangeType;
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

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-10-17T15:26:33.054Z")

@Controller
@Api(tags = {"config"})
public class ConfigApiController implements ConfigApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigApiController.class);

    @Autowired
    private CaseCategoryRepository caseCategoryRepository;

    @Autowired
    private CaseTypeRepository caseTypeRepository;

    @Autowired
    private TimerangeTypeRepository timerangeTypeRepository;

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @org.springframework.beans.factory.annotation.Autowired
    public ConfigApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<CaseType>> getCaseTypes() {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<CaseType>>(caseTypeRepository.findAll(), HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve CaseTypes", e);
                return new ResponseEntity<List<CaseType>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<List<CaseType>>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CaseType> addCaseType(@ApiParam(value = "The CaseType object to be created") @Valid @RequestBody CaseType casetype) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<CaseType>(caseTypeRepository.save(casetype), HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error("Couldn't add new CaseType", e);
                return new ResponseEntity<CaseType>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<CaseType>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<CaseType> getCaseType(@ApiParam(value = "the name of the CaseType", required = true) @PathVariable("name") String name) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                Optional<CaseType> caseType = caseTypeRepository.findById(name);

                if (caseType.isPresent()) {
                    return new ResponseEntity<CaseType>(caseType.get(), HttpStatus.OK);
                } else {
                    return new ResponseEntity<CaseType>(HttpStatus.NOT_FOUND);
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve CaseType", e);
                return new ResponseEntity<CaseType>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<CaseType>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CaseType> updateCaseType(@ApiParam(value = "the name of the CaseType to be updated", required = true) @PathVariable("name") String name, @ApiParam(value = "The updated CaseType object") @Valid @RequestBody CaseType caseType) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {

                if (caseTypeRepository.findById(String.valueOf(name)).isPresent()) {
                    return new ResponseEntity<CaseType>(caseTypeRepository.save(caseType), HttpStatus.OK);
                } else {
                    return new ResponseEntity<CaseType>(HttpStatus.NOT_FOUND);
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't update CaseType", e);
                return new ResponseEntity<CaseType>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<CaseType>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CaseType> deleteCaseType(@ApiParam(value = "the name of the CaseType to be deleted.", required = true) @PathVariable("name") String name) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {

                Optional<CaseType> caseType = caseTypeRepository.findById(name);

                if (caseType.isPresent()) {
                    caseType.get().setCaseCategory(null);
                    caseTypeRepository.delete(caseType.get());
                    return new ResponseEntity<CaseType>(HttpStatus.OK);
                } else {
                    return new ResponseEntity<CaseType>(HttpStatus.NOT_FOUND);
                }
            } catch (DataIntegrityViolationException e) {
                throw new DataDeleteViolationException(new Throwable("The CaseType is still referenced by at least one NetworkContext"));
            } catch (Exception e) {
                LOGGER.error("Couldn't delete CaseType", e);
                return new ResponseEntity<CaseType>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<CaseType>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<CaseCategory>> getCaseCategories() {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<CaseCategory>>(caseCategoryRepository.findAll(), HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve CaseCategories", e);
                return new ResponseEntity<List<CaseCategory>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<List<CaseCategory>>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CaseCategory> addCaseCategory(@ApiParam(value = "The CaseCategory object to be created") @Valid @RequestBody CaseCategory casecategory) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<CaseCategory>(caseCategoryRepository.save(casecategory), HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error("Couldn't add new CaseCategory", e);
                return new ResponseEntity<CaseCategory>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<CaseCategory>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<CaseCategory> getCaseCategory(@ApiParam(value = "the name of the CaseCategory", required = true) @PathVariable("name") String name) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                Optional<CaseCategory> caseCategory = caseCategoryRepository.findById(name);

                if (caseCategory.isPresent()) {
                    return new ResponseEntity<CaseCategory>(caseCategory.get(), HttpStatus.OK);
                } else {
                    return new ResponseEntity<CaseCategory>(HttpStatus.NOT_FOUND);
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve CaseCategory", e);
                return new ResponseEntity<CaseCategory>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<CaseCategory>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CaseCategory> updateCaseCategory(@ApiParam(value = "the name of the CaseCategory to be updated", required = true) @PathVariable("name") String name, @ApiParam(value = "The updated CaseCategory object") @Valid @RequestBody CaseCategory caseCategory) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {

                if (caseCategoryRepository.findById(String.valueOf(name)).isPresent()) {
                    return new ResponseEntity<CaseCategory>(caseCategoryRepository.save(caseCategory), HttpStatus.OK);
                } else {
                    return new ResponseEntity<CaseCategory>(HttpStatus.NOT_FOUND);
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't update CaseCategory", e);
                return new ResponseEntity<CaseCategory>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<CaseCategory>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CaseCategory> deleteCaseCategory(@ApiParam(value = "the name of the CaseCategory to be deleted", required = true) @PathVariable("name") String name) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            Optional<CaseCategory> caseCategory = caseCategoryRepository.findById(name);

            if (caseCategory.isPresent()) {
                if (!caseCategory.get().getCaseTypes().isEmpty()) {
                    throw new DataDeleteViolationException(new Throwable("The CaseCategory is still referenced by at least one CaseType"));
                }
                caseCategoryRepository.delete(caseCategory.get());
                return new ResponseEntity<CaseCategory>(HttpStatus.OK);
            } else {
                return new ResponseEntity<CaseCategory>(HttpStatus.NOT_FOUND);
            }
        }

        return new ResponseEntity<CaseCategory>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Gets all time range </p>
     *
     * @return List<TimerangeType> : List all the existing time range
     */
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<TimerangeType>> getTimerangeTypes() {

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<List<TimerangeType>>(timerangeTypeRepository.findAll(), HttpStatus.OK);
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve TimerangeTypes", e);
                return new ResponseEntity<List<TimerangeType>>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<List<TimerangeType>>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TimerangeType> addTimerangeType(@ApiParam(value = "The TimerangeType object to be created") @Valid @RequestBody TimerangeLogic timerangeLogic) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                if (timerangeLogic.getStartType() == null || timerangeLogic.getEndType() == null) {
                    LOGGER.error("Couldn't add new TimerangeType, The startType or the endType are wrong");
                    return new ResponseEntity<TimerangeType>(HttpStatus.BAD_REQUEST);
                }
                TimerangeType timerangeType = validateTimerange(timerangeLogic);
                if (timerangeType == null) {
                    LOGGER.error("Couldn't add new TimerangeType, The time range is wrong");
                    return new ResponseEntity<TimerangeType>(HttpStatus.BAD_REQUEST);
                }
                return new ResponseEntity<TimerangeType>(timerangeTypeRepository.save(timerangeType), HttpStatus.OK);
            } catch (ResourceNotFoundException e) {
                LOGGER.error("Couldn't update TimerangeType", e);
                return new ResponseEntity<TimerangeType>(HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                LOGGER.error("Couldn't add new TimerangeType", e);
                return new ResponseEntity<TimerangeType>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<TimerangeType>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Find time range </p>
     *
     * @param name : Name of time range to find
     * @return TimerangeType : A time range with the specified name was found
     */
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<TimerangeType> getTimerangeType(@ApiParam(value = "the name of the TimerangeType", required = true) @PathVariable("name") String name) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                Optional<TimerangeType> timerangeType = timerangeTypeRepository.findById(name);

                if (timerangeType.isPresent()) {
                    return new ResponseEntity<TimerangeType>(timerangeType.get(), HttpStatus.OK);
                } else {
                    return new ResponseEntity<TimerangeType>(HttpStatus.NOT_FOUND);
                }
            } catch (Exception e) {
                LOGGER.error("Couldn't retrieve TimerangeType", e);
                return new ResponseEntity<TimerangeType>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<com.rte_france.apogee.sea.server.model.timerange.TimerangeType>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Update time range </p>
     *
     * @param name          : Update time range
     * @param timerangeType : The time range you want to post
     * @return timerangeType : A time range with the specified name was updated
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TimerangeType> updateTimerangeType(@ApiParam(value = "the name of the TimerangeType to be updated", required = true) @PathVariable("name") String name, @ApiParam(value = "The updated TimerangeType object") @Valid @RequestBody TimerangeLogic timerangeLogic) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                if (timerangeTypeRepository.findById(String.valueOf(name)).isPresent()) {
                    if (timerangeLogic.getStartType() == null || timerangeLogic.getEndType() == null) {
                        LOGGER.error("Couldn't add new TimerangeType, The startType or the endType are wrong");
                        return new ResponseEntity<TimerangeType>(HttpStatus.BAD_REQUEST);
                    }
                    TimerangeType timerangeType = validateTimerange(timerangeLogic);
                    if (timerangeType == null) {
                        LOGGER.error("Couldn't add new TimerangeType, The time range is wrong");
                        return new ResponseEntity<TimerangeType>(HttpStatus.BAD_REQUEST);
                    }

                    return new ResponseEntity<TimerangeType>(timerangeTypeRepository.save(timerangeType), HttpStatus.OK);
                } else {
                    return new ResponseEntity<TimerangeType>(HttpStatus.NOT_FOUND);
                }
            } catch (ResourceNotFoundException e) {
                LOGGER.error("Couldn't update TimerangeType", e);
                return new ResponseEntity<TimerangeType>(HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                LOGGER.error("Couldn't update TimerangeType", e);
                return new ResponseEntity<TimerangeType>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<TimerangeType>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p> Delete time range </p>
     *
     * @param name : Name of time range to deleted
     * @return TimerangeType : A time range with the specified name was deleted
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<TimerangeType> deleteTimerangeType(@ApiParam(value = "the name of the TimerangeType to be deleted", required = true) @PathVariable("name") String name) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {

            Optional<TimerangeType> timerangeType = timerangeTypeRepository.findById(name);
            if (timerangeType.isPresent()) {
                timerangeTypeRepository.delete(timerangeType.get());
                return new ResponseEntity<TimerangeType>(HttpStatus.OK);
            } else {
                return new ResponseEntity<TimerangeType>(HttpStatus.NOT_FOUND);
            }
        }

        return new ResponseEntity<TimerangeType>(HttpStatus.NOT_IMPLEMENTED);
    }

    private TimerangeType validateTimerange(TimerangeLogic timerangeLogic) {
        TimerangeType timerangeType = new TimerangeType();

        if (timerangeLogic.getStartType().equals(StartType.NOW)) {
            if (timerangeLogic.getEndType().equals(EndType.HOURRELATIVE)) {
                if (timerangeLogic.getEndTimeHour() == null || timerangeLogic.getStartTimeMinutes() == null) {
                    return null;
                } else {
                    timerangeType.setEndTimeHour(timerangeLogic.getEndTimeHour());
                    timerangeType.setStartTimeMinutes(timerangeLogic.getStartTimeMinutes());
                }
            } else if (timerangeLogic.getEndType().equals(EndType.MIDNIGHT)) {
                if (timerangeLogic.getEndTimeDay() == null || timerangeLogic.getAlternateIfLessHoursThan() == null || timerangeLogic.getAlternateTimerange() == null || timerangeLogic.getStartTimeMinutes() == null) {
                    return null;
                }
                Optional<TimerangeType> timerangeAlternate = timerangeTypeRepository.findById(timerangeLogic.getAlternateTimerange());
                if (!timerangeAlternate.isPresent()) {
                    return null;
                } else {
                    timerangeType.setStartTimeMinutes(timerangeLogic.getStartTimeMinutes());
                    timerangeType.setAlternateIfLessHoursThan(timerangeLogic.getAlternateIfLessHoursThan());
                    timerangeType.setEndTimeDay(timerangeLogic.getEndTimeDay());
                    timerangeType.setAlternateTimerange(timerangeAlternate.get());
                }
            } else {
                return null;
            }
        } else if (timerangeLogic.getStartType().equals(StartType.MIDNIGHT)) {
            if (timerangeLogic.getStartTimeDay() == null || timerangeLogic.getEndTimeDay() == null) {
                return null;
            } else {
                timerangeType.setStartTimeDay(timerangeLogic.getStartTimeDay());
                timerangeType.setEndTimeDay(timerangeLogic.getEndTimeDay());
            }
        } else {
            return null;
        }
        String[] availableZoneIds = TimeZone.getAvailableIDs();
        List<String> availableZoneIdsList = Arrays.asList(availableZoneIds);
        boolean zoneOk = availableZoneIdsList.contains(timerangeLogic.getTimeZone());
        if (!zoneOk) {
            return null;
        }
        timerangeType.setTimeZone(timerangeLogic.getTimeZone());
        timerangeType.setName(timerangeLogic.getName());
        timerangeType.setStartType(timerangeLogic.getStartType());
        timerangeType.setEndType(timerangeLogic.getEndType());
        return timerangeType;

    }
}
