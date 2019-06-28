package com.rte_france.apogee.sea.server.web.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rte_france.apogee.sea.server.exceptions.ResourceNotFoundException;
import com.rte_france.apogee.sea.server.model.computation.AbstractComputationResult;
import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.computation.logic.LimitViolationByIdenfifierAndRemedials;
import com.rte_france.apogee.sea.server.model.computation.logic.SnapshotResult;
import com.rte_france.apogee.sea.server.model.dao.computation.*;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotContextRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotContingencyContextRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotContingencyRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotRepository;
import com.rte_france.apogee.sea.server.model.dao.user.UserRepository;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshot;
import com.rte_france.apogee.sea.server.model.user.User;
import com.rte_france.apogee.sea.server.model.user.Usertype;
import com.rte_france.apogee.sea.server.services.computation.IComputationService;
import com.rte_france.apogee.sea.server.services.exceptions.SnapshotNotFoundException;
import com.rte_france.apogee.sea.server.third.IThirdService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-08-16T16:24:45.457+02:00")

@Controller
@Api(tags = {"computation"})
public class ComputationApiController implements ComputationApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputationApiController.class);

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    private final WebRequest webRequest;

    @Autowired
    private NetworkContextRepository networkContextRepository;

    @Autowired
    private ComputationResultRepository computationResultRepository;

    @Autowired
    private NetworkPostContingencyResultRepository networkPostContingencyResultRepository;

    @Autowired
    private NetworkLimitViolationsResultRepository networkLimitViolationsResultRepository;

    @Autowired
    private CaseTypeRepository caseTypeRepository;

    @Autowired
    private CaseCategoryRepository caseCategoryRepository;

    @Autowired
    private IComputationService iComputationService;

    @Autowired
    private IThirdService iThirdService;

    @Autowired
    private UiSnapshotContingencyRepository uiSnapshotContingencyRepository;

    @Autowired
    private UiSnapshotContingencyContextRepository uiSnapshotContingencyContextRepository;

    @Autowired
    private UiSnapshotContextRepository uiSnapshotContextRepository;

    @Autowired
    private UiSnapshotRepository uiSnapshotRepository;

    @Autowired
    UserRepository userRepository;

    public ComputationApiController(ObjectMapper objectMapper, HttpServletRequest request, WebRequest webRequest) {
        this.objectMapper = objectMapper;
        this.request = request;
        this.webRequest = webRequest;
    }


    /**
     * <p>Get all network contexts.</p>
     *
     * @param caseType optionally filter the result to the contexts of the specified caseType.
     * @return A list of network contexts.
     */
    @JsonView(Views.NetworkContext.class)
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<NetworkContext>> getNetworkContexts(
            @ApiParam(value = "optionally filter the result to the contexts of the specified caseType.")
            @Valid @RequestParam(value = "caseType", required = false) String caseType) {

        List<NetworkContext> res;
        if ((caseType != null) && !"".equals(caseType)) {
            res = networkContextRepository.findByCaseType(caseType);
        } else {
            res = networkContextRepository.findAll();
        }
        return new ResponseEntity<List<NetworkContext>>(res, HttpStatus.OK);
    }


    /**
     * <p>Delete all or one computation context.</p>
     *
     * @param idAfsImportedCase If provided, only the context of the specified AFS ImportedCase ID will be deleted.
     * @return textual summary of the action
     */
    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> deleteNetworkContexts(
            @ApiParam(value = "If provided, only the context of the specified AFS ImportedCase ID will be deleted.")
            @Valid @RequestParam(value = "idAfsImportedCase", required = false) String idAfsImportedCase) {

        String res;
        if ((idAfsImportedCase != null) && !"".equals(idAfsImportedCase)) {
            Optional<NetworkContext> networkContextOptional = networkContextRepository.findByIdAfsImportedCase(idAfsImportedCase);
            if (networkContextOptional.isPresent()) {
                uiSnapshotContingencyContextRepository.deleteUiSnapshotContingencyContextByNetworkContext(networkContextOptional.get().getId());
                uiSnapshotContingencyContextRepository.deleteUiSnapshotContextByNetworkContext(networkContextOptional.get().getId());
                networkContextRepository.delete(networkContextOptional.get());
                res = "The context of the specified afs id imported case is deleted";
            } else {
                res = "The context of the specified afs id imported case is not deleted";
            }
        } else {
            List<NetworkContext> networkContexts = networkContextRepository.findAll();
            networkContexts.forEach(networkContext -> {
                uiSnapshotContingencyContextRepository.deleteUiSnapshotContingencyContextByNetworkContext(networkContext.getId());
                uiSnapshotContingencyContextRepository.deleteUiSnapshotContextByNetworkContext(networkContext.getId());
            });

            networkContextRepository.deleteAll();
            res = "The contexts are deleted";
        }
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }


    /**
     * <p>Get the last version of the network contexts. The last version is determined by the computation date.</p>
     * <p>Given a {computation type ; network date} tuple, multiple versions the network contexts may exist.</p>
     * <p>All these versions differ by their computation date only.</p>
     * <p>For example the may be multiple versions of today's 7pm peak forecast.</p>
     * <p>This API will return only the network context having the most recent computation date.</p>
     *
     * @param caseType optionally filter the result to the contexts of the specified caseType.
     * @return A list of network contexts.
     */
    @JsonView(Views.NetworkContext.class)
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<NetworkContext>> getLastNetworkContexts(
            @ApiParam(value = "optionally filter the result to the contexts of the specified caseType.")
            @Valid @RequestParam(value = "caseType", required = false) String caseType) {

        List<NetworkContext> res;
        if ((caseType != null) && !"".equals(caseType)) {
            res = networkContextRepository.findLatestByCaseType(caseType);
        } else {
            res = networkContextRepository.findLatestNetworkContexts();
        }
        return new ResponseEntity<List<NetworkContext>>(res, HttpStatus.OK);
    }


    /**
     * <p>Get the last version of the network contexts using Case Category priorities.</p>
     * <p>For a given Case Category and Case Type. The last version is determined by the computation date.</p>
     * <p>For multiple network contexts with the same network date but different case categories,</p>
     * <p>the priority order configured in the case category is applied.</p>
     *
     * @return A list of network contexts.
     */
    @JsonView({Views.ComputationResults.class})
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<NetworkContext>> getLastNetworkContextsWithPriority() {
        List<NetworkContext> networkContexts = iComputationService.fetchLastNetworkContextsWithPriority();
        return new ResponseEntity<List<NetworkContext>>(networkContexts, HttpStatus.OK);
    }


    /**
     * <p>Get the computation results of the provided AFS computation runner ID.</p>
     *
     * @param idAfsRunner The AFS computation runner ID.
     * @return A computation result.
     */
    @JsonView({Views.ComputationResults.class})
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<AbstractComputationResult> getComputationResult(
            @ApiParam(value = "The AFS computation runner ID.", required = true)
            @PathVariable("idAfsRunner") String idAfsRunner) {

        Optional<AbstractComputationResult> computationResultOptional = computationResultRepository.findByIdAfsRunner(idAfsRunner);
        return new ResponseEntity<AbstractComputationResult>(
                computationResultOptional.orElseThrow(() -> new ResourceNotFoundException("Could not find any AFS computation runner with id='" + idAfsRunner + "'")),
                HttpStatus.OK);
    }


    /**
     * <p>Creates a new snapshot of the current computation results, for use in the User Interface.</p>
     *
     * @return nothing
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> createUiSnapshot() {
        String accept = request.getHeader("Accept");
        iComputationService.insertDataSetInUiSnapshot();
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }


    /**
     * <p>Get computation results from a snapshot.</p>
     * <p>Either the latest snapshot, or a specified snapshot can be retrieved.</p>
     * <p>Results are grouped and paginated by contingencies.</p>
     *
     * @param page       page number to retrieve, starts at page 1.
     * @param size       page size.
     * @param snapshotid if provided, use the specified snapshot ID, otherwise use the latest snapshot available.
     * @param zones      comma delimited list of zones to filter.
     * @param timerange  if provided, use the specified timerange, otherwise use the time range that returns everything.
     * @param exclude    if its value is true, the exclusions filter is applied.
     * @return snapshot results.
     */
    @PreAuthorize("hasAuthority('READ')")
    @JsonView(Views.UiSnapshot.class)
    public ResponseEntity<SnapshotResult> getUiSnapshotResults(
            @NotNull @ApiParam(value = "Numéro de page à récupérer.", required = true) @Valid @RequestParam(value = "page", required = true) Integer page,
            @NotNull @ApiParam(value = "Taille de page à récupérer.", required = true) @Valid @RequestParam(value = "size", required = true) Integer size,
            @ApiParam(value = "if provided, use the specified snapshot ID, otherwise use the latest snapshot available")
            @Valid @RequestParam(value = "snapshotid", required = false) String snapshotid,
            @ApiParam(value = "list of zones to filter") @Valid @RequestParam(value = "zones", required = false) List<String> zones,
            @ApiParam(value = "if provided, use the specified timerange, otherwise use the time range that returns everything \"Tout\"")
            @Valid @RequestParam(value = "timerange", required = false) String timerange, @ApiParam(value = "True, exlude is active.")
            @Valid @RequestParam(value = "exclude", required = false) Boolean exclude) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                //Find the latest snapshot
                Optional<UiSnapshot> uiSnapshotOptional = uiSnapshotRepository.findLatestUiSnapshot();
                UiSnapshot uiSnapshot = uiSnapshotOptional.orElseThrow(() -> new SnapshotNotFoundException("The snapshot id " + snapshotid + " does not exist in the database"));
                Long uiSnapshotId = uiSnapshot.getId();

                // To build eTag with uisnapshotId and user name
                StringBuilder eTag = new StringBuilder();
                eTag.append(Long.toString(uiSnapshotId));
                eTag.append(" " + auth.getName());

                if (webRequest.checkNotModified(eTag.toString())) {
                    return null;
                }

                SnapshotResult sr = iComputationService.getMapNetworkContextByContingency(auth.getName(), page, size, snapshotid, zones, timerange, exclude.booleanValue());
                return ResponseEntity.ok()
                        .eTag(eTag.toString())
                        .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS))
                        .body(sr);
            } catch (com.rte_france.apogee.sea.server.services.computation.IComputationService.ComputationServiceException e) {
                return new ResponseEntity<SnapshotResult>(HttpStatus.BAD_REQUEST);
            } catch (SnapshotNotFoundException e) {
                throw e;
            }
        }
        return new ResponseEntity<com.rte_france.apogee.sea.server.model.computation.logic.SnapshotResult>(HttpStatus.NOT_IMPLEMENTED);
    }


    /**
     * <p>Get the computation results for a specific UI snapshot and Contingency.</p>
     *
     * @param snapshotid    The UI snapshot ID.
     * @param contingencyid The contingency ID.
     * @param contextsId    The list of network context ID.
     * @param exclude       if its value is true, the exclusions filter is applied.
     * @return contingency results.
     */
    @PreAuthorize("hasAuthority('READ')")
    @JsonView(Views.Public.class)
    public ResponseEntity<LimitViolationByIdenfifierAndRemedials> getUiSnapshotContingencyResults(
            @ApiParam(value = "The UI snapshot ID.", required = true)
            @PathVariable("snapshotid") String snapshotid,
            @NotNull @ApiParam(value = "The contingency ID.", required = true)
            @Valid @RequestParam(value = "contingencyid", required = true) String contingencyid,
            @NotNull @ApiParam(value = "list of Network contexts ID", required = true)
            @Valid @RequestParam(value = "contextsId", required = true) List<String> contextsId, @ApiParam(value = "True, exlude is active.")
            @Valid @RequestParam(value = "exclude", required = false) Boolean exclude) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                //user check
                Optional<User> user = this.userRepository.findOneByUsername(auth.getName());
                Usertype actualUsertype = null;
                if (user.isPresent() && user.get().getActualUsertype().getExcludeZone() != null) {
                    actualUsertype = user.get().getActualUsertype();
                }

                return new ResponseEntity<LimitViolationByIdenfifierAndRemedials>(iComputationService.getLimitViolationsByContingency(actualUsertype, contingencyid, snapshotid, contextsId, exclude.booleanValue()), HttpStatus.OK);
            } catch (com.rte_france.apogee.sea.server.services.computation.IComputationService.ComputationServiceException e) {
                return new ResponseEntity<LimitViolationByIdenfifierAndRemedials>(HttpStatus.BAD_REQUEST);
            } catch (SnapshotNotFoundException e) {
                throw e;
            }
        }

        return new ResponseEntity<LimitViolationByIdenfifierAndRemedials>(HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * <p>Upload manually an iTesla Security Analysis Result file (JSON) and remedial (JSON).</p>
     * <p>Used primarily for testing purposes (avoid having to integrate AFS and iTesla).</p>
     * <p>File name should be caseType_networkDate_computationDate.json</p>
     * <p>For example: pf_20190110T0800Z_20190110T0800Z.json or srj-ij_20190110T1230Z_20190110T0800Z.json</p>
     * <p>For example remedialJson:
     * {"BXLIEL61SIRMI": [
     * {
     * "idLogicContext": 689,
     * "idAbstractLogic": 8212,
     * "shortDescription": "Ouverture SIRML4ZTREZ.1"
     * },
     * {
     * "idLogicContext": 688,
     * "idAbstractLogic": 8210,
     * "shortDescription": "Ouverture BXLIEL41LUCON"
     * }
     * ]
     * }</p>
     *
     * @param saresult The json file of computation result
     * @param remedial The json file of remedial
     * @return
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> uploadComputationResult(@ApiParam(value = "The security analysis result file") @Valid @RequestPart("saresult") MultipartFile saresult,
                                                        @ApiParam(value = "The remedials file") @Valid @RequestPart(value = "remedial") MultipartFile remedial) {
        String accept = request.getHeader("Accept");
        try {
            iThirdService.saveComputationResult(saresult, remedial);
        } catch (com.rte_france.apogee.sea.server.third.IThirdService.ThirdServiceException e) {
            LOGGER.error("ThirdServiceException:", e);
            return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }
}
