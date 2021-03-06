/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.cloud.openshift.scenario.builder;

import java.util.HashMap;
import java.util.Map;

import org.kie.cloud.api.deployment.constants.DeploymentConstants;
import org.kie.cloud.api.scenario.WorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenario;
import org.kie.cloud.api.scenario.builder.WorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenarioBuilder;
import org.kie.cloud.openshift.constants.OpenShiftConstants;
import org.kie.cloud.openshift.constants.OpenShiftTemplateConstants;
import org.kie.cloud.openshift.scenario.WorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenarioImpl;

public class WorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenarioBuilderImpl implements WorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenarioBuilder {

    private final Map<String, String> envVariables = new HashMap<>();

    public WorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenarioBuilderImpl() {
        envVariables.put(OpenShiftTemplateConstants.KIE_SERVER_USER, DeploymentConstants.getKieServerUser());
        envVariables.put(OpenShiftTemplateConstants.KIE_SERVER_PWD, DeploymentConstants.getKieServerPassword());
        envVariables.put(OpenShiftTemplateConstants.KIE_ADMIN_USER, DeploymentConstants.getWorkbenchUser());
        envVariables.put(OpenShiftTemplateConstants.KIE_ADMIN_PWD, DeploymentConstants.getWorkbenchPassword());
        envVariables.put(OpenShiftTemplateConstants.BUSINESS_CENTRAL_HTTPS_SECRET, OpenShiftConstants.getKieApplicationSecretName());
        envVariables.put(OpenShiftTemplateConstants.KIE_SERVER_HTTPS_SECRET, OpenShiftConstants.getKieApplicationSecretName());
    }

    @Override
    public WorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenario build() {
        return new WorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenarioImpl(envVariables);
    }

    @Override
    public WorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenarioBuilder withExternalMavenRepo(String repoUrl, String repoUserName, String repoPassword) {
        envVariables.put(OpenShiftTemplateConstants.MAVEN_REPO_URL, repoUrl);
        envVariables.put(OpenShiftTemplateConstants.MAVEN_REPO_USERNAME, repoUserName);
        envVariables.put(OpenShiftTemplateConstants.MAVEN_REPO_PASSWORD, repoPassword);
        return this;
    }

    @Override
    public WorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenarioBuilder withSmartRouterId(String smartRouterId) {
        envVariables.put(OpenShiftTemplateConstants.KIE_SERVER_ROUTER_ID, smartRouterId);
        return this;
    }
}
