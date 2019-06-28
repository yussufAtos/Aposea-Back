package com.rte_france.apogee.sea.server.web.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.remedials.Prioritize;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import com.rte_france.apogee.sea.server.services.logic.RemedialsListForPrioritize;
import com.rte_france.apogee.sea.server.services.prioritize.IPrioritizeRemedialsService;
import com.rte_france.apogee.sea.server.third.IThirdService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.NoSuchElementException;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-11-16T13:21:35.031+01:00")

@Controller
@Api(tags = {"remedial"})
public class RemedialApiController implements RemedialApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemedialApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    private IPrioritizeRemedialsService iPrioritizeRemedialsService;

    private com.rte_france.apogee.sea.server.third.IThirdService iThirdService;

    @org.springframework.beans.factory.annotation.Autowired
    public RemedialApiController(ObjectMapper objectMapper, HttpServletRequest request, IPrioritizeRemedialsService iPrioritizeRemedialsService,
                                 IThirdService iThirdService) {
        this.objectMapper = objectMapper;
        this.request = request;
        this.iPrioritizeRemedialsService = iPrioritizeRemedialsService;
        this.iThirdService = iThirdService;
    }

    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<Remedial>> fetchRemedials(
            @ApiParam(value = "optionally filter the result to the remedial of the specified contingencyId.")
            @Valid @RequestParam(value = "contingencyId", required = false) String contingencyId) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            List<Remedial> result;
            result = iPrioritizeRemedialsService.getRemedials(contingencyId);
            return new ResponseEntity<List<Remedial>>(result, HttpStatus.OK);
        }
        return new ResponseEntity<List<Remedial>>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteRemedials(
            @ApiParam(value = "if provided, only delete remedial of the specified id remedial.")
            @Valid @RequestParam(value = "remedialId", required = false) String remedialId) {
        String accept = request.getHeader("Accept");
        try {
            iPrioritizeRemedialsService.deleteRemedials(remedialId);
        } catch (IPrioritizeRemedialsService.PrioritizeRemedialServiceException e) {
            LOGGER.error(e.getMessage(), e);
            return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<Void> refreshRemedialsFromRepository(
            @NotNull @ApiParam(value = "optionally, the AFS computation runner ID for which candidate remedials should be refreshed.", required = true)
            @Valid @RequestParam(value = "idAfsRunner", required = true) String idAfsRunner) {
        String accept = request.getHeader("Accept");
        try {
            iThirdService.retrieveAndSaveRemedials(idAfsRunner);
        } catch (com.rte_france.apogee.sea.server.third.IThirdService.ThirdServiceException e) {
            LOGGER.error("Bad request", e);
            return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasAuthority('READ')")
    @JsonView(Views.Prioritize.class)
    public ResponseEntity<RemedialsListForPrioritize> fetchRemedialsForPrioritize(@ApiParam(value = "Id of networkcontext.", required = true) @PathVariable("networkcontextId") String networkcontextId,
                                                                                  @NotNull @ApiParam(value = "filter the result to the remedials of the specified contingencyId.", required = true)
                                                                                  @Valid @RequestParam(value = "contingencyId", required = true) String contingencyId,
                                                                                  @NotNull @ApiParam(value = "return prioritized and candidates remedials of the specified date (e.g. \"2018-11-12T11:23:18.692Z\").", required = true)
                                                                                  @Valid @RequestParam(value = "networkDate", required = true) String networkDate) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                return new ResponseEntity<RemedialsListForPrioritize>(iPrioritizeRemedialsService.getRemedialsListForPrioritize(networkDate, contingencyId, networkcontextId), HttpStatus.OK);

            } catch (IPrioritizeRemedialsService.PrioritizeRemedialServiceException e) {
                LOGGER.error("Couldn't serialize response for content type application/json", e);
                LOGGER.error(e.getMessage(), e);
                return new ResponseEntity<RemedialsListForPrioritize>(HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<RemedialsListForPrioritize>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> addPrioritizeRemedial(@ApiParam(value = "The prioritize remedial JSON you want to post")
                                                      @Valid @RequestBody List<Prioritize> prioritizeRemedial) {
        String accept = request.getHeader("Accept");
        try {
            iPrioritizeRemedialsService.savePrioritizeRemedial(prioritizeRemedial);
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        } catch (IPrioritizeRemedialsService.PrioritizeRemedialServiceException e) {
            LOGGER.error(e.getMessage(), e);
            return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
        } catch (NoSuchElementException e) {
            LOGGER.error(e.getMessage(), e);
            return new ResponseEntity<Void>(HttpStatus.CONFLICT);
        } catch (Exception e) {
            LOGGER.error("Couldn't saving prioritize remedial", e);
            return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasAuthority('READ')")
    @JsonView(Views.Prioritize.class)
    public ResponseEntity<List<Prioritize>> fetchPrioritizeRemedials(@ApiParam(value = "if provided, only return prioritized remedials of the specified date (e.g. \"2018-11-12T11:23:18.692Z\").")
                                                                     @Valid @RequestParam(value = "prioritizeDate", required = false) String prioritizeDate,
                                                                     @ApiParam(value = "optionally filter the result to the prioritize of the specified contingencyId.")
                                                                     @Valid @RequestParam(value = "contingencyId", required = false) String contingencyId) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            List<Prioritize> result;
            try {
                result = iPrioritizeRemedialsService.getPrioritizeRemedial(prioritizeDate, contingencyId);
                return new ResponseEntity<List<Prioritize>>(result, HttpStatus.OK);
            } catch (IPrioritizeRemedialsService.PrioritizeRemedialServiceException e) {
                LOGGER.error("Couldn't serialize response for content type application/json", e);
                return new ResponseEntity<List<Prioritize>>(HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<List<Prioritize>>(HttpStatus.NOT_IMPLEMENTED);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deletePrioritizeRemedials(@ApiParam(value = "Additional parameter allowing to restrict the delete to the prioritize of the specified id contingency.")
                                                          @Valid @RequestParam(value = "contingencyId", required = false) String contingencyId) {
        String accept = request.getHeader("Accept");
        iPrioritizeRemedialsService.deleteByNetworkContingency(contingencyId);
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }


}
