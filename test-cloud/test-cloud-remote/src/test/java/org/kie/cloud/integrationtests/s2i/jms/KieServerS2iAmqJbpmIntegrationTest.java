/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.cloud.integrationtests.s2i.jms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.kie.cloud.api.DeploymentScenarioBuilderFactory;
import org.kie.cloud.api.DeploymentScenarioBuilderFactoryLoader;
import org.kie.cloud.api.deployment.AmqDeployment;
import org.kie.cloud.api.scenario.GenericScenario;
import org.kie.cloud.api.settings.DeploymentSettings;
import org.kie.cloud.api.settings.builder.KieServerS2IAmqSettingsBuilder;
import org.kie.cloud.common.provider.KieServerClientProvider;
import org.kie.cloud.integrationtests.category.ApbNotSupported;
import org.kie.cloud.integrationtests.category.JBPMOnly;
import org.kie.cloud.integrationtests.category.OperatorNotSupported;
import org.kie.cloud.provider.git.Git;
import org.kie.cloud.tests.common.AbstractMethodIsolatedCloudIntegrationTest;
import org.kie.cloud.tests.common.client.util.Kjar;
import org.kie.cloud.tests.common.time.Constants;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.QueryServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
@Category({ApbNotSupported.class, OperatorNotSupported.class})
public class KieServerS2iAmqJbpmIntegrationTest extends AbstractMethodIsolatedCloudIntegrationTest<GenericScenario> {

    private static final Logger logger = LoggerFactory.getLogger(KieServerS2iAmqJbpmIntegrationTest.class);

    @Parameter(value = 0)
    public String testScenarioName;

    @Parameter(value = 1)
    public KieServerS2IAmqSettingsBuilder kieServerS2IAmqSettingsBuilder;

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        List<Object[]> scenarios = new ArrayList<>();
        DeploymentScenarioBuilderFactory deploymentScenarioFactory = DeploymentScenarioBuilderFactoryLoader
                .getInstance();

        try {
            KieServerS2IAmqSettingsBuilder kieServerS2IAmqSettings = deploymentScenarioFactory
                    .getKieServerS2IAmqSettingsBuilder();
            scenarios.add(new Object[] { "KIE Server S2I AMQ", kieServerS2IAmqSettings });
        } catch (UnsupportedOperationException ex) {
            logger.info("KIE Server AMQ S2I is skipped.", ex);
        }

        return scenarios;
    }

    protected KieServicesClient kieServicesClient;
    protected ProcessServicesClient processServicesClient;
    protected UserTaskServicesClient taskServicesClient;
    protected QueryServicesClient queryServicesClient;

    private AmqDeployment amqDeployment;

    private static final String KIE_CONTAINER_DEPLOYMENT = CONTAINER_ID + "=" + Kjar.DEFINITION.toString();

    private static final String REPO_BRANCH = "master";
    private static final String PROJECT_SOURCE_FOLDER = "/kjars-sources";

    private String repositoryName;

    @Override
    protected GenericScenario createDeploymentScenario(DeploymentScenarioBuilderFactory deploymentScenarioFactory) {
        repositoryName = Git.getProvider().createGitRepositoryWithPrefix("KieServerS2iAmqJbpmRepository",
                KieServerS2iAmqJbpmIntegrationTest.class.getResource(PROJECT_SOURCE_FOLDER).getFile());

        DeploymentSettings kieServerS2ISettings = kieServerS2IAmqSettingsBuilder
                .withContainerDeployment(KIE_CONTAINER_DEPLOYMENT)
                .withSourceLocation(Git.getProvider().getRepositoryUrl(repositoryName), REPO_BRANCH, DEFINITION_PROJECT_NAME)
                .build();

        return deploymentScenarioFactory.getGenericScenarioBuilder().withKieServer(kieServerS2ISettings).build();
    }

    @Before
    public void setUp() {
        amqDeployment = deploymentScenario.getDeployments().stream()
                .filter(AmqDeployment.class::isInstance)
                .map(AmqDeployment.class::cast)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No AMQ deployment founded."));

        kieServicesClient = KieServerClientProvider.getKieServerJmsClient(amqDeployment.getTcpUrl());
        processServicesClient = KieServerClientProvider.getProcessJmsClient(kieServicesClient);
        taskServicesClient = KieServerClientProvider.getTaskJmsClient(kieServicesClient);
    }

    @After
    public void deleteRepo() {
        Git.getProvider().deleteGitRepository(repositoryName);
    }

    @Test
    @Category(JBPMOnly.class)
    public void testContainerAfterExecServerS2IStart() {
        List<KieContainerResource> containers = kieServicesClient.listContainers().getResult().getContainers();
        assertThat(containers).isNotNull().hasSize(1);

        KieContainerResource container = containers.get(0);
        assertThat(container).isNotNull();
        assertThat(container.getContainerId()).isNotNull().isEqualTo(CONTAINER_ID);

        ReleaseId containerReleaseId = container.getResolvedReleaseId();
        assertThat(containerReleaseId).isNotNull();
        assertThat(containerReleaseId.getGroupId()).isNotNull().isEqualTo(PROJECT_GROUP_ID);
        assertThat(containerReleaseId.getArtifactId()).isNotNull().isEqualTo(DEFINITION_PROJECT_NAME);
        assertThat(containerReleaseId.getVersion()).isNotNull().isEqualTo(DEFINITION_PROJECT_VERSION);

        Long processId = processServicesClient.startProcess(CONTAINER_ID, Constants.ProcessId.USERTASK);
        assertThat(processId).isNotNull();

        List<TaskSummary> tasks = taskServicesClient.findTasks(Constants.User.YODA, 0, 10);
        assertThat(tasks).isNotNull().hasSize(1);

        taskServicesClient.completeAutoProgress(CONTAINER_ID, tasks.get(0).getId(), Constants.User.YODA, null);

        ProcessInstance userTaskPi = processServicesClient.getProcessInstance(CONTAINER_ID, processId);
        assertThat(userTaskPi).isNotNull();
        assertThat(userTaskPi.getState()).isEqualTo(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED);
    }

}
