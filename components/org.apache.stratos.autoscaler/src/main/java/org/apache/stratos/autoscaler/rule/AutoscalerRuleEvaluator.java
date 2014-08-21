/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler.rule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionLbHolder;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.*;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.Properties;

/**
 * This class is responsible for evaluating the current details of topology, statistics, and health
 * status against the rules set(written in Drools)
 */
public class AutoscalerRuleEvaluator {
	
	private static final Log log = LogFactory.getLog(AutoscalerRuleEvaluator.class);

	private static final String DRL_FILE_NAME = "mincheck.drl";
	private static final String SCALING_DRL_FILE_NAME = "scaling.drl";
	private static final String TERMINATE_ALL_DRL_FILE_NAME = "terminateall.drl";

	private static KnowledgeBase minCheckKbase;
	private static KnowledgeBase scaleCheckKbase;
	@SuppressWarnings("unused")
	private static KnowledgeBase terminateAllKbase;

    public AutoscalerRuleEvaluator(){

        minCheckKbase = readKnowledgeBase(DRL_FILE_NAME);

        if (log.isDebugEnabled()) {
            log.debug("Minimum check rule is parsed successfully");
        }

        scaleCheckKbase = readKnowledgeBase(SCALING_DRL_FILE_NAME);

        if (log.isDebugEnabled()) {
            log.debug("Scale check rule is parsed successfully");
        }
        terminateAllKbase = readKnowledgeBase(TERMINATE_ALL_DRL_FILE_NAME);

        if (log.isDebugEnabled()) {
            log.debug("Terminate all rule is parsed successfully");
        }
    }

    
    
    public static FactHandle evaluateMinCheck(StatefulKnowledgeSession ksession, FactHandle handle, Object obj) {

        if (handle == null) {

            ksession.setGlobal("$delegator", new RuleTasksDelegator());
            handle = ksession.insert(obj);
        } else {
            ksession.update(handle, obj);
        }
        ksession.fireAllRules();
        if(log.isDebugEnabled()){
            log.debug(String.format("Minimum check executed for : %s ", obj));
        }
        return handle;
    }


    public static FactHandle evaluateScaleCheck(StatefulKnowledgeSession ksession, FactHandle handle, Object obj) {

        if (handle == null) {
            ksession.setGlobal("$delegator", new RuleTasksDelegator());

            handle = ksession.insert(obj);
        } else {
            ksession.update(handle, obj);
        }
        ksession.fireAllRules();
        if(log.isDebugEnabled()){
            log.debug(String.format("Scale check executed for : %s ", obj));
        }
        return handle;
    }



    public static FactHandle evaluateTerminateAll(StatefulKnowledgeSession ksession, FactHandle handle, Object obj) {

        if (handle == null) {

            ksession.setGlobal("$delegator", new RuleTasksDelegator());
            handle = ksession.insert(obj);
        } else {
            ksession.update(handle, obj);
        }
        ksession.fireAllRules();
        if(log.isDebugEnabled()){
            log.debug(String.format("Terminate all check executed for : %s ", obj));
        }
        return handle;
    }



    public StatefulKnowledgeSession getMinCheckStatefulSession() {
        StatefulKnowledgeSession ksession;
        ksession = minCheckKbase.newStatefulKnowledgeSession();
        ksession.setGlobal("log", RuleLog.getInstance());
        return ksession;
    }
    public StatefulKnowledgeSession getScaleCheckStatefulSession() {
        StatefulKnowledgeSession ksession;
        ksession = scaleCheckKbase.newStatefulKnowledgeSession();
        ksession.setGlobal("log", RuleLog.getInstance());
        return ksession;
    }
    public StatefulKnowledgeSession getTerminateAllStatefulSession() {
        StatefulKnowledgeSession ksession;
        ksession = scaleCheckKbase.newStatefulKnowledgeSession();
        ksession.setGlobal("log", RuleLog.getInstance());
        return ksession;
    }

    public static String getLbClusterId(PartitionContext partitionContext, String nwpartitionId) {
        Properties props = partitionContext.getProperties();
        String value =
                       (String) props.get(org.apache.stratos.messaging.util.Constants.LOAD_BALANCER_REF);

        if (value == null){
            return null;
        }

        String lbClusterId = null;

        NetworkPartitionLbHolder networkPartitionLbHolder = PartitionManager.getInstance().getNetworkPartitionLbHolder(nwpartitionId);
        if (value.equals(org.apache.stratos.messaging.util.Constants.DEFAULT_LOAD_BALANCER)) {
            lbClusterId = networkPartitionLbHolder.getDefaultLbClusterId();
        } else if (value.equals(org.apache.stratos.messaging.util.Constants.SERVICE_AWARE_LOAD_BALANCER)) {
            String serviceName = partitionContext.getServiceName();
            lbClusterId = networkPartitionLbHolder.getLBClusterIdOfService(serviceName);
        }
        return lbClusterId;
    }

    private static KnowledgeBase readKnowledgeBase(String drlFileName) {
        
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        String configDir = CarbonUtils.getCarbonConfigDirPath();
        Resource resource = ResourceFactory.newFileResource(configDir + File.separator + drlFileName );
		kbuilder.add(resource, ResourceType.DRL);
        KnowledgeBuilderErrors errors = kbuilder.getErrors();
        if (errors.size() > 0) {
            for (KnowledgeBuilderError error: errors) {
                log.error(error.getMessage());
            }
            throw new IllegalArgumentException("Could not parse knowledge.");
        }
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        return kbase;
    }



}
