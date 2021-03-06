/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.wso2.carbon.gateway.internal;

import org.apache.camel.spring.SpringCamelContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wso2.carbon.gateway.internal.mediation.camel.CamelConfigWatchAgent;
import org.wso2.carbon.gateway.internal.mediation.camel.CamelCustomRouteManager;
import org.wso2.carbon.gateway.internal.mediation.camel.CamelMediationComponent;
import org.wso2.carbon.gateway.internal.mediation.camel.CamelMediationEngine;
import org.wso2.carbon.messaging.CarbonMessageProcessor;

import java.io.File;

/**
 * OSGi Bundle Activator of the gateway Carbon component.
 */
public class GatewayActivator implements BundleActivator {

    private static final Logger log = LoggerFactory.getLogger(GatewayActivator.class);

    public static final String CAMEL_CONTEXT_CONFIG = "camel.config";
    public static final String CAMEL_CONTEXT_NAME = "camel-context.xml";
    public static final String CAMEL_CONTEXT_CONFIG_FILE =
            "conf" + File.separator + "camel" + File.separator + CAMEL_CONTEXT_NAME;

    public static final String HOT_DEPLOYMENT_CONFIG_DIRECTORY = "deployment" + File.separator + "camel";

    public void start(BundleContext bundleContext) throws Exception {
        try {
            SpringCamelContext.setNoStart(true);
            String camelDefaultConfigFile = System.getProperty(CAMEL_CONTEXT_CONFIG, CAMEL_CONTEXT_CONFIG_FILE);
            ApplicationContext applicationContext = new ClassPathXmlApplicationContext(camelDefaultConfigFile);
            SpringCamelContext camelContext = (SpringCamelContext) applicationContext.getBean("wso2-cc");
            camelContext.start();
            CamelMediationComponent component = (CamelMediationComponent) camelContext.getComponent("wso2-gw");

            CamelMediationEngine engine = component.getEngine();

            bundleContext.registerService(CarbonMessageProcessor.class, engine, null);

            // Add the routes from the custom routes config files in the repository/conf/camel directory
            CamelCustomRouteManager camelCustomRouteManager = new CamelCustomRouteManager(camelContext);

            camelCustomRouteManager.addRoutesToContext(HOT_DEPLOYMENT_CONFIG_DIRECTORY);

            // Start the watch service for the custom route file modification in the repository/conf/camel directory
            new CamelConfigWatchAgent().startWatchingForModifications(camelCustomRouteManager);

        } catch (Exception exception) {
            String msg = "Error while loading " + CAMEL_CONTEXT_CONFIG_FILE + " configuration file";
            log.error(msg + exception);
            throw new RuntimeException(msg, exception);
        }
    }

    public void stop(BundleContext bundleContext) throws Exception {

    }

}
