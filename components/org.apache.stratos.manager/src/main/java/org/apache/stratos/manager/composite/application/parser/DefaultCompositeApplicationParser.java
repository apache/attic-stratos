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

package org.apache.stratos.manager.composite.application.parser;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.composite.application.beans.*;
import org.apache.stratos.manager.composite.application.structure.CompositeAppContext;
import org.apache.stratos.manager.composite.application.structure.GroupContext;
import org.apache.stratos.manager.composite.application.structure.SubscribableContext;
import org.apache.stratos.manager.composite.application.utils.ApplicationUtils;
import org.apache.stratos.manager.exception.CompositeApplicationDefinitionException;
import org.apache.stratos.manager.exception.PersistenceManagerException;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;

import java.rmi.RemoteException;
import java.util.*;

public class DefaultCompositeApplicationParser implements CompositeApplicationParser {

    private static Log log = LogFactory.getLog(DefaultCompositeApplicationParser.class);

    DataInsertionAndRetrievalManager dataInsertionAndRetrievalMgr;

    public DefaultCompositeApplicationParser () {
        dataInsertionAndRetrievalMgr = new DataInsertionAndRetrievalManager();
    }

    @Override
    public CompositeAppContext parse(Object obj) throws CompositeApplicationDefinitionException {

        ApplicationDefinition compositeAppDefinition = null;

        if (obj instanceof ApplicationDefinition) {
            compositeAppDefinition = (ApplicationDefinition) obj;
        }

        if (compositeAppDefinition == null) {
            throw new CompositeApplicationDefinitionException("Invalid Composite Application Definition");
        }

        if (compositeAppDefinition.getAlias() == null || compositeAppDefinition.getAlias().isEmpty()) {
            throw new CompositeApplicationDefinitionException("Invalid alias specified");
        }

        if (compositeAppDefinition.getApplicationId() == null || compositeAppDefinition.getApplicationId().isEmpty()) {
            throw new CompositeApplicationDefinitionException("Invalid Composite App id specified");
        }

        // get the defined groups
        Map<String, GroupDefinition> definedGroups = getDefinedGroups(compositeAppDefinition);
        if (log.isDebugEnabled()) {
            Set<Map.Entry<String, GroupDefinition>> groupEntries = definedGroups.entrySet();
            log.debug("Defined Groups: [ ");
            for (Map.Entry<String, GroupDefinition> groupEntry : groupEntries) {
                log.debug("Group alias: " + groupEntry.getKey());
            }
            log.debug(" ]");
        }

        // get the Subscribables Information
        Map<String, SubscribableInfo> subscribablesInfo = getSubscribableInformation(compositeAppDefinition);
        if (log.isDebugEnabled()) {
            Set<Map.Entry<String, SubscribableInfo>> subscribableInfoEntries = subscribablesInfo.entrySet();
            log.debug("Defined Subscribable Information: [ ");
            for (Map.Entry<String, SubscribableInfo> subscribableInfoEntry : subscribableInfoEntries) {
                log.debug("Subscribable Information alias: " + subscribableInfoEntry.getKey());
            }
            log.debug(" ]");
        }

        if (subscribablesInfo == null) {
            throw new CompositeApplicationDefinitionException("Invalid Composite Application Definition, no Subscribable Information specified");
        }

        return buildCompositeAppStructure (compositeAppDefinition, definedGroups, subscribablesInfo);
    }

