package com.rte_france.apogee.sea.server.afs;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.powsybl.action.dsl.afs.ActionScriptExtension;
import com.powsybl.afs.*;
import com.powsybl.afs.ext.base.ImportedCaseExtension;
import com.powsybl.afs.ext.base.LocalNetworkCacheServiceExtension;
import com.powsybl.afs.mapdb.storage.MapDbAppStorage;
import com.powsybl.afs.storage.AppStorage;
import com.powsybl.afs.storage.DefaultListenableAppStorage;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.BusbarSectionContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.afs.ContingencyStoreExtension;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.import_.ImportersLoader;
import com.powsybl.iidm.import_.ImportersLoaderList;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.security.*;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import com.powsybl.security.afs.SecurityAnalysisRunnerExtension;
import com.powsybl.security.afs.SecurityAnalysisRunningService;
import com.rte_france.itesla.security.SecurityAnalysisProcessParameters;
import com.rte_france.itesla.security.SecurityAnalysisProcessResult;
import com.rte_france.itesla.security.afs.SecurityAnalysisProcessRunner;
import com.rte_france.itesla.security.afs.SecurityAnalysisProcessRunnerExtension;
import com.rte_france.itesla.security.afs.SecurityAnalysisProcessRunningService;
import com.rte_france.itesla.security.result.LimitViolationBuilder;
import com.rte_france.itesla.security.result.LimitViolations;
import com.rte_france.itesla.variant.result.*;
import com.rte_france.powsybl.shortcircuit.FaultResult;
import com.rte_france.powsybl.shortcircuit.ShortCircuitAnalysisResult;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public final class AfsForTests {

    public static ListenableAppStorage listenableAppStorage;
    static final String TEST_FS_NAME = "computations";

    private AfsForTests() {
    }

    /**
     * Creates a network importer which always returns an instance of Eurostag example 1.
     */
    public static Importer createImporterMock() {
        return new ImporterMock();
    }

    /**
     * Creates an AppData object which:
     * - has one mapdb FS in memory, named "mem"
     * - knows imported cases, action scripts, and runners
     * - does not know any service
     * - has one importer which always return the EurostagExample1 test network
     */
    public static AppData createMemAppData() {
        AppStorage storage = MapDbAppStorage.createHeap(TEST_FS_NAME);
        listenableAppStorage = new DefaultListenableAppStorage(storage);

        AppFileSystem afs = new AppFileSystem(TEST_FS_NAME, false, storage);

        ComputationManager computationManager = Mockito.mock(ComputationManager.class);

        List<AppFileSystemProvider> fsProviders = ImmutableList.of(m -> ImmutableList.of(afs));
        ImportersLoader importersLoader = new ImportersLoaderList(Collections.singletonList(createImporterMock()));
        ImportConfig importConfig = new ImportConfig();
        List<ProjectFileExtension> projectFiles = ImmutableList.of(new ImportedCaseExtension(importersLoader, importConfig),
                new ContingencyStoreExtension(),
                new ActionScriptExtension(),
                new SecurityAnalysisRunnerExtension(new SecurityAnalysisParameters()),
                new SecurityAnalysisProcessRunnerExtension(new SecurityAnalysisProcessParameters()));

        List<FileExtension> files = Collections.emptyList();
        List<ServiceExtension> services = ImmutableList.of(new SecurityAnalysisServiceExtensionMock(),
                new LocalNetworkCacheServiceExtension(), new SecurityAnalysisProcessServiceExtensionMock());

        AppData appData = new AppData(computationManager, computationManager, fsProviders, files, projectFiles, services);

        return appData;
    }


    /**
     * A mock network importer which always creates the eurostag example 1 network.
     */
    private static class ImporterMock implements Importer {

        static final String FORMAT = "net";

        @Override
        public String getFormat() {
            return FORMAT;
        }

        @Override
        public String getComment() {
            return "";
        }

        @Override
        public boolean exists(ReadOnlyDataSource dataSource) {
            return true;
        }

        @Override
        public Network importData(ReadOnlyDataSource dataSource, Properties parameters) {
            return EurostagTutorialExample1Factory.create();
        }

        @Override
        public void copy(ReadOnlyDataSource fromDataSource, DataSource toDataSource) {
        }
    }


    private static class SecurityAnalysisServiceMock implements SecurityAnalysisRunningService {
        @Override
        public void run(SecurityAnalysisRunner runner) {
            runner.writeResult(createResult());
        }
    }


    @AutoService(ServiceExtension.class)
    public static class SecurityAnalysisServiceExtensionMock implements ServiceExtension<SecurityAnalysisRunningService> {

        @Override
        public ServiceKey<SecurityAnalysisRunningService> getServiceKey() {
            return new ServiceKey<>(SecurityAnalysisRunningService.class, false);
        }

        @Override
        public SecurityAnalysisRunningService createService(ServiceCreationContext context) {
            return new SecurityAnalysisServiceMock();
        }
    }

    private static SecurityAnalysisResult createResult() {
        LimitViolationsResult preContingencyResult = new LimitViolationsResult(true, ImmutableList.of(new LimitViolation("s1", LimitViolationType.HIGH_VOLTAGE, 400.0, 1f, 440.0)));
        return new SecurityAnalysisResult(preContingencyResult, Collections.emptyList());
    }


    private static class SecurityAnalysisProcessRunningServiceMock implements SecurityAnalysisProcessRunningService {
        @Override
        public void start(SecurityAnalysisProcessRunner runner) {
            runner.writeResult(createSecurityAnalysisProcessResult());
        }

        @Override
        public void run(SecurityAnalysisProcessRunner securityAnalysisProcessRunner) {
            securityAnalysisProcessRunner.writeResult(createSecurityAnalysisProcessResult());
        }
    }

    @AutoService(ServiceExtension.class)
    public static class SecurityAnalysisProcessServiceExtensionMock implements ServiceExtension<SecurityAnalysisProcessRunningService> {

        @Autowired
        PlatformConfig platformConfig;

        @Override
        public ServiceKey<SecurityAnalysisProcessRunningService> getServiceKey() {
            return new ServiceKey<>(SecurityAnalysisProcessRunningService.class, false);
        }

        @Override
        public SecurityAnalysisProcessRunningService createService(ServiceCreationContext serviceCreationContext) {
            return new SecurityAnalysisProcessRunningServiceMock();
        }
    }

    public static SecurityAnalysisProcessResult createSecurityAnalysisProcessResult() {
        return SecurityAnalysisProcessResult.builder()
                .variantSimulatorResult(createVariantSimulatorResult())
                .shortCircuitAnalysisResult(createShortCircuitAnalysisResult())
                .build();
    }

    /**
     * Result with :
     * - 2 variants for N state, one with 2 violations, one with failed result
     * - 2 variants for contingency "NHV1_NHV2_1", one with 2 violations, one with no more violation
     */
    private static VariantSimulatorResult createVariantSimulatorResult() {
        List<LimitViolation> violations = createViolations();
        NetworkMetadata metaData = new NetworkMetadata(EurostagTutorialExample1Factory.create());
        Contingency contingency1 = new Contingency("HV line 2", singletonList(new BranchContingency("NHV1_NHV2_1")));

        // Create N state results, with 2 variants
        StateResult n = StateResult.builder()
                .initialVariant(VariantResult.builder()
                        .computationOk(true)
                        .violations(violations)
                        .build())
                .actionsResult(ActionResult.builder()
                        .ruleId("n-rule-1")
                        .variantResult(VariantResult.builder()
                                .computationOk(true)
                                .violations(violations)
                                .build())
                        .build()
                )
                .actionsResult(ActionResult.builder()
                        .ruleId("n-rule-2")
                        .variantResult(VariantResult.failed())
                        .build()
                )
                .build();

        // Create post-contingency result, with 2 variants
        StateResult cont = StateResult.builder()
                .initialVariant(VariantResult.builder()
                        .computationOk(true)
                        .violations(violations)
                        .build())
                .actionsResult(ActionResult.builder()
                        .ruleId("n-rule-3")
                        .variantResult(VariantResult.builder()
                                .computationOk(true)
                                .violations(violations)
                                .build())
                        .build()
                )
                .actionsResult(ActionResult.builder()
                        .ruleId("n-rule-4")
                        .variantResult(VariantResult.builder()
                                .computationOk(true)
                                .violations(emptyList())
                                .build())
                        .build()
                )
                .build();

        PostContingencyVariantsResult contingencyResult = new PostContingencyVariantsResult(contingency1, cont);
        return VariantSimulatorResult.builder()
                .networkMetadata(metaData)
                .preContingencyResult(n)
                .postContingencyResult(contingencyResult)
                .build();
    }

    /**
     * Creates a list of 2 limit violations
     */
    private static List<LimitViolation> createViolations() {
        LimitViolationBuilder violationBuilder = LimitViolations.current()
                .subject("NHV1_NHV2_2")
                .name("CURRENT")
                .duration(0)
                .limit(500)
                .sideOne();
        return ImmutableList.of(violationBuilder.value(667.67957f).sideOne().build(),
                violationBuilder.value(711.42523f).sideTwo().build());
    }


    public static ShortCircuitAnalysisResult createShortCircuitAnalysisResult() {
        List<FaultResult> faultResults = new ArrayList<>();
        FaultResult faultResult = new FaultResult("faultResultID", 1);
        faultResults.add(faultResult);
        List<LimitViolation> limitViolations = new ArrayList<>();
        String subjectId = "id";
        LimitViolationType limitType = LimitViolationType.HIGH_SHORT_CIRCUIT_CURRENT;
        float limit = 2000;
        float limitReduction = 1;
        float value = 2500;
        LimitViolation limitViolation = new LimitViolation(subjectId, limitType, limit, limitReduction, value);
        limitViolations.add(limitViolation);
        return new ShortCircuitAnalysisResult(faultResults, limitViolations);
    }

    public static SecurityAnalysisResult createSecurityAnalysisResult() {
        LimitViolationsResult preContingencyResult = new LimitViolationsResult(true, Collections.emptyList());

        ContingencyElement contingencyElement = new BusbarSectionContingency("NHV1_NHV2_1");
        List<ContingencyElement> elements = new ArrayList<ContingencyElement>();
        elements.add(contingencyElement);

        Contingency contingency = new Contingency("HV line 1", elements);

        LimitViolation limitViolation1 = new LimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT, "CURRENT", 0, 500.0f, 1.0f, 667.67957f, Branch.Side.ONE);
        LimitViolation limitViolation2 = new LimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT, "CURRENT", 0, 500.0f, 1.0f, 711.42523f, Branch.Side.TWO);
        List<LimitViolation> limitViolations = new ArrayList<LimitViolation>();

        limitViolations.add(limitViolation1);
        limitViolations.add(limitViolation2);

        List<String> actionsTaken = new ArrayList<String>();
        actionsTaken.add("load_shed_100");
        actionsTaken.add("load_shed_100");
        actionsTaken.add("load_shed_100");

        LimitViolationsResult limitViolationsResult = new LimitViolationsResult(true, limitViolations, actionsTaken);

        PostContingencyResult postContingencyResult = new PostContingencyResult(contingency, limitViolationsResult);

        List<PostContingencyResult> postContingencyResults = new ArrayList<PostContingencyResult>();

        postContingencyResults.add(postContingencyResult);

        SecurityAnalysisResult result = new SecurityAnalysisResult(preContingencyResult, postContingencyResults);

        return result;
    }

}