    private Map<String, GroupDefinition> getDefinedGroups (ApplicationDefinition compositeAppDefinition) throws
            CompositeApplicationDefinitionException {

        // map [group alias -> Group Definition]
        Map<String, GroupDefinition> definedGroups = null;

        if (compositeAppDefinition.getComponents() != null) {
            if (compositeAppDefinition.getComponents().getGroups() != null) {
                definedGroups = new HashMap<String, GroupDefinition>();

                for (GroupDefinition group : compositeAppDefinition.getComponents().getGroups()) {

                    // check validity of group name
                    if (group.getName() == null || group.getName().isEmpty()) {
                        throw new CompositeApplicationDefinitionException("Invalid Group name specified");
                    }

                    // check if group is deployed
                    if(!isGroupDeployed(group.getName())) {
                        throw new CompositeApplicationDefinitionException("Group with name " + group.getName() + " not deployed");
                    }

                    // check validity of group alias
                    if (group.getAlias() == null || group.getAlias().isEmpty() || !ApplicationUtils.isAliasValid(group.getAlias())) {
                        throw new CompositeApplicationDefinitionException("Invalid Group alias specified: [ " + group.getAlias() + " ]");
                    }

                    // check if a group is already defined under the same alias
                    if(definedGroups.get(group.getAlias()) != null) {
                        // a group with same alias already exists, can't continue
                        throw new CompositeApplicationDefinitionException("A Group with alias " + group.getAlias() + " already exists");
                    }

                    definedGroups.put(group.getAlias(), group);
                    if (log.isDebugEnabled()) {
                        log.debug("Added Group Definition [ " + group.getName() +" , " + group.getAlias() + " ] to map [group alias -> Group Definition]");
                    }
                }
            }
        }

        return definedGroups;
    }

    private Map<String, SubscribableInfo> getSubscribableInformation (ApplicationDefinition compositeAppDefinition) throws
            CompositeApplicationDefinitionException {

        // map [cartridge alias -> Subscribable Information]
        Map<String, SubscribableInfo> subscribableInformation = null;

        if (compositeAppDefinition.getSubscribableInfo() != null) {
            subscribableInformation = new HashMap<String, SubscribableInfo>();

            for (SubscribableInfo subscribableInfo : compositeAppDefinition.getSubscribableInfo()) {

                if (subscribableInfo.getAlias() == null || subscribableInfo.getAlias().isEmpty() ||
                        !ApplicationUtils.isAliasValid(subscribableInfo.getAlias())) {
                    throw new CompositeApplicationDefinitionException("Invalid alias specified for Subscribable Information Obj: [ " + subscribableInfo.getAlias() + " ]");
                }

                // check if a group is already defined under the same alias
                if(subscribableInformation.get(subscribableInfo.getAlias()) != null) {
                    // a group with same alias already exists, can't continue
                    throw new CompositeApplicationDefinitionException("A Subscribable Info obj with alias " + subscribableInfo.getAlias() + " already exists");
                }

                subscribableInformation.put(subscribableInfo.getAlias(), subscribableInfo);
                if (log.isDebugEnabled()) {
                    log.debug("Added Subcribables Info obj [ " + subscribableInfo.getAlias() + " ] to map [cartridge alias -> Subscribable Information]");
                }
            }
        }

        return subscribableInformation;
    }

    private boolean isGroupDeployed (String serviceGroupName) throws CompositeApplicationDefinitionException {

        try {
           return dataInsertionAndRetrievalMgr.getServiceGroupDefinition(serviceGroupName) != null;

        } catch (PersistenceManagerException e) {
            throw new CompositeApplicationDefinitionException(e);
        }

    }

    private CompositeAppContext buildCompositeAppStructure (ApplicationDefinition compositeAppDefinition,
                                                            Map<String, GroupDefinition> definedGroups,
                                                            Map<String, SubscribableInfo> subscribableInformation)
            throws CompositeApplicationDefinitionException {

        CompositeAppContext compositeAppContext = new CompositeAppContext(compositeAppDefinition.getApplicationId());

        if (compositeAppDefinition.getComponents() != null) {
            // get top level Subscribables
            if (compositeAppDefinition.getComponents().getSubscribables() != null) {
                compositeAppContext.setSubscribableContexts(getSubsribableContexts(compositeAppDefinition.getComponents().getSubscribables(),
                        subscribableInformation));
            }

            // get Groups
            if (compositeAppDefinition.getComponents().getGroups() != null) {
                compositeAppContext.setGroupContexts(getGroupContexts(compositeAppDefinition.getComponents().getGroups(),
                        subscribableInformation, definedGroups));
            }

            // get top level Dependency definitions
            if (compositeAppDefinition.getComponents().getDependencies() != null) {
            	List<String> startupOrderList = compositeAppDefinition.getComponents().getDependencies().getStartupOrders();
            	String [] startupOrders = new String [startupOrderList.size()];
            	startupOrders = startupOrderList.toArray(startupOrders);
                compositeAppContext.setStartupOrders(startupOrders);

                compositeAppContext.setKillBehaviour(compositeAppDefinition.getComponents().getDependencies().getTerminationBehaviour());
            }

            // Set application properties
            if(compositeAppDefinition.getProperty() != null) {
                Properties properties = new Properties();
                for(PropertyBean propertyBean : compositeAppDefinition.getProperty()) {
                     properties.put(propertyBean.getName(), propertyBean.getValue());
                }
                compositeAppContext.setProperties(properties);
            }
        }

        return compositeAppContext;
    }

    private Set<GroupContext> getGroupContexts (List<GroupDefinition> groupDefinitions,
                                                 Map<String, SubscribableInfo> subscribableInformation,
                                                 Map<String, GroupDefinition> definedGroups)
            throws CompositeApplicationDefinitionException {

        Set<GroupContext> groupContexts = new HashSet<GroupContext>();

        for (GroupDefinition group : groupDefinitions) {
            groupContexts.add(getGroupContext(group, subscribableInformation, definedGroups));
        }

        //Set<GroupContext> topLevelGroupContexts = getTopLevelGroupContexts(groupContexts);
        Set<GroupContext> nestedGroupContexts = new HashSet<GroupContext>();
        getNestedGroupContexts(nestedGroupContexts, groupContexts);
        filterDuplicatedGroupContexts(groupContexts, nestedGroupContexts);

        return groupContexts;
    }

//    private Set<GroupContext> getTopLevelGroupContexts (Set<GroupContext> groupContexts) {
//
//        Set<GroupContext> topLevelGroupContexts = new HashSet<GroupContext>();
//        for (GroupContext groupContext : groupContexts) {
//            topLevelGroupContexts.add(groupContext);
//        }
//
//        return topLevelGroupContexts;
//    }

    private void getNestedGroupContexts (Set<GroupContext> nestedGroupContexts, Set<GroupContext> groupContexts) {

        if (groupContexts != null) {
            for (GroupContext groupContext : groupContexts) {
                if (groupContext.getGroupContexts() != null) {
                    nestedGroupContexts.addAll(groupContext.getGroupContexts());
                    getNestedGroupContexts(nestedGroupContexts, groupContext.getGroupContexts());
                }
            }
        }
    }

    private void filterDuplicatedGroupContexts (Set<GroupContext> topLevelGroupContexts, Set<GroupContext> nestedGroupContexts) {

        for (GroupContext nestedGropCtxt : nestedGroupContexts) {
            filterNestedGroupFromTopLevel(topLevelGroupContexts, nestedGropCtxt);
        }
    }

    private void filterNestedGroupFromTopLevel (Set<GroupContext> topLevelGroupContexts, GroupContext nestedGroupCtxt) {

        Iterator<GroupContext> parentIterator = topLevelGroupContexts.iterator();
        while (parentIterator.hasNext()) {
            GroupContext parentGroupCtxt = parentIterator.next();
            // if there is an exactly similar nested Group Context and a top level Group Context
            // it implies that they are duplicates. Should be removed from top level.
            if (parentGroupCtxt.equals(nestedGroupCtxt)) {
                parentIterator.remove();
            }
        }
    }

    private GroupContext getGroupContext (GroupDefinition group, Map<String, SubscribableInfo> subscribableInformation,
                                          Map<String, GroupDefinition> definedGroups) throws CompositeApplicationDefinitionException {

        // check if are in the defined Group set
        GroupDefinition definedGroupDef = definedGroups.get(group.getAlias());
        if (definedGroupDef == null) {
            throw new CompositeApplicationDefinitionException("Group Definition with name: " + group.getName() + ", alias: " +
                    group.getAlias() + " is not found in the all Group Definitions collection");
        }

        GroupContext groupContext = new GroupContext();

        groupContext.setName(group.getName());
        groupContext.setAlias(group.getAlias());
        groupContext.setAutoscalingPolicy(group.getAutoscalingPolicy());
        groupContext.setDeploymentPolicy(group.getDeploymentPolicy());
        groupContext.setStartupOrders(getStartupOrderForGroup(group.getName()));
        groupContext.setKillBehaviour(getKillbehaviour(group.getName()));

        // get group level Subscribables
        if (group.getSubscribables() != null) {
            groupContext.setSubscribableContexts(getSubsribableContexts(group.getSubscribables(), subscribableInformation));
        }
        // get nested groups
        if (group.getSubGroups() != null) {
            Set<GroupContext> nestedGroupContexts = new HashSet<GroupContext>();
            // check sub groups
            for (GroupDefinition subGroup : group.getSubGroups()) {
                // get the complete Group Definition
                subGroup = definedGroups.get(subGroup.getAlias());
                nestedGroupContexts.add(getGroupContext(subGroup, subscribableInformation, definedGroups));
            }

            groupContext.setGroupContexts(nestedGroupContexts);
        }

        return groupContext;
    }

    private String [] getStartupOrderForGroup(String serviceGroupName) throws CompositeApplicationDefinitionException {

        ServiceGroupDefinition groupDefinition;

        try {
            groupDefinition = dataInsertionAndRetrievalMgr.getServiceGroupDefinition(serviceGroupName);

        } catch (PersistenceManagerException e) {
            throw new CompositeApplicationDefinitionException(e);
        }

        if (groupDefinition == null) {
            throw new CompositeApplicationDefinitionException("Service Group Definition not found for name " + serviceGroupName);
        }

        if (groupDefinition.getDependencies() != null) {
            if (groupDefinition.getDependencies().getStartupOrders() != null) {
                List<String> startupOrdersList = groupDefinition.getDependencies().getStartupOrders();
                String [] startupOrders = new String [startupOrdersList.size()];
                return startupOrdersList.toArray(startupOrders);
            }
        }

        return null;
    }


    private String getKillbehaviour (String serviceGroupName) throws CompositeApplicationDefinitionException {

        ServiceGroupDefinition groupDefinition;

        try {
            groupDefinition = dataInsertionAndRetrievalMgr.getServiceGroupDefinition(serviceGroupName);

        } catch (PersistenceManagerException e) {
            throw new CompositeApplicationDefinitionException(e);
        }

        if (groupDefinition == null) {
            throw new CompositeApplicationDefinitionException("Service Group Definition not found for name " + serviceGroupName);
        }

        if (groupDefinition.getDependencies() != null) {
            return groupDefinition.getDependencies().getTerminationBehaviour();
        }

        return null;

    }

    private Set<SubscribableContext> getSubsribableContexts (List<SubscribableDefinition> subscribableDefinitions,
                                                              Map<String, SubscribableInfo> subscribableInformation)
            throws CompositeApplicationDefinitionException {

        Set<SubscribableContext> subscribableContexts = new HashSet<SubscribableContext>();

        for (SubscribableDefinition subscribableDefinition : subscribableDefinitions) {
            // check is there is a related Subscribable Information
            SubscribableInfo subscribableInfo = subscribableInformation.get(subscribableDefinition.getAlias());
            if (subscribableInfo == null) {
                throw new CompositeApplicationDefinitionException("Related Subscribable Information not found for Subscribable with alias: "
                        + subscribableDefinition.getAlias());
            }

            // check if Cartridge Type is valid
            if (subscribableDefinition.getType() == null || subscribableDefinition.getType().isEmpty()) {
                throw new CompositeApplicationDefinitionException ("Invalid Cartridge Type specified : [ "
                        + subscribableDefinition.getType() + " ]");
            }

            // check if a cartridge with relevant type is already deployed. else, can't continue
            if (!isCartrigdeDeployed(subscribableDefinition.getType())) {
                throw new CompositeApplicationDefinitionException("No deployed Cartridge found with type [ " + subscribableDefinition.getType() +
                        " ] for Composite Application");
            }

            subscribableContexts.add(ParserUtils.convert(subscribableDefinition, subscribableInfo));
        }

        return subscribableContexts;
    }

    private boolean isCartrigdeDeployed (String cartridgeType) throws CompositeApplicationDefinitionException {

        CloudControllerServiceClient ccServiceClient;

        try {
            ccServiceClient = CloudControllerServiceClient.getServiceClient();

        } catch (AxisFault axisFault) {
            throw new CompositeApplicationDefinitionException(axisFault);
        }

        try {
            return ccServiceClient.getCartridgeInfo(cartridgeType) != null;

        } catch (RemoteException e) {
            throw new CompositeApplicationDefinitionException(e);

        } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
            throw new CompositeApplicationDefinitionException(e);
        }
    }

}
