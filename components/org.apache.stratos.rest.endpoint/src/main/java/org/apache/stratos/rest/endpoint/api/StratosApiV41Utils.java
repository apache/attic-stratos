/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.rest.endpoint.api;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.*;
import org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy;
import org.apache.stratos.autoscaler.stub.deployment.policy.ApplicationPolicy;
import org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.stub.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.stub.pojo.ServiceGroup;
import org.apache.stratos.cloud.controller.stub.*;
import org.apache.stratos.cloud.controller.stub.domain.Cartridge;
import org.apache.stratos.cloud.controller.stub.domain.NetworkPartition;
import org.apache.stratos.cloud.controller.stub.domain.Partition;
import org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster;
import org.apache.stratos.common.beans.IaasProviderInfoBean;
import org.apache.stratos.common.beans.PropertyBean;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.beans.UserInfoBean;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.application.ApplicationNetworkPartitionIdListBean;
import org.apache.stratos.common.beans.application.ComponentBean;
import org.apache.stratos.common.beans.application.domain.mapping.ApplicationDomainMappingsBean;
import org.apache.stratos.common.beans.application.domain.mapping.DomainMappingBean;
import org.apache.stratos.common.beans.application.signup.ApplicationSignUpBean;
import org.apache.stratos.common.beans.artifact.repository.GitNotificationPayloadBean;
import org.apache.stratos.common.beans.cartridge.*;
import org.apache.stratos.common.beans.kubernetes.KubernetesClusterBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesHostBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesMasterBean;
import org.apache.stratos.common.beans.partition.NetworkPartitionBean;
import org.apache.stratos.common.beans.partition.NetworkPartitionReferenceBean;
import org.apache.stratos.common.beans.partition.PartitionReferenceBean;
import org.apache.stratos.common.beans.policy.autoscale.AutoscalePolicyBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.common.beans.topology.ApplicationInfoBean;
import org.apache.stratos.common.beans.topology.ApplicationInstanceBean;
import org.apache.stratos.common.beans.topology.ClusterBean;
import org.apache.stratos.common.beans.topology.GroupInstanceBean;
import org.apache.stratos.common.client.AutoscalerServiceClient;
import org.apache.stratos.common.client.CloudControllerServiceClient;
import org.apache.stratos.common.client.StratosManagerServiceClient;
import org.apache.stratos.common.exception.InvalidEmailException;
import org.apache.stratos.common.util.ClaimsMgtUtil;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.manager.service.stub.StratosManagerServiceApplicationSignUpExceptionException;
import org.apache.stratos.manager.service.stub.StratosManagerServiceDomainMappingExceptionException;
import org.apache.stratos.manager.service.stub.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.manager.service.stub.domain.application.signup.ArtifactRepository;
import org.apache.stratos.manager.service.stub.domain.application.signup.DomainMapping;
import org.apache.stratos.manager.user.management.StratosUserManagerUtils;
import org.apache.stratos.manager.user.management.exception.UserManagerException;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.application.Group;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.rest.endpoint.Constants;
import org.apache.stratos.rest.endpoint.ServiceHolder;
import org.apache.stratos.rest.endpoint.exception.*;
import org.apache.stratos.rest.endpoint.util.converter.ObjectConverter;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.tenant.mgt.core.TenantPersistor;
import org.wso2.carbon.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


public class StratosApiV41Utils {
    public static final String APPLICATION_STATUS_DEPLOYED = "Deployed";
    public static final String APPLICATION_STATUS_CREATED = "Created";
    public static final String APPLICATION_STATUS_UNDEPLOYING = "Undeploying";
    public static final int SUPER_TENANT_ID = -1234;

    private static final Log log = LogFactory.getLog(StratosApiV41Utils.class);


    /**
     * Add new cartridge util method
     *
     * @param cartridgeBean Cartridge definition
     * @throws RestAPIException
     */
    public static void addCartridge(CartridgeBean cartridgeBean, String cartridgeUuid,
                                    int tenantId) throws RestAPIException {

        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Adding cartridge: [tenant-id] %d [cartridge-uuid] %s[cartridge-type] %s ",
                        tenantId, cartridgeUuid, cartridgeBean.getType()));
            }

            List<IaasProviderBean> iaasProviders = cartridgeBean.getIaasProvider();
            if ((iaasProviders == null) || iaasProviders.size() == 0) {
                throw new RestAPIException(String.format("IaaS providers not found in cartridge: %s",
                        cartridgeBean.getType()));
            }

            for (PortMappingBean portMapping : cartridgeBean.getPortMapping()) {
                if (StringUtils.isBlank(portMapping.getName())) {
                    portMapping.setName(portMapping.getProtocol() + "-" + portMapping.getPort());
                    if (log.isInfoEnabled()) {
                        log.info(String.format("Port mapping name not found, default value generated: " +
                                        "[cartridge-uuid] %s [cartridge-type] %s [port-mapping-name] %s",
                                cartridgeUuid, cartridgeBean.getType(), portMapping.getName()));
                    }
                }
            }

            Cartridge cartridgeConfig = createCartridgeConfig(cartridgeBean, cartridgeUuid, tenantId);
            CloudControllerServiceClient cloudControllerServiceClient = CloudControllerServiceClient.getInstance();
            cloudControllerServiceClient.addCartridge(cartridgeConfig);

            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Successfully added cartridge: [tenant-id] %d [cartridge-uuid] %s [cartridge-type] %s ",
                        tenantId, cartridgeUuid, cartridgeBean.getType()));
            }
        } catch (CloudControllerServiceCartridgeAlreadyExistsExceptionException e) {
            String msg = "Could not add cartridge as it is already exits";
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (CloudControllerServiceInvalidCartridgeDefinitionExceptionException e) {
            String msg = "Could not add cartridge as invalid cartridge definition";
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (RemoteException e) {
            String msg = "Could not add cartridge";
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (CloudControllerServiceInvalidIaasProviderExceptionException e) {
            String msg = "Could not add cartridge as invalid iaas provider";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }



    /**
     * Update Cartridge
     *
     * @param cartridgeBean Cartridge Definition
     * @throws RestAPIException
     */
    public static void updateCartridge(CartridgeBean cartridgeBean) throws RestAPIException {
        try {
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();

            CloudControllerServiceClient cloudControllerServiceClient = CloudControllerServiceClient.getInstance();
            Cartridge existingCartridge = cloudControllerServiceClient.getCartridgeByTenant(cartridgeBean.getType(),
                    carbonContext.getTenantId());
            Cartridge cartridgeConfig = createCartridgeConfig(cartridgeBean, existingCartridge.getUuid(),
                    existingCartridge.getTenantId());
            cartridgeConfig.setUuid(existingCartridge.getUuid());

            if (log.isDebugEnabled()) {
                log.debug(String.format("Updating cartridge: [tenant-id] %d [cartridge-uuid] %s [cartridge-type] %s ",
                        existingCartridge.getTenantId(), existingCartridge.getUuid(), cartridgeBean.getType()));
            }

            List<IaasProviderBean> iaasProviders = cartridgeBean.getIaasProvider();
            if ((iaasProviders == null) || iaasProviders.size() == 0) {
                throw new RestAPIException(String.format("IaaS providers not found in cartridge: [tenant-id] %d " +
                                "[cartridge-uuid] %s [cartridge-type] %s ", existingCartridge.getTenantId(),
                        existingCartridge.getUuid(), cartridgeBean.getType()));
            }

            cloudControllerServiceClient.updateCartridge(cartridgeConfig);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Successfully updated cartridge: [tenant-id] %d [cartridge-uuid] %s " +
                                "[cartridge-type] %s", existingCartridge.getTenantId(), existingCartridge.getUuid(),
                        cartridgeBean.getType()));
            }
        } catch (CloudControllerServiceCartridgeDefinitionNotExistsExceptionException e) {
            String msg = "Could not add cartridge";
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (CloudControllerServiceInvalidCartridgeDefinitionExceptionException e) {
            String msg = "Could not add cartridge";
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (RemoteException e) {
            String msg = "Could not add cartridge";
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (CloudControllerServiceInvalidIaasProviderExceptionException e) {
            String msg = "Could not add cartridge";
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (CloudControllerServiceCartridgeNotFoundExceptionException e) {
            String msg = "Could not find existing cartridge";
            log.error(msg, e);
        }
    }

    /**
     * Create cartridge configuration
     *
     * @param cartridgeDefinition Cartridge definition
     * @return Created cartridge
     * @throws RestAPIException
     */
    private static Cartridge createCartridgeConfig(CartridgeBean cartridgeDefinition, String cartridgeUuid,
                                                   int tenantId) throws RestAPIException {
        Cartridge cartridgeConfig =
                ObjectConverter.convertCartridgeBeanToStubCartridgeConfig(cartridgeDefinition, cartridgeUuid, tenantId);
        if (cartridgeConfig == null) {
            throw new RestAPIException("Could not read cartridge definition, cartridge deployment failed");
        }
        if (StringUtils.isEmpty(cartridgeConfig.getCategory())) {
            throw new RestAPIException(String.format("Category is not specified in cartridge: [tenant-id] %d [cartridge-uuid] %s " +
                    "[cartridge-type] %s ", cartridgeConfig.getTenantId(), cartridgeConfig.getUuid(),
                    cartridgeConfig.getType()));
        }
        return cartridgeConfig;
    }

    /**
     * Remove Cartridge
     *
     * @param cartridgeType Cartridge Type
     * @throws RestAPIException
     */
    public static void removeCartridge(String cartridgeType, int tenantId) throws RestAPIException, RemoteException,
            CloudControllerServiceCartridgeNotFoundExceptionException,
            CloudControllerServiceInvalidCartridgeTypeExceptionException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        Cartridge cartridge= cloudControllerServiceClient.getCartridgeByTenant(cartridgeType, tenantId);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Removing cartridge: [tenant-id] %d [cartridge-uuid] %s [cartridge-type] %s ",
                    tenantId, cartridge.getUuid(), cartridgeType));
        }

        if (cartridge== null) {
            throw new RuntimeException(String.format("Cartridge not found: [cartridge-type] %s in tenant: " +
                    "[tenant-id] %s", cartridgeType, tenantId));
        }

        StratosManagerServiceClient smServiceClient = getStratosManagerServiceClient();

        // Validate whether cartridge can be removed
        if (!smServiceClient.canCartridgeBeRemoved(cartridge.getUuid())) {
            String logMessage = String.format("Cannot remove cartridge : [tenant-id] %d [cartridge-uuid] %s " +
                            "[cartridge-type] %s since it is used in another cartridge group or an application",
                    tenantId, cartridge.getUuid(), cartridgeType);
            String message = String.format("Cannot remove cartridge :n[cartridge-type] %s since it is used in another" +
                            " cartridge group or an application", cartridgeType);
            log.error(logMessage);
            throw new RestAPIException(message);
        }
        cloudControllerServiceClient.removeCartridge(cartridge.getUuid());

        if (log.isInfoEnabled()) {
            log.info(String.format("Successfully removed cartridge: [tenant-id] %d [cartridge-uuid] %s " +
                    "[cartridge-type] %s", tenantId, cartridge.getUuid(), cartridgeType));
        }
    }

    /**
     * Get List of Cartridges by filter
     *
     * @param filter               filter
     * @param criteria             criteria
     * @param configurationContext Configuration Context
     * @return List of cartridges matches filter
     * @throws RestAPIException
     */
    public static List<CartridgeBean> getCartridgesByFilter(
            String filter, String criteria, ConfigurationContext configurationContext,int tenantId) throws RestAPIException {
        List<CartridgeBean> cartridges = null;

        if (Constants.FILTER_TENANT_TYPE_SINGLE_TENANT.equals(filter)) {
            cartridges = getAvailableCartridges(null, false, configurationContext,tenantId);
        } else if (Constants.FILTER_TENANT_TYPE_MULTI_TENANT.equals(filter)) {
            cartridges = getAvailableCartridges(null, true, configurationContext,tenantId);
        } else if (Constants.FILTER_LOAD_BALANCER.equals(filter)) {
            cartridges = getAvailableLbCartridges(false, configurationContext,tenantId);
        } else if (Constants.FILTER_PROVIDER.equals(filter)) {
            cartridges = getAvailableCartridgesByProvider(criteria,tenantId);
        }


        return cartridges;
    }

    /**
     * Get a Cartridge by filter
     *
     * @param filter               filter
     * @param cartridgeType        cartride Type
     * @param configurationContext Configuration Context
     * @return Cartridge matching filter
     * @throws RestAPIException
     */
    public static CartridgeBean getCartridgeByFilter(
            String filter, String cartridgeType, ConfigurationContext configurationContext,int tenantId) throws RestAPIException {
        List<CartridgeBean> cartridges = getCartridgesByFilter(filter, null, configurationContext,tenantId);

        for (CartridgeBean cartridge : cartridges) {
            if (cartridge.getType().equals(cartridgeType)) {
                return cartridge;
            }
        }
        return null;
    }

    /**
     * Get the available Load balancer cartridges
     *
     * @param multiTenant          Multi tenant true of false
     * @param configurationContext Configuration Context
     * @return List of available Load balancer cartridges
     * @throws RestAPIException
     */
    private static List<CartridgeBean> getAvailableLbCartridges(
            boolean multiTenant, ConfigurationContext configurationContext,int tenantId) throws RestAPIException {
        List<CartridgeBean> cartridges = getAvailableCartridges(null, multiTenant,
                configurationContext,tenantId);
        List<CartridgeBean> lbCartridges = new ArrayList<CartridgeBean>();
        for (CartridgeBean cartridge : cartridges) {
            if (Constants.FILTER_LOAD_BALANCER.equalsIgnoreCase(cartridge.getCategory())) {
                lbCartridges.add(cartridge);
            }
        }
        return lbCartridges;
    }

    /**
     * Get the available cartridges by provider
     *
     * @param provider provide name
     * @return List of the cartridge definitions
     * @throws RestAPIException
     */
    private static List<CartridgeBean> getAvailableCartridgesByProvider(String provider,int tenantId) throws RestAPIException {
        List<CartridgeBean> cartridges = new ArrayList<CartridgeBean>();

        if (log.isDebugEnabled()) {
            log.debug("Reading cartridges: [provider-name] " + provider);
        }

        try {
            String[] availableCartridges = CloudControllerServiceClient.getInstance().getRegisteredCartridges();

            if (availableCartridges != null) {
                for (String cartridgeType : availableCartridges) {
                    Cartridge cartridgeInfo = null;
                    try {
                        cartridgeInfo = CloudControllerServiceClient.getInstance().getCartridgeByTenant(cartridgeType,tenantId);
                    } catch (Exception e) {
                        if (log.isWarnEnabled()) {
                            log.warn("Error when calling getCartridgeInfo for " + cartridgeType + ", Error: "
                                    + e.getMessage());
                        }
                    }
                    if (cartridgeInfo == null) {
                        // This cannot happen. But continue
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Cartridge Info not found: [tenant-id] %d [cartridge-type] %s",
                                    tenantId, cartridgeType));
                        }
                        continue;
                    }


                    if (!cartridgeInfo.getProvider().equals(provider)) {
                        continue;
                    }

                    CartridgeBean cartridge = ObjectConverter.
                            convertCartridgeToCartridgeDefinitionBean(cartridgeInfo);
                    cartridges.add(cartridge);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("There are no available cartridges in the tenant: [tenant-id] " + tenantId);
                }
            }
        } catch (AxisFault axisFault) {
            String errorMsg = String.format(
                    "Error while getting CloudControllerServiceClient instance to connect to the Cloud Controller. " +
                            "Cause: %s ", axisFault.getMessage());
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg, axisFault);
        } catch (RemoteException e) {
            String errorMsg =
                    String.format("Error while getting cartridge information for provider %s  Cause: %s ", provider,
                            e.getMessage());
            log.error(errorMsg, e);
            throw new RestAPIException(errorMsg, e);
        }

        if (log.isDebugEnabled()) {
            log.debug("Returning available cartridges " + cartridges.size());
        }

        return cartridges;
    }

    public static List<CartridgeBean> getAvailableCartridges(
            String cartridgeSearchString, Boolean multiTenant, ConfigurationContext configurationContext,int tenantId)
            throws RestAPIException {

        List<CartridgeBean> cartridges = new ArrayList<CartridgeBean>();

        if (log.isDebugEnabled()) {
            log.debug("Getting available cartridges. [Search String]: " + cartridgeSearchString + ", [Multi-Tenant]: " + multiTenant);
        }

        try {
            Pattern searchPattern = getSearchStringPattern(cartridgeSearchString);

            //String[] availableCartridges = CloudControllerServiceClient.getInstance().getRegisteredCartridges();
            Cartridge[] availableCartridges = CloudControllerServiceClient.getInstance().getCartridgesByTenant
                    (tenantId);

            if (availableCartridges != null) {
                for (Cartridge cartridgeDefinition : availableCartridges) {
                    Cartridge cartridgeInfo = null;
                    try {
                        //cartridgeInfo = CloudControllerServiceClient.getInstance().getCartridgeByTenant
                        // (cartridgeType,tenantId);
                        cartridgeInfo = cartridgeDefinition;
                    } catch (Exception e) {
                        if (log.isWarnEnabled()) {
                            log.warn("Error when calling getCartridgeInfo for " + cartridgeDefinition.getType() + ", Error: "
                                    + e.getMessage());
                        }
                    }
                    if (cartridgeInfo == null) {
                        // This cannot happen. But continue
                        if (log.isDebugEnabled()) {
                            log.debug("Cartridge Info not found.");
                        }
                        continue;
                    }

                    if (multiTenant != null && !multiTenant && cartridgeInfo.getMultiTenant()) {
                        // Need only Single-Tenant cartridges
                        continue;
                    } else if (multiTenant != null && multiTenant && !cartridgeInfo.getMultiTenant()) {
                        // Need only Multi-Tenant cartridges
                        continue;
                    }

                    if (!StratosApiV41Utils.cartridgeMatches(cartridgeInfo, searchPattern)) {
                        continue;
                    }
                    if(cartridgeInfo.getTenantId() == tenantId) {
                        CartridgeBean cartridge = ObjectConverter.convertCartridgeToCartridgeDefinitionBean(cartridgeInfo);
                        cartridges.add(cartridge);
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("There are no available cartridges");
                }
            }
        } catch (Exception e) {
            String msg = "Error while getting available cartridges. Cause: " + e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        //Collections.sort(cartridges);

        if (log.isDebugEnabled()) {
            log.debug("Returning available cartridges " + cartridges.size());
        }

        return cartridges;
    }

    /**
     * Get cartridge details
     *
     * @param cartridgeType Cartridge Type
     * @return Cartridge details
     * @throws RestAPIException
     */
    public static CartridgeBean getCartridge(String cartridgeType, int tenantId) throws RestAPIException {
        try {
            Cartridge cartridgeInfo = CloudControllerServiceClient.getInstance().getCartridgeByTenant(cartridgeType,
                    tenantId);
            if (cartridgeInfo == null) {
                return null;
            }
            if (cartridgeInfo.getTenantId() == tenantId) {
                return ObjectConverter.convertCartridgeToCartridgeDefinitionBean(cartridgeInfo);
            } else {
                return null;
            }
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        } catch (CloudControllerServiceCartridgeNotFoundExceptionException e) {
            String message = e.getFaultMessage().getCartridgeNotFoundException().getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Check cartridge is available
     *
     * @param cartridgeType cartridgeType
     * @return CartridgeBean
     * @throws RestAPIException
     */
    public static CartridgeBean getCartridgeForValidate(String cartridgeType,int tenantId) throws RestAPIException,
            CloudControllerServiceCartridgeNotFoundExceptionException {
        try {
            Cartridge cartridgeInfo = CloudControllerServiceClient.getInstance().getCartridgeByTenant(cartridgeType,tenantId);
            if (cartridgeInfo == null) {
                return null;
            }
            return ObjectConverter.convertCartridgeToCartridgeDefinitionBean(cartridgeInfo);
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message, e);
            throw new RestAPIException(message, e);
        }

    }

    /**
     * Convert SearchString to Pattern
     *
     * @param searchString SearchString
     * @return Pattern
     */
    private static Pattern getSearchStringPattern(String searchString) {
        if (log.isDebugEnabled()) {
            log.debug("Creating search pattern for " + searchString);
        }
        if (searchString != null) {
            // Copied from org.wso2.carbon.webapp.mgt.WebappAdmin.doesWebappSatisfySearchString(WebApplication, String)
            String regex = searchString.toLowerCase().replace("..?", ".?").replace("..*", ".*").replaceAll("\\?", ".?")
                    .replaceAll("\\*", ".*?");
            if (log.isDebugEnabled()) {
                log.debug("Created regex: " + regex + " for search string " + searchString);
            }

            return Pattern.compile(regex);
        }
        return null;
    }

    /**
     * Search cartridge Display name/Description for pattern
     *
     * @param cartridgeInfo cartridgeInfo
     * @param pattern       Pattern
     * @return Pattern match status
     */
    private static boolean cartridgeMatches(Cartridge cartridgeInfo, Pattern pattern) {
        if (pattern != null) {
            boolean matches = false;
            if (cartridgeInfo.getDisplayName() != null) {
                matches = pattern.matcher(cartridgeInfo.getDisplayName().toLowerCase()).find();
            }
            if (!matches && cartridgeInfo.getDescription() != null) {
                matches = pattern.matcher(cartridgeInfo.getDescription().toLowerCase()).find();
            }
            return matches;
        }
        return true;
    }

    // Util methods to get the service clients

    /**
     * Get CloudController Service Client
     *
     * @return CloudControllerServiceClient
     * @throws RestAPIException
     */
    private static CloudControllerServiceClient getCloudControllerServiceClient() throws RestAPIException {

        try {
            return CloudControllerServiceClient.getInstance();

        } catch (AxisFault axisFault) {
            String errorMsg = "Error while getting CloudControllerServiceClient instance to connect to the "
                    + "Cloud Controller. Cause: " + axisFault.getMessage();
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg, axisFault);
        }
    }

    /**
     * Get Autoscaler Service Client
     *
     * @return AutoscalerServiceClient
     * @throws RestAPIException
     */
    private static AutoscalerServiceClient getAutoscalerServiceClient() throws RestAPIException {
        try {
            return AutoscalerServiceClient.getInstance();
        } catch (AxisFault axisFault) {
            String errorMsg = "Error while getting AutoscalerServiceClient instance to connect to the "
                    + "Autoscaler. Cause: " + axisFault.getMessage();
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg, axisFault);
        }
    }

    /**
     * Get Stratos Manager Service Client
     *
     * @return StratosManagerServiceClient
     * @throws RestAPIException
     */
    private static StratosManagerServiceClient getStratosManagerServiceClient() throws RestAPIException {
        try {
            return StratosManagerServiceClient.getInstance();
        } catch (AxisFault axisFault) {
            String errorMsg = "Error while getting StratosManagerServiceClient instance to connect to the "
                    + "Stratos Manager. Cause: " + axisFault.getMessage();
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg, axisFault);
        }
    }

    // Util methods for Autoscaling policies

    /**
     * Add AutoscalePolicy
     *
     * @param autoscalePolicyBean autoscalePolicyBean
     * @throws RestAPIException
     */
    public static void addAutoscalingPolicy(AutoscalePolicyBean autoscalePolicyBean, String autoscalingPolicyUuid,
                                            int tenantId) throws RestAPIException,
            AutoscalerServiceInvalidPolicyExceptionException,
            AutoscalerServiceAutoScalingPolicyAlreadyExistExceptionException {

        log.info(String.format("Adding autoscaling policy: [tenant-id] %d [autoscaling-policy-uuid] %s " +
                        "[autoscaling-policy-id] %s", tenantId, autoscalingPolicyUuid, autoscalePolicyBean.getId()));

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy autoscalePolicy = ObjectConverter.
                    convertToCCAutoscalerPojo(autoscalePolicyBean, autoscalingPolicyUuid, tenantId);

            try {
                autoscalerServiceClient.addAutoscalingPolicy(autoscalePolicy);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            }
        }
    }

    /**
     * Add an application policy
     *
     * @param applicationPolicyBean applicationPolicyBean
     * @throws RestAPIException
     */
    public static void addApplicationPolicy(ApplicationPolicyBean applicationPolicyBean, String applicationPolicyUuid,
                                            int tenantId) throws RestAPIException,
            AutoscalerServiceInvalidApplicationPolicyExceptionException,
            AutoscalerServiceApplicationPolicyAlreadyExistsExceptionException {

        if (applicationPolicyBean == null) {
            String msg = "Application policy bean is null";
            log.error(msg);
            throw new ApplicationPolicyIsEmptyException(msg);
        }
        CloudControllerServiceClient cloudServiceClient=getCloudControllerServiceClient();
        AutoscalerServiceClient serviceClient = getAutoscalerServiceClient();
        try {
            ApplicationPolicy applicationPolicy = ObjectConverter.convertApplicationPolicyBeanToStubAppPolicy(
                    applicationPolicyBean, applicationPolicyUuid, tenantId);
            if (applicationPolicy == null) {
                String msg = "Application policy is null";
                log.error(msg);
                throw new ApplicationPolicyIsEmptyException(msg);
            }
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            NetworkPartition[] existingNetworkPartitions = cloudServiceClient.getNetworkPartitionsByTenant
                    (carbonContext.getTenantId());
            String[] networkPartitions = applicationPolicy.getNetworkPartitions();
            String[] networkPartitionsUuid = new String[applicationPolicy.getNetworkPartitions().length];

                for (int i = 0; i < networkPartitions.length; i++) {
                    if (existingNetworkPartitions != null) {
                        for (NetworkPartition networkPartition : existingNetworkPartitions) {
                            if (networkPartitions[i].equals(networkPartition.getId()) && (tenantId == networkPartition
                                    .getTenantId())) {
                                networkPartitionsUuid[i] = networkPartition.getUuid();
                            }
                        }
                    } else {
                        String message = String.format("Network partition not found: for [application-policy-id] %s" +
                                        "[network-partition-id] %s", applicationPolicyBean.getId(),
                                networkPartitions[i]);
                        throw new RestAPIException(message);
                    }
                }

            applicationPolicy.setNetworkPartitionsUuid(networkPartitionsUuid);
            serviceClient.addApplicationPolicy(applicationPolicy);

        } catch (RemoteException e) {
            String msg = "Could not add application policy. " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (AutoscalerServiceRemoteExceptionException e) {
            String msg = "Could not add application policy. " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    /**
     * Updates Application Policy
     *
     * @param applicationPolicyBean applicationPolicyBean
     * @throws RestAPIException
     */
    public static void updateApplicationPolicy(ApplicationPolicyBean applicationPolicyBean) throws RestAPIException,
            AutoscalerServiceInvalidApplicationPolicyExceptionException,
            AutoscalerServiceApplicatioinPolicyNotExistsExceptionException {

        log.info(String.format("Updating application policy: [application-policy-id] %s", applicationPolicyBean.getId()));

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                AutoscalerServiceClient serviceClient = AutoscalerServiceClient.getInstance();
                PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                ApplicationPolicy applicationPolicy = serviceClient.getApplicationPolicyByTenant
                        (applicationPolicyBean.getId(), carbonContext.getTenantId());
                autoscalerServiceClient.updateApplicationPolicy(ObjectConverter
                        .convertApplicationPolicyBeanToStubAppPolicy(applicationPolicyBean,
                                applicationPolicy.getUuid(), applicationPolicy.getTenantId()));
            } catch (RemoteException e) {
                String msg = "Could not update application policy" + e.getLocalizedMessage();
                log.error(msg, e);
                throw new RestAPIException(msg);
            } catch (AutoscalerServiceRemoteExceptionException e) {
                String msg = "Could not update application policy" + e.getLocalizedMessage();
                log.error(msg, e);
                throw new RestAPIException(msg);
            }
        }
    }

    /**
     * Get Application Policies
     *
     * @return Array of ApplicationPolicyBeans
     * @throws RestAPIException
     */
    public static ApplicationPolicyBean[] getApplicationPolicies() throws RestAPIException {

        ApplicationPolicy[] applicationPolicies = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                applicationPolicies = autoscalerServiceClient.getApplicationPoliciesByTenant(carbonContext.getTenantId());
            } catch (RemoteException e) {
                String msg = "Could not get application policies" + e.getLocalizedMessage();
                log.error(msg, e);
                throw new RestAPIException(msg);
            }
        }
        return ObjectConverter.convertASStubApplicationPoliciesToApplicationPolicies(applicationPolicies);
    }

    /**
     * Get ApplicationPolicy by Id
     *
     * @param applicationPolicyId applicationPolicyId
     * @return ApplicationPolicyBean
     * @throws RestAPIException
     */
    public static ApplicationPolicyBean getApplicationPolicy(String applicationPolicyId) throws RestAPIException {

        if (applicationPolicyId == null) {
            String msg = "Application policy bean id null";
            log.error(msg);
            throw new ApplicationPolicyIdIsEmptyException(msg);
        }

        if (StringUtils.isBlank(applicationPolicyId)) {
            String msg = "Application policy id is empty";
            log.error(msg);
            throw new ApplicationPolicyIdIsEmptyException(msg);
        }

        ApplicationPolicyBean applicationPolicyBean;
        try {
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            org.apache.stratos.autoscaler.stub.deployment.policy.ApplicationPolicy applicationPolicy =
                    AutoscalerServiceClient.getInstance().getApplicationPolicy(applicationPolicyId,
                            carbonContext.getTenantId());
            if (applicationPolicy == null) {
                return null;
            }
            applicationPolicyBean = ObjectConverter.convertASStubApplicationPolicyToApplicationPolicy(applicationPolicy);
        } catch (RemoteException e) {
            String msg = "Could not find application policy: [application-policy-id] " + applicationPolicyId;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        return applicationPolicyBean;
    }

    /**
     * Removes an Application Policy
     *
     * @param applicationPolicyId applicationPolicyId
     * @throws RestAPIException
     */
    public static void removeApplicationPolicy(String applicationPolicyId) throws RestAPIException,
            AutoscalerServiceInvalidPolicyExceptionException, AutoscalerServiceUnremovablePolicyExceptionException {

        if (applicationPolicyId == null) {
            String msg = "Application policy bean id null";
            log.error(msg);
            throw new ApplicationPolicyIdIsEmptyException(msg);
        }

        if (StringUtils.isBlank(applicationPolicyId)) {
            String msg = "Application policy id is empty";
            log.error(msg);
            throw new ApplicationPolicyIdIsEmptyException(msg);
        }

        try {
            AutoscalerServiceClient serviceClient = AutoscalerServiceClient.getInstance();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            ApplicationPolicy applicationPolicy = serviceClient.getApplicationPolicyByTenant
                    (applicationPolicyId, carbonContext.getTenantId());
            serviceClient.removeApplicationPolicy(applicationPolicy.getUuid());
        } catch (RemoteException e) {
            String msg = "Could not remove application policy. " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    /**
     * Updates an Autoscaling Policy
     *
     * @param autoscalePolicyBean autoscalePolicyBean
     * @throws RestAPIException
     */
    public static void updateAutoscalingPolicy(AutoscalePolicyBean autoscalePolicyBean) throws RestAPIException,
            AutoscalerServiceInvalidPolicyExceptionException {

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                String autoscalerUuid = autoscalerServiceClient
                        .getAutoScalePolicyForTenant(autoscalePolicyBean.getId(), carbonContext.getTenantId())
                        .getUuid();
                log.debug(String.format("Updating autoscaling policy: [tenant-id] %d [autoscaling-policy-uuid] %s " +
                                "[autoscaling-policy-id] %s", carbonContext.getTenantId(), autoscalerUuid,
                        autoscalePolicyBean.getId()));
                org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy autoscalePolicy =
                        ObjectConverter.convertToCCAutoscalerPojo(autoscalePolicyBean, autoscalerUuid,
                                carbonContext.getTenantId());
                autoscalerServiceClient.updateAutoscalingPolicy(autoscalePolicy);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            }
        }
    }

    /**
     * Removes an AutoscalingPolicy
     *
     * @param autoscalePolicyId autoscalePolicyId
     * @throws RestAPIException
     */
    public static void removeAutoscalingPolicy(String autoscalePolicyId) throws RestAPIException,
            AutoscalerServicePolicyDoesNotExistExceptionException,
            AutoscalerServiceUnremovablePolicyExceptionException {

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            AutoscalePolicy autoscalePolicyBean;
            try {
                PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                autoscalePolicyBean =autoscalerServiceClient.getAutoScalePolicyForTenant(autoscalePolicyId,
                        carbonContext.getTenantId());
                log.debug(String.format("Removing autoscaling policy: [tenant-id] %d [autoscaling-policy-uuid] %s " +
                                "[autoscaling-policy-id] %s", autoscalePolicyBean.getTenantId(),
                        autoscalePolicyBean.getUuid(), autoscalePolicyId));
                autoscalerServiceClient.removeAutoscalingPolicy(autoscalePolicyBean.getUuid());
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            }
        }
    }

    /**
     * Get list of Autoscaling Policies
     *
     * @return Array of AutoscalingPolicies
     * @throws RestAPIException
     */
    public static AutoscalePolicyBean[] getAutoScalePolicies() throws RestAPIException {
        try {
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            AutoscalePolicy[] autoscalePolicies
                    = AutoscalerServiceClient.getInstance().getAutoScalingPoliciesByTenant(carbonContext.getTenantId());
            return ObjectConverter.convertStubAutoscalePoliciesToAutoscalePolicies(autoscalePolicies);
        } catch (RemoteException e) {
            String message = "Could not get autoscaling policies";
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Get an AutoScalePolicy
     *
     * @param autoscalePolicyId autoscalePolicyId
     * @return AutoscalePolicyBean
     * @throws RestAPIException
     */
    public static AutoscalePolicyBean getAutoScalePolicy(String autoscalePolicyId) throws RestAPIException {

        AutoscalePolicyBean autoscalePolicyBean;
        try {
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            AutoscalePolicy autoscalePolicy = AutoscalerServiceClient.getInstance().getAutoScalePolicyForTenant
                    (autoscalePolicyId, carbonContext.getTenantId());

            if (autoscalePolicy == null) {
                return null;
            }
            autoscalePolicyBean = ObjectConverter.convertStubAutoscalePolicyToAutoscalePolicy(autoscalePolicy);
        } catch (RemoteException e) {
            String errorMsg = "Error while getting information for autoscaling policy: [autoscaing-policy-id]" +
                    autoscalePolicyId + ".  Cause: " + e.getMessage();
            log.error(errorMsg, e);
            throw new RestAPIException(errorMsg, e);
        }
        return autoscalePolicyBean;


    }

    // Util methods for repo actions

    /**
     * Notify ArtifactUpdatedEvent
     *
     * @param payload GitNotificationPayloadBean
     * @throws RestAPIException
     */
    public static void notifyArtifactUpdatedEvent(GitNotificationPayloadBean payload) throws RestAPIException {
        try {
            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            serviceClient.notifyArtifactUpdatedEventForRepository(payload.getRepository().getUrl());
        } catch (Exception e) {
            String message = "Could not send artifact updated event";
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    // Util methods for service groups

    /**
     * Add a Service Group
     *
     * @param serviceGroupDefinition serviceGroupDefinition
     * @throws InvalidCartridgeGroupDefinitionException
     * @throws RestAPIException
     */
    public static void addCartridgeGroup(CartridgeGroupBean serviceGroupDefinition, String cartridgeGroupUuid,
                                         int tenantId)
            throws InvalidCartridgeGroupDefinitionException, ServiceGroupDefinitionException, RestAPIException,
            CloudControllerServiceCartridgeNotFoundExceptionException,
            AutoscalerServiceInvalidServiceGroupExceptionException {

        if (serviceGroupDefinition == null) {
            throw new RuntimeException("Cartridge group definition is null");
        }

        List<String> cartridgeTypes = new ArrayList<String>();
        String[] cartridgeUuids;
        List<String> groupNames;
        String[] cartridgeGroupNames;

        if (log.isDebugEnabled()) {
            log.debug(String.format("Checking cartridges in cartridge group [tenant-id] %d [cartridge-group-uuid] %s " +
                    "[cartridge-group-name] %s", tenantId, cartridgeGroupUuid, serviceGroupDefinition.getName()));
        }

        ServiceGroup serviceGroup = ObjectConverter.convertServiceGroupDefinitionToASStubServiceGroup
                (serviceGroupDefinition, cartridgeGroupUuid, tenantId);
        try {
            findCartridgesInGroupBean(serviceGroup, cartridgeTypes);
        } catch (RemoteException e) {
            throw new RestAPIException(e.getMessage());
        }

        //validate the group definition to check if cartridges duplicate in any groups defined
        validateCartridgeDuplicationInGroupDefinition(serviceGroupDefinition);

        //validate the group definition to check if groups duplicate in any groups and
        //validate the group definition to check for cyclic group behaviour

        try {
            validateGroupDuplicationInGroupDefinition(serviceGroupDefinition);
        } catch (RemoteException e) {
            String message = String.format("Error while validating group duplications in cartridge group: [tenant-id]" +
                    " %d [cartridge-group-uuid] %s [cartridge-group-name] %s", tenantId, cartridgeGroupUuid,
                    serviceGroupDefinition.getName());
            log.error(message, e);
            throw new RestAPIException(message, e);
        }

        cartridgeUuids = new String[cartridgeTypes.size()];
        int j = 0;
        for (String cartridgeUuid : cartridgeTypes) {
            cartridgeUuids[j] = cartridgeUuid;
            j++;
        }

        // if any sub groups are specified in the group, they should be already deployed
        if (serviceGroupDefinition.getGroups() != null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("checking subGroups in cartridge group: [tenant-id] %d [cartridge-group-uuid]" +
                                " %s [cartridge-group-name] %s", tenantId, cartridgeGroupUuid,
                        serviceGroupDefinition.getName()));
            }

            List<CartridgeGroupBean> groupDefinitions = serviceGroupDefinition.getGroups();
            groupNames = new ArrayList<String>();
            cartridgeGroupNames = new String[groupDefinitions.size()];
            int i = 0;
            for (CartridgeGroupBean groupList : groupDefinitions) {
                groupNames.add(groupList.getName());
                cartridgeGroupNames[i] = groupList.getName();
                i++;
            }

            Set<String> duplicates = findDuplicates(groupNames);
            if (duplicates.size() > 0) {

                StringBuilder duplicatesOutput = new StringBuilder();
                for (String dup : duplicates) {
                    duplicatesOutput.append(dup).append(" ");
                }
                if (log.isDebugEnabled()) {
                    log.debug("duplicate sub-groups defined: " + duplicatesOutput.toString());
                }
                throw new InvalidCartridgeGroupDefinitionException("Invalid cartridge group definition, duplicate " +
                        "sub-groups defined:" + duplicatesOutput.toString());
            }
        }
        AutoscalerServiceClient asServiceClient = getAutoscalerServiceClient();
        try {
            asServiceClient.addServiceGroup(serviceGroup);
            // Add cartridge group elements to SM cache - done after service group has been added
            StratosManagerServiceClient smServiceClient = getStratosManagerServiceClient();
            smServiceClient.addUsedCartridgesInCartridgeGroups(serviceGroup.getUuid(), cartridgeUuids);
        } catch (RemoteException e) {
            String message = String.format("Could not add the cartridge group: [cartridge-group-name] %s ",
                    serviceGroupDefinition.getName());
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Update a cartridge group
     *
     * @param cartridgeGroup
     * @throws RestAPIException
     */
    public static void updateServiceGroup(CartridgeGroupBean cartridgeGroup,int tenantId) throws RestAPIException,
            InvalidCartridgeGroupDefinitionException, CloudControllerServiceCartridgeNotFoundExceptionException {
        try {
            AutoscalerServiceClient autoscalerServiceClient = AutoscalerServiceClient.getInstance();
            ServiceGroup existingServiceGroup =autoscalerServiceClient.getServiceGroupByTenant(cartridgeGroup.getName(), tenantId);

            StratosManagerServiceClient smServiceClient = getStratosManagerServiceClient();

            // Validate whether cartridge group can be updated
            if (!smServiceClient.canCartirdgeGroupBeRemoved(existingServiceGroup.getUuid())) {
                String message = String.format("Cannot update cartridge group: [cartridge-group-uuid] %s " +
                                "[cartridge-group-name] %s since it is used in another cartridge group or an application",
                        existingServiceGroup.getUuid(), cartridgeGroup.getName());

                log.error(message);
                throw new RestAPIException(message);
            }

            //validate the group definition to check if cartridges duplicate in any groups defined
            validateCartridgeDuplicationInGroupDefinition(cartridgeGroup);

            //validate the group definition to check if groups duplicate in any groups and
            //validate the group definition to check for cyclic group behaviour
            validateGroupDuplicationInGroupDefinition(cartridgeGroup);

            if (existingServiceGroup != null) {
                ServiceGroup serviceGroup= ObjectConverter.convertServiceGroupDefinitionToASStubServiceGroup(cartridgeGroup, existingServiceGroup.getUuid(), tenantId);
                autoscalerServiceClient.updateServiceGroup(serviceGroup);

                List<String> cartridgesBeforeUpdating = new ArrayList<String>();
                List<String> cartridgesAfterUpdating = new ArrayList<String>();

                ServiceGroup serviceGroupToBeUpdated = autoscalerServiceClient.getServiceGroup(existingServiceGroup.getUuid());
                findCartridgesInServiceGroup(serviceGroupToBeUpdated, cartridgesBeforeUpdating);
                findCartridgesInGroupBean(serviceGroup, cartridgesAfterUpdating);

                List<String> cartridgesToRemove = new ArrayList<String>();
                List<String> cartridgesToAdd = new ArrayList<String>();

                if (cartridgesBeforeUpdating != null) {
                    if (!cartridgesBeforeUpdating.isEmpty()) {
                        cartridgesToRemove.addAll(cartridgesBeforeUpdating);
                    }
                }

                if (cartridgesAfterUpdating != null) {
                    if (!cartridgesAfterUpdating.isEmpty()) {
                        cartridgesToAdd.addAll(cartridgesAfterUpdating);
                    }
                }

                if ((cartridgesBeforeUpdating != null) && (cartridgesAfterUpdating != null)) {
                    if ((!cartridgesBeforeUpdating.isEmpty()) && (!cartridgesAfterUpdating.isEmpty())) {
                        for (String before : cartridgesBeforeUpdating) {
                            for (String after : cartridgesAfterUpdating) {
                                if (before.toLowerCase().equals(after.toLowerCase())) {
                                    if (cartridgesToRemove.contains(after)) {
                                        cartridgesToRemove.remove(after);
                                    }
                                    if (cartridgesToAdd.contains(after)) {
                                        cartridgesToAdd.remove(after);
                                    }
                                }
                            }
                        }
                    }
                }

                // Add cartridge group elements to SM cache - done after cartridge group has been updated
                if (cartridgesToAdd != null) {
                    if (!cartridgesToAdd.isEmpty()) {
                        {
                            smServiceClient.addUsedCartridgesInCartridgeGroups(existingServiceGroup.getUuid(),
                                    cartridgesToAdd.toArray(new String[cartridgesToRemove.size()]));
                        }
                    }
                }

                // Remove cartridge group elements from SM cache - done after cartridge group has been updated
                if (cartridgesToRemove != null) {
                    if (!cartridgesToRemove.isEmpty()) {
                        smServiceClient.removeUsedCartridgesInCartridgeGroups(existingServiceGroup.getUuid(),
                                cartridgesToRemove.toArray(new String[cartridgesToRemove.size()]));
                    }
                }
            }
        } catch (RemoteException e) {
            String message = String.format("Could not update cartridge group: [cartridge-group-name] %s,",
                    cartridgeGroup.getName());
            log.error(message);
            throw new RestAPIException(message, e);
        } catch (AutoscalerServiceInvalidServiceGroupExceptionException e) {
            String message = String.format("Autoscaler invalid cartridge group definition: [cartridge-group-name] %s",
                    cartridgeGroup.getName());
            log.error(message);
            throw new InvalidCartridgeGroupDefinitionException(message, e);
        } catch (ServiceGroupDefinitionException e) {
            String message = String.format("Invalid cartridge group definition: [cartridge-group-name] %s",
                    cartridgeGroup.getName());
            log.error(message);
            throw new InvalidCartridgeGroupDefinitionException(message, e);
        }
    }

    /**
     * returns any duplicates in a List
     *
     * @param checkedList List to find duplicates from
     * @return Set of duplicates
     */
    private static Set<String> findDuplicates(List<String> checkedList) {
        final Set<String> retVals = new HashSet<String>();
        final Set<String> set1 = new HashSet<String>();

        for (String val : checkedList) {

            if (!set1.add(val)) {
                retVals.add(val);
            }
        }
        return retVals;
    }

    /**
     * Get a Service Group Definition by Name
     *
     * @param name Group Name
     * @return GroupBean
     * @throws RestAPIException
     */
    public static CartridgeGroupBean getServiceGroupDefinition(String name,int tenantId) throws RestAPIException {

        if (log.isDebugEnabled()) {
            log.debug("Reading cartridge group: [cartridge-group-name] " + name);
        }

        try {
            AutoscalerServiceClient asServiceClient = AutoscalerServiceClient.getInstance();
            ServiceGroup serviceGroup = asServiceClient.getServiceGroupByTenant(name, tenantId);
            if (serviceGroup == null) {
                return null;
            }

            return ObjectConverter.convertStubServiceGroupToServiceGroupDefinition(serviceGroup);

        } catch (Exception e) {
            String message = "Could not get cartridge group: [cartridge-group-name] " + name;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Get a Service Group Definition by Name
     *
     * @param groupName Group Name
     * @param tenantId tenant Id
     * @return GroupBean
     * @throws RestAPIException
     */
    public static CartridgeGroupBean getOuterServiceGroupDefinition(String groupName,
                                                                    int tenantId) throws RestAPIException {
        if (log.isDebugEnabled()) {
            log.debug("Reading cartridge group: [cartridge-group-name] " + groupName);
        }

        try {
            AutoscalerServiceClient asServiceClient = AutoscalerServiceClient.getInstance();
            ServiceGroup serviceGroup = asServiceClient.getOuterServiceGroupByTenant(groupName, tenantId);
            if (serviceGroup == null) {
                return null;
            }
            return ObjectConverter.convertStubServiceGroupToServiceGroupDefinition(serviceGroup);

        } catch (Exception e) {
            String message = "Could not get cartridge group: [cartridge-group-name] " + groupName;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }


    /**
     * Get a list of GroupBeans
     *
     * @return array of Group Beans
     * @throws RestAPIException
     */
    public static CartridgeGroupBean[] getServiceGroupDefinitions() throws RestAPIException {

        if (log.isDebugEnabled()) {
            log.debug("Reading cartridge groups...");
        }

        try {
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            AutoscalerServiceClient asServiceClient = AutoscalerServiceClient.getInstance();
            ServiceGroup[] serviceGroups = asServiceClient.getServiceGroupsByTenant(carbonContext.getTenantId());
            if (serviceGroups == null || serviceGroups.length == 0 || (serviceGroups.length == 1 && serviceGroups[0]
                    == null)) {
                return null;
            }

            CartridgeGroupBean[] serviceGroupDefinitions = new CartridgeGroupBean[serviceGroups.length];
            for (int i = 0; i < serviceGroups.length; i++) {
                serviceGroupDefinitions[i] = ObjectConverter.convertStubServiceGroupToServiceGroupDefinition(
                        serviceGroups[i]);
            }
            return serviceGroupDefinitions;

        } catch (Exception e) {
            throw new RestAPIException(e);
        }
    }


    /**
     * Remove Service Group
     *
     * @param name Group Name
     * @throws RestAPIException
     */
    public static void removeServiceGroup(String name, int tenantId) throws RestAPIException,
            AutoscalerServiceCartridgeGroupNotFoundExceptionException, CloudControllerServiceCartridgeNotFoundExceptionException {

        AutoscalerServiceClient asServiceClient = getAutoscalerServiceClient();
        StratosManagerServiceClient smServiceClient = getStratosManagerServiceClient();

        String serviceGroupUuid;
        // Check whether cartridge group exists
        try {

            ServiceGroup serviceGroup = asServiceClient.getServiceGroupByTenant(name, tenantId);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Removing cartridge group: [tenant-id] %d [cartridge-group-uuid] %s  " +
                        "[cartridge-group-name] %s", tenantId, serviceGroup.getUuid(), name));
            }

            if (serviceGroup == null) {
                String message = "Cartridge group: [cartridge-group-name] " + name + " cannot be removed since it " +
                        "does not exist in tenant " + tenantId;
                log.error(message);
                throw new RestAPIException(message);
            }
            // Validate whether cartridge group can be removed
            if (!smServiceClient.canCartirdgeGroupBeRemoved(serviceGroup.getUuid())) {
                String message = String.format("Cannot remove cartridge group: [cartridge-group-uuid] %s [group-name]" +
                                " %s since it is used in another cartridge group or an application in tenant %d",
                        serviceGroup.getUuid(), serviceGroup.getName(), tenantId);
                log.error(message);
                throw new RestAPIException(message);
            }

            serviceGroupUuid = serviceGroup.getUuid();
            asServiceClient.undeployServiceGroupDefinition(serviceGroup.getUuid());

            // Remove the dependent cartridges and cartridge groups from Stratos Manager cache
            // - done after service group has been removed

            List<String> cartridgeList = new ArrayList<String>();
            findCartridgesInServiceGroup(serviceGroup, cartridgeList);
            String[] cartridgeNames = cartridgeList.toArray(new String[cartridgeList.size()]);
            smServiceClient.removeUsedCartridgesInCartridgeGroups(serviceGroupUuid, cartridgeNames);

        } catch (RemoteException e) {
            throw new RestAPIException("Could not remove cartridge groups", e);
        }

        log.info(String.format("Successfully removed the cartridge group: [tenant-id] %d [cartridge-group-uuid] %s " +
                "[cartridge-group-name] %s", tenantId, serviceGroupUuid, name));
    }

    /**
     * Find Cartridges In ServiceGroup
     *
     * @param serviceGroup serviceGroup
     * @param cartridgeNames   List of cartridges
     */
    private static void findCartridgesInServiceGroup(ServiceGroup serviceGroup, List<String> cartridgeNames) throws
            RemoteException, CloudControllerServiceCartridgeNotFoundExceptionException {

        if (serviceGroup == null || cartridgeNames == null) {
            return;
        }
        String cartridgeUuid;
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        if (serviceGroup.getCartridges().length > 0) {
            for (String cartridgeName : serviceGroup.getCartridges()) {
                if (cartridgeName != null && (!cartridgeNames.contains(cartridgeName))) {
                    cartridgeUuid = CloudControllerServiceClient.getInstance().getCartridgeByTenant(cartridgeName,
                            carbonContext.getTenantId()).getUuid();
                    cartridgeNames.add(cartridgeUuid);
                }
            }
        }

        if (serviceGroup.getGroups() != null) {
            for (ServiceGroup seGroup : serviceGroup.getGroups()) {
                findCartridgesInServiceGroup(seGroup, cartridgeNames);
            }
        }
    }

    /**
     * Find Cartridges in GroupBean
     *
     * @param groupBean  groupBean
     * @param cartridges List of cartridges
     */
    private static void findCartridgesInGroupBean(ServiceGroup groupBean, List<String> cartridges) throws
            RemoteException, CloudControllerServiceCartridgeNotFoundExceptionException {
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        if (groupBean == null || cartridges == null) {
            return;
        }

        if (groupBean.getCartridges() != null) {
            for (String cartridge : groupBean.getCartridges()) {
                if (!cartridges.contains(cartridge)) {
                    Cartridge cartridge1 = CloudControllerServiceClient.getInstance().getCartridgeByTenant(cartridge,
                            groupBean.getTenantId());
                    cartridges.add(cartridge1.getUuid());
                }
            }
        }

        if (groupBean.getGroups() != null) {
            for (ServiceGroup seGroup : groupBean.getGroups()) {
                findCartridgesInGroupBean(seGroup, cartridges);
            }
        }
    }

    // Util methods for Applications

    /**
     * Verify the existence of the application and add it.
     *
     * @param appDefinition Application definition
     * @param ctxt          Configuration context
     * @param userName      Username
     * @param tenantDomain  Tenant Domain
     * @throws RestAPIException
     */
    public static void addApplication(ApplicationBean appDefinition, String applicationUuid, int tenantId,
                                      ConfigurationContext ctxt, String userName,
                                      String tenantDomain) throws RestAPIException,
            AutoscalerServiceCartridgeNotFoundExceptionException,
            AutoscalerServiceCartridgeGroupNotFoundExceptionException {

        if (StringUtils.isBlank(appDefinition.getApplicationId())) {
            String message = "Please specify the application name";
            log.error(message);
            throw new ApplicationAlreadyExistException(message);
        }
        // check if an application with same id already exists
        try {
            if (AutoscalerServiceClient.getInstance().existApplication(appDefinition.getApplicationId(), tenantId)) {
                String msg = String.format("Application already exists: [application-uuid] %s [application-name] %s",
                        applicationUuid, appDefinition.getName());
                throw new RestAPIException(msg);
            }
        } catch (RemoteException e) {
            throw new RestAPIException("Could not read application", e);
        }

        validateApplication(appDefinition);

        // To validate groups have unique alias in the application definition
        validateGroupsInApplicationDefinition(appDefinition);


        ApplicationContext applicationContext = ObjectConverter.convertApplicationDefinitionToStubApplicationContext(
                appDefinition, applicationUuid, tenantId);
        applicationContext.setTenantId(ApplicationManagementUtil.getTenantId(ctxt));
        applicationContext.setTenantDomain(tenantDomain);
        applicationContext.setTenantAdminUsername(userName);

        if (appDefinition.getProperty() != null) {
            org.apache.stratos.autoscaler.stub.Properties properties = new org.apache.stratos.autoscaler.stub.Properties();
            for (PropertyBean propertyBean : appDefinition.getProperty()) {
                org.apache.stratos.autoscaler.stub.Property property = new org.apache.stratos.autoscaler.stub.Property();
                property.setName(propertyBean.getName());
                property.setValue(propertyBean.getValue());
                properties.addProperties(property);
            }
            applicationContext.setProperties(properties);
        }

        try {
            AutoscalerServiceClient.getInstance().addApplication(applicationContext);

            List<String> usedCartridges = new ArrayList<String>();
            List<String> usedCartridgeGroups = new ArrayList<String>();
            findCartridgesAndGroupsInApplication(appDefinition, usedCartridges, usedCartridgeGroups);
            StratosManagerServiceClient smServiceClient = getStratosManagerServiceClient();
            smServiceClient.addUsedCartridgesInApplications(applicationUuid,
                    usedCartridges.toArray(new String[usedCartridges.size()]));
            smServiceClient.addUsedCartridgeGroupsInApplications(applicationUuid,
                    usedCartridgeGroups.toArray(new String[usedCartridgeGroups.size()]));

        } catch (AutoscalerServiceApplicationDefinitionExceptionException e) {
            String message = e.getFaultMessage().getApplicationDefinitionException().getMessage();
            throw new RestAPIException(message, e);
        } catch (RemoteException e) {
            throw new RestAPIException(e);
        }
    }

    /**
     * Update the existence of the application and update it.
     *
     * @param appDefinition Application definition
     * @param ctxt          Configuration context
     * @param userName      Username
     * @param tenantDomain  Tenant Domain
     * @throws RestAPIException
     */
    public static void updateApplication(ApplicationBean appDefinition, String applicationUuid, int tenantId,
                                         ConfigurationContext ctxt, String userName, String tenantDomain)
            throws RestAPIException, AutoscalerServiceCartridgeNotFoundExceptionException, AutoscalerServiceCartridgeGroupNotFoundExceptionException {

        if (StringUtils.isBlank(applicationUuid)) {
            String message = "Please specify the application name";
            log.error(message);
            throw new RestAPIException(message);
        }

        validateApplication(appDefinition);

        ApplicationContext applicationContext = ObjectConverter.convertApplicationDefinitionToStubApplicationContext(
                appDefinition, applicationUuid, tenantId);
        applicationContext.setTenantId(ApplicationManagementUtil.getTenantId(ctxt));
        applicationContext.setTenantDomain(tenantDomain);
        applicationContext.setTenantAdminUsername(userName);

        if (appDefinition.getProperty() != null) {
            org.apache.stratos.autoscaler.stub.Properties properties = new org.apache.stratos.autoscaler.stub.Properties();
            for (PropertyBean propertyBean : appDefinition.getProperty()) {
                org.apache.stratos.autoscaler.stub.Property property = new org.apache.stratos.autoscaler.stub.Property();
                property.setName(propertyBean.getName());
                property.setValue(propertyBean.getValue());
                properties.addProperties(property);
            }
            applicationContext.setProperties(properties);
        }

        try {
            AutoscalerServiceClient.getInstance().updateApplication(applicationContext);
        } catch (AutoscalerServiceApplicationDefinitionExceptionException e) {
            String message = e.getFaultMessage().getApplicationDefinitionException().getMessage();
            throw new RestAPIException(message, e);
        } catch (RemoteException e) {
            throw new RestAPIException(e);
        }
    }

    /**
     * Find Cartridges And Groups In Application
     *
     * @param applicationBean ApplicationBean
     * @param cartridges      List<String> cartridges
     * @param cartridgeGroups List <String> cartridgeGroups
     */
    private static void findCartridgesAndGroupsInApplication(
            ApplicationBean applicationBean, List<String> cartridges, List<String> cartridgeGroups) throws
            RemoteException, RestAPIException {

        if (applicationBean == null || applicationBean.getComponents() == null) {
            return;
        }

        ComponentBean componentBean = applicationBean.getComponents();

        List<CartridgeGroupReferenceBean> groupReferenceBeans = componentBean.getGroups();
        if (groupReferenceBeans != null) {
            for (CartridgeGroupReferenceBean groupReferenceBean : groupReferenceBeans) {
                findCartridgesAndGroupsInCartridgeGroup(groupReferenceBean, cartridges, cartridgeGroups);
            }
        }

        List<CartridgeReferenceBean> cartridgeReferenceBeans = componentBean.getCartridges();
        findCartridgeNamesInCartridges(cartridgeReferenceBeans, cartridges);
    }

    /**
     * Find Cartridges And Groups In CartridgeGroup
     *
     * @param groupReferenceBean GroupReferenceBean
     * @param cartridges         List <String>
     * @param cartridgeGroups    List <String>
     */
    private static void findCartridgesAndGroupsInCartridgeGroup(CartridgeGroupReferenceBean groupReferenceBean,
                                                                List<String> cartridges,
                                                                List<String> cartridgeGroups) throws RemoteException,
            RestAPIException {

        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        if (groupReferenceBean == null || cartridgeGroups == null) {
            return;
        }

        if (!cartridgeGroups.contains(groupReferenceBean.getName())) {
            ServiceGroup serviceGroup = AutoscalerServiceClient.getInstance().getServiceGroupByTenant
                    (groupReferenceBean.getName(), carbonContext.getTenantId());
            cartridgeGroups.add(serviceGroup.getUuid());
        }

        if (groupReferenceBean.getGroups() != null) {
            for (CartridgeGroupReferenceBean grReferenceBean : groupReferenceBean.getGroups()) {
                findCartridgesAndGroupsInCartridgeGroup(grReferenceBean, cartridges, cartridgeGroups);
                findCartridgeNamesInCartridges(groupReferenceBean.getCartridges(), cartridges);
            }
        }

        findCartridgeNamesInCartridges(groupReferenceBean.getCartridges(), cartridges);
    }

    /**
     * Find Cartridge Names In Cartridges
     *
     * @param cartridgeReferenceBeans List of CartridgeReferenceBean
     * @param cartridges              List <String>
     */
    private static void findCartridgeNamesInCartridges(
            List<CartridgeReferenceBean> cartridgeReferenceBeans, List<String> cartridges) throws RemoteException,
            RestAPIException {

        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        if (cartridgeReferenceBeans == null || cartridges == null) {
            return;
        }

        for (CartridgeReferenceBean cartridgeReferenceBean : cartridgeReferenceBeans) {
            if (cartridgeReferenceBean != null && !cartridges.contains(cartridgeReferenceBean.getUuid())) {
                Cartridge cartridge = null;
                try {
                    cartridge = CloudControllerServiceClient.getInstance().getCartridgeByTenant
                            (cartridgeReferenceBean.getType(), carbonContext.getTenantId());
                } catch (CloudControllerServiceCartridgeNotFoundExceptionException e) {
                    String message = e.getFaultMessage().getCartridgeNotFoundException().getMessage();
                    log.error(message);
                    throw new RestAPIException(message, e);
                }
                cartridges.add(cartridge.getUuid());
            }
        }
    }

    /**
     * Validate Application
     *
     * @param appDefinition ApplicationBean
     * @throws RestAPIException
     */
    private static void validateApplication(ApplicationBean appDefinition) throws RestAPIException {

        if (StringUtils.isBlank(appDefinition.getAlias())) {
            String message = "Please specify the application alias";
            log.error(message);
            throw new RestAPIException(message);
        }
    }

    /**
     * This method is to validate the application definition to have unique aliases among its groups
     *
     * @param applicationDefinition - the application definition
     * @throws RestAPIException
     */
    private static void validateGroupsInApplicationDefinition(ApplicationBean applicationDefinition) throws RestAPIException {

        ConcurrentHashMap<String, CartridgeGroupReferenceBean> groupsInApplicationDefinition = new ConcurrentHashMap<String, CartridgeGroupReferenceBean>();
        boolean groupParentHasDeploymentPolicy = false;

        if ((applicationDefinition.getComponents().getGroups() != null) &&
                (!applicationDefinition.getComponents().getGroups().isEmpty())) {

            //This is to validate the top level groups in the application definition
            for (CartridgeGroupReferenceBean group : applicationDefinition.getComponents().getGroups()) {
                if (groupsInApplicationDefinition.get(group.getAlias()) != null) {
                    String message = "Cartridge group alias exists more than once: [group-alias] " +
                            group.getAlias();
                    throw new RestAPIException(message);
                }

                // Validate top level group deployment policy with cartridges
                if (group.getCartridges() != null) {
                    groupParentHasDeploymentPolicy = group.getDeploymentPolicy() != null;
                    validateCartridgesForDeploymentPolicy(group.getCartridges(), groupParentHasDeploymentPolicy);
                }

                groupsInApplicationDefinition.put(group.getAlias(), group);

                if (group.getGroups() != null) {
                    //This is to validate the groups aliases recursively
                    validateGroupsRecursively(groupsInApplicationDefinition, group.getGroups(), groupParentHasDeploymentPolicy);
                }
            }
        }

        if ((applicationDefinition.getComponents().getCartridges() != null) &&
                (!applicationDefinition.getComponents().getCartridges().isEmpty())) {
            validateCartridgesForDeploymentPolicy(applicationDefinition.getComponents().getCartridges(), false);
        }

    }

    /**
     * This method validates cartridges in groups
     * Deployment policy should not defined in cartridge if group has a deployment policy
     * If group does not have a DP, then cartridge should have one
     *
     * @param cartridgeReferenceBeans - Cartridges in a group
     * @throws RestAPIException
     */
    private static void validateCartridgesForDeploymentPolicy(List<CartridgeReferenceBean> cartridgeReferenceBeans,
                                                              boolean hasDeploymentPolicy) throws RestAPIException {

        if (hasDeploymentPolicy) {
            for (CartridgeReferenceBean cartridge : cartridgeReferenceBeans) {
                if (cartridge.getSubscribableInfo().getDeploymentPolicy() != null) {
                    String message = "Group deployment policy already exists. Remove deployment policy from " +
                            "cartridge subscription : [cartridge-alias] " + cartridge.getSubscribableInfo().getAlias();
                    throw new RestAPIException(message);
                }
            }
        } else {
            for (CartridgeReferenceBean cartridge : cartridgeReferenceBeans) {
                if (cartridge.getSubscribableInfo().getDeploymentPolicy() == null) {
                    String message = String.format("Deployment policy is not defined for cartridge [cartridge] %s." +
                                    "It has not inherited any deployment policies.",
                            cartridge.getSubscribableInfo().getAlias());
                    throw new RestAPIException(message);
                }
            }

        }


    }

    /**
     * This method validates group aliases recursively
     *
     * @param groupsSet - the group collection in which the groups are added to
     * @param groups    - the group collection in which it traverses through
     * @throws RestAPIException
     */

    private static void validateGroupsRecursively(ConcurrentHashMap<String, CartridgeGroupReferenceBean> groupsSet,
                                                  Collection<CartridgeGroupReferenceBean> groups, boolean hasDeploymentPolicy)
            throws RestAPIException {

        boolean groupHasDeploymentPolicy;

        for (CartridgeGroupReferenceBean group : groups) {
            if (groupsSet.get(group.getAlias()) != null) {
                String message = "Cartridge group alias exists more than once: [group-alias] " +
                        group.getAlias();
                throw new RestAPIException(message);
            }

            if (group.getDeploymentPolicy() != null) {
                if (hasDeploymentPolicy) {
                    String message = "Parent group has a deployment policy. Remove deployment policy from the" +
                            " group: [group-alias] " + group.getAlias();
                    throw new RestAPIException(message);
                } else {
                    groupHasDeploymentPolicy = true;
                }
            } else {
                groupHasDeploymentPolicy = hasDeploymentPolicy;
            }

            if (group.getCartridges() != null) {
                validateCartridgesForDeploymentPolicy(group.getCartridges(), groupHasDeploymentPolicy);
            }

            groupsSet.put(group.getAlias(), group);

            if (group.getGroups() != null) {
                validateGroupsRecursively(groupsSet, group.getGroups(), groupHasDeploymentPolicy);
            }
        }
    }

    /**
     * Deploy application with an application policy.
     *
     * @param applicationId       Application Id
     * @param applicationPolicyId Application policy Id
     * @throws RestAPIException
     */
    public static void deployApplication(String applicationId, String applicationPolicyId)
            throws RestAPIException {

        if (StringUtils.isEmpty(applicationPolicyId)) {
            String message = "Application policy id is Empty";
            log.error(message);
            throw new RestAPIException(message);
        }

        try {
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            int tenantId = carbonContext.getTenantId();

            AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
            ApplicationContext application = autoscalerServiceClient.getApplicationByTenant(applicationId, tenantId);

            if (log.isInfoEnabled()) {
                log.info(String.format("Starting to deploy application: [tenant-id] %d [application-uuid] %s " +
                                "[application-id] %s", tenantId, application.getApplicationUuid(), applicationId));
            }

            if (application == null) {
                String message = String.format("Application not found: [application-id] %s", applicationId);
                log.error(message);
                throw new RestAPIException(message);
            }

            if (application.getStatus().equalsIgnoreCase(APPLICATION_STATUS_DEPLOYED)) {
                String message = String.format(
                        "Application is already in DEPLOYED state: [application-uuid] %s [application-id] %s [current" +
                                " status] %s ", application.getApplicationUuid(), applicationId,
                        application.getStatus());
                log.error(message);
                throw new ApplicationAlreadyDeployedException(message);
            }

            // This is a redundant state since there is only CREATED,DEPLOYED state.
            // But this will be useful when more status are added.
            if (!application.getStatus().equalsIgnoreCase(APPLICATION_STATUS_CREATED)) {
                String message = String.format(
                        "Application is not in CREATED state: [application-uuid] %s [application-id] %s [current " +
                                "status] %s ", application.getApplicationUuid(), applicationId, application.getStatus());
                log.error(message);
                throw new RestAPIException(message);
            }

            ApplicationBean applicationBean = getApplication(applicationId,tenantId);
            //int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            if (applicationBean.isMultiTenant() && (tenantId != -1234)) {
                String message = String.format(
                        "Multi-tenant applications can only be deployed by super tenant: [application-uuid] %s " +
                                "[application-id] %s", application.getApplicationUuid(), applicationId);
                log.error(message);
                throw new RestAPIException(message);
            }
            String applicationPolicyUuid=AutoscalerServiceClient.getInstance().getApplicationPolicyByTenant(
                    applicationPolicyId,application.getTenantId()).getUuid();
            autoscalerServiceClient.deployApplication(application.getApplicationUuid(), applicationPolicyUuid);
            if (log.isInfoEnabled()) {
                log.info(String.format("Application deployed successfully: [application-uuid] %s [application-id] %s",
                        application.getApplicationUuid(), applicationId));
            }
        } catch (RemoteException e) {
            log.error(e.getMessage(), e);
            throw new RestAPIException(e.getMessage(), e);
        } catch (AutoscalerServiceInvalidPolicyExceptionException e) {
            String message = e.getFaultMessage().getInvalidPolicyException().getMessage();
            log.error(message, e);
            throw new RestAPIException(message, e);
        } catch (AutoscalerServiceApplicationDefinitionExceptionException e) {
            String message = e.getMessage();
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Get Application Network Partitions
     *
     * @param applicationId Application ID
     * @return ApplicationNetworkPartitionIdListBean
     */
    public static ApplicationNetworkPartitionIdListBean getApplicationNetworkPartitions(String applicationId) {
        try {
            AutoscalerServiceClient serviceClient = AutoscalerServiceClient.getInstance();
            String[] networkPartitions = serviceClient.getApplicationNetworkPartitions(applicationId);
            ApplicationNetworkPartitionIdListBean appNetworkPartitionsBean = new ApplicationNetworkPartitionIdListBean();
            appNetworkPartitionsBean.setNetworkPartitionIds(Arrays.asList(networkPartitions));
            return appNetworkPartitionsBean;
        } catch (Exception e) {
            String message = String.format("Could not get application network partitions for " +
                    "application: [application-id] %s", applicationId);
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Remove Application
     *
     * @param applicationId Application Id
     * @throws RestAPIException
     */
    public static void removeApplication(String applicationId) throws RestAPIException {

        try {
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            AutoscalerServiceClient asServiceClient = getAutoscalerServiceClient();

            ApplicationContext asApplication = asServiceClient.getApplicationByTenant(applicationId,
                    carbonContext.getTenantId());

            log.info(String.format("Starting to remove application: [tenant-id] %d [application-uuid %s " +
                            "[application-id] %s", carbonContext.getTenantId(), asApplication.getApplicationUuid(),
                    applicationId));

            ApplicationBean application = ObjectConverter.convertStubApplicationContextToApplicationDefinition(
                    asApplication);
            asServiceClient.deleteApplication(asApplication.getApplicationUuid());

            List<String> usedCartridges = new ArrayList<String>();
            List<String> usedCartridgeGroups = new ArrayList<String>();
            findCartridgesAndGroupsInApplication(application, usedCartridges, usedCartridgeGroups);
            StratosManagerServiceClient smServiceClient = getStratosManagerServiceClient();
            smServiceClient.removeUsedCartridgesInApplications(
                    asApplication.getApplicationUuid(),
                    usedCartridges.toArray(new String[usedCartridges.size()]));

            smServiceClient.removeUsedCartridgeGroupsInApplications(
                    asApplication.getApplicationUuid(),
                    usedCartridgeGroups.toArray(new String[usedCartridgeGroups.size()]));

        } catch (RemoteException e) {
            String message = "Could not delete application: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Get Application Details
     *
     * @param applicationId Application Id
     * @return ApplicationBean
     * @throws RestAPIException
     */
    public static ApplicationBean getApplication(String applicationId,int tenantId) throws RestAPIException {
        try {
            return ObjectConverter.convertStubApplicationContextToApplicationDefinition(
                    AutoscalerServiceClient.getInstance().getApplicationByTenant(applicationId,tenantId));
        } catch (RemoteException e) {
            String message = "Could not read application: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Get list of Applications
     *
     * @return List of Application Beans
     * @throws RestAPIException
     */
    public static List<ApplicationBean> getApplications() throws RestAPIException {
        try {
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            List<ApplicationBean> applicationDefinitions = new ArrayList<ApplicationBean>();
            ApplicationContext[] applicationContexts = AutoscalerServiceClient.getInstance().getApplicationsByTenant
                    (carbonContext.getTenantId());
            if (applicationContexts != null) {
                for (ApplicationContext applicationContext : applicationContexts) {
                    if (applicationContext != null) {
                        ApplicationBean applicationDefinition =
                                ObjectConverter.convertStubApplicationContextToApplicationDefinition(applicationContext);
                        applicationDefinitions.add(applicationDefinition);
                    }
                }
            }
            return applicationDefinitions;
        } catch (RemoteException e) {
            String message = "Could not read applications";
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Undeploy an Application
     *
     * @param applicationId applicationId
     * @param force         parameter to set force undeployment
     * @throws RestAPIException
     */
    public static void undeployApplication(String applicationId, boolean force) throws RestAPIException {
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (force) {
            if (log.isDebugEnabled()) {
                log.debug("Forcefully undeploying application [application-id] " + applicationId);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Gracefully undeploying application [application-id] " + applicationId);
            }
        }
        if (autoscalerServiceClient != null) {
            try {
                autoscalerServiceClient.undeployApplication(applicationId, force);
            } catch (RemoteException e) {
                String message = "Could not undeploy application: [application-id] " + applicationId;
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (AutoscalerServiceApplicationDefinitionExceptionException e) {
                String message = "Could not undeploy application: [application-id] " + applicationId;
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
    }

    /**
     * Get Application Runtime
     *
     * @param applicationId Application Id
     * @return ApplicationInfoBean
     */
    public static ApplicationInfoBean getApplicationRuntime(String applicationId,int tenantId)
            throws RestAPIException {
        ApplicationInfoBean applicationBean = null;
        ApplicationContext applicationContext;
        String applicationUuid;
        //Checking whether application is in deployed mode
        try {
            applicationUuid=getAutoscalerServiceClient().getApplicationByTenant(applicationId,tenantId).getApplicationUuid();
            applicationContext = getAutoscalerServiceClient().
                    getApplication(applicationUuid);
        } catch (RemoteException e) {
            String message = "Could not get application definition: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);

        } catch (RestAPIException e) {
            String message = "Could not get application definition: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);

        }

        try {
            ApplicationManager.acquireReadLockForApplication(applicationUuid);
            Application application = ApplicationManager.getApplications().getApplication(applicationUuid);
            if (application == null) {
                return null;
            }
            if (application.getInstanceContextCount() > 0
                    || (applicationContext != null &&
                    applicationContext.getStatus().equals("Deployed"))) {

                applicationBean = ObjectConverter.convertApplicationToApplicationInstanceBean(application);
                for (ApplicationInstanceBean instanceBean : applicationBean.getApplicationInstances()) {
                    addClustersInstancesToApplicationInstanceBean(instanceBean, application);
                    addGroupsInstancesToApplicationInstanceBean(instanceBean, application);
                }
            }
        } finally {
            ApplicationManager.releaseReadLockForApplication(applicationUuid);
        }

        return applicationBean;
    }

    /**
     * Add GroupsInstances To ApplicationInstanceBean
     *
     * @param applicationInstanceBean ApplicationInstanceBean
     * @param application             Application
     */
    private static void addGroupsInstancesToApplicationInstanceBean(ApplicationInstanceBean applicationInstanceBean,
                                                                    Application application) throws RestAPIException {
        Collection<Group> groups = application.getGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                List<GroupInstanceBean> groupInstanceBeans = ObjectConverter.convertGroupToGroupInstancesBean(
                        applicationInstanceBean.getInstanceId(), group);
                for (GroupInstanceBean groupInstanceBean : groupInstanceBeans) {
                    setSubGroupInstances(group, groupInstanceBean);
                    applicationInstanceBean.getGroupInstances().add(groupInstanceBean);
                }
            }
        }

    }

    /**
     * Add ClustersInstances To ApplicationInstanceBean
     *
     * @param applicationInstanceBean ApplicationInstanceBean
     * @param application             Application
     */
    private static void addClustersInstancesToApplicationInstanceBean(
            ApplicationInstanceBean applicationInstanceBean, Application application) throws RestAPIException {

        Map<String, ClusterDataHolder> topLevelClusterDataMap = application.getClusterDataMap();
        if (topLevelClusterDataMap != null) {
            for (Map.Entry<String, ClusterDataHolder> entry : topLevelClusterDataMap.entrySet()) {
                ClusterDataHolder clusterDataHolder = entry.getValue();
                String clusterId = clusterDataHolder.getClusterId();
                String serviceType = clusterDataHolder.getServiceUuid();
                try {
                    TopologyManager.acquireReadLockForCluster(serviceType, clusterId);
                    Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterId);

                    applicationInstanceBean.getClusterInstances().add(ObjectConverter.
                            convertClusterToClusterInstanceBean(applicationInstanceBean.getInstanceId(),
                                    cluster, entry.getKey()));
                } finally {
                    TopologyManager.releaseReadLockForCluster(serviceType, clusterId);
                }
            }
        }
    }

    /**
     * Add ClustersInstances To GroupInstanceBean
     *
     * @param groupInstanceBean GroupInstanceBean
     * @param group             Group
     */
    private static void addClustersInstancesToGroupInstanceBean(
            GroupInstanceBean groupInstanceBean,
            Group group) throws RestAPIException {
        Map<String, ClusterDataHolder> topLevelClusterDataMap = group.getClusterDataMap();
        if (topLevelClusterDataMap != null && !topLevelClusterDataMap.isEmpty()) {
            for (Map.Entry<String, ClusterDataHolder> entry : topLevelClusterDataMap.entrySet()) {
                ClusterDataHolder clusterDataHolder = entry.getValue();
                String clusterId = clusterDataHolder.getClusterId();
                String serviceTypeUuid = clusterDataHolder.getServiceUuid();
                try {
                    TopologyManager.acquireReadLockForCluster(serviceTypeUuid, clusterId);
                    Cluster topLevelCluster = TopologyManager.getTopology().getService(serviceTypeUuid).getCluster(clusterId);
                    groupInstanceBean.getClusterInstances().add(ObjectConverter.
                            convertClusterToClusterInstanceBean(groupInstanceBean.getInstanceId(),
                                    topLevelCluster, entry.getKey()));
                } finally {
                    TopologyManager.releaseReadLockForCluster(serviceTypeUuid, clusterId);
                }
            }
        }

    }

    /**
     * Set Sub Group Instances
     *
     * @param group             Group
     * @param groupInstanceBean GroupInstanceBean
     */
    private static void setSubGroupInstances(Group group, GroupInstanceBean groupInstanceBean) throws RestAPIException {
        Collection<Group> subgroups = group.getGroups();
        addClustersInstancesToGroupInstanceBean(groupInstanceBean, group);
        if (subgroups != null && !subgroups.isEmpty()) {
            for (Group subGroup : subgroups) {
                List<GroupInstanceBean> groupInstanceBeans = ObjectConverter.
                        convertGroupToGroupInstancesBean(groupInstanceBean.getInstanceId(),
                                subGroup);
                for (GroupInstanceBean groupInstanceBean1 : groupInstanceBeans) {
                    setSubGroupInstances(subGroup, groupInstanceBean1);
                    groupInstanceBean.getGroupInstances().add(groupInstanceBean1);
                }

            }
        }

    }

    // Util methods for Kubernetes clusters

    /**
     * Add Kubernetes Cluster
     *
     * @param kubernetesClusterBean KubernetesClusterBean
     * @return add status
     * @throws RestAPIException
     */
    public static boolean addKubernetesCluster(KubernetesClusterBean kubernetesClusterBean,
                                               String kubernetesClusterUuid, int tenantId) throws RestAPIException,
            CloudControllerServiceInvalidKubernetesClusterExceptionException,
            CloudControllerServiceKubernetesClusterAlreadyExistsExceptionException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster kubernetesCluster =
                    ObjectConverter.convertToCCKubernetesClusterPojo(kubernetesClusterBean, kubernetesClusterUuid, tenantId);

            try {
                return cloudControllerServiceClient.deployKubernetesCluster(kubernetesCluster);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            }
        }
        return false;
    }


    /**
     * Update Kubernetes Cluster
     *
     * @param kubernetesClusterBean KubernetesClusterBean
     * @return add status
     * @throws RestAPIException
     */
    public static boolean updateKubernetesCluster(KubernetesClusterBean kubernetesClusterBean,
                                                  String kubernetesClusterUuid, int tenantId) throws RestAPIException,
            CloudControllerServiceInvalidKubernetesClusterExceptionException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster kubernetesCluster =
                    ObjectConverter.convertToCCKubernetesClusterPojo(kubernetesClusterBean, kubernetesClusterUuid,
                            tenantId);

            try {
                return cloudControllerServiceClient.updateKubernetesCluster(kubernetesCluster);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * Add Kubernetes Host
     *
     * @param kubernetesClusterId KubernetesClusterId
     * @param kubernetesHostBean  KubernetesHostBean
     * @return add status
     * @throws RestAPIException
     */
    public static boolean addKubernetesHost(String kubernetesClusterId, KubernetesHostBean kubernetesHostBean,int tenantId)
            throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost kubernetesHost =
                    ObjectConverter.convertKubernetesHostToStubKubernetesHost(kubernetesHostBean,tenantId);
            try {
                KubernetesCluster kubernetesCluster=cloudControllerServiceClient.getKubernetesClusterByTenantId(kubernetesClusterId, tenantId);
                return cloudControllerServiceClient.addKubernetesHost(kubernetesCluster.getClusterUuid(), kubernetesHost);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceInvalidKubernetesHostExceptionException e) {
                String message = e.getFaultMessage().getInvalidKubernetesHostException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (CloudControllerServiceNonExistingKubernetesClusterExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesClusterException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return false;
    }

    /**
     * Update Kubernetes Master
     *
     * @param kubernetesMasterBean KubernetesMasterBean
     * @return update status
     * @throws RestAPIException
     */
    public static boolean updateKubernetesMaster(KubernetesMasterBean kubernetesMasterBean,int tenantId) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesMaster kubernetesMaster =
                    ObjectConverter.convertStubKubernetesMasterToKubernetesMaster(kubernetesMasterBean,tenantId);

            try {
                return cloudControllerServiceClient.updateKubernetesMaster(kubernetesMaster);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceInvalidKubernetesMasterExceptionException e) {
                String message = e.getFaultMessage().getInvalidKubernetesMasterException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (CloudControllerServiceNonExistingKubernetesMasterExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesMasterException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return false;
    }

    /**
     * Get Available Kubernetes Clusters
     *
     * @return Array of KubernetesClusterBeans
     * @throws RestAPIException
     */
    public static KubernetesClusterBean[] getAvailableKubernetesClusters(int tenantId) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster[]
                        kubernetesClusters = cloudControllerServiceClient.getAvailableKubernetesClusters(tenantId);
                if (kubernetesClusters == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("There are no available Kubernetes clusters");
                    }

                    return null;
                }

                return ObjectConverter.convertStubKubernetesClustersToKubernetesClusters(kubernetesClusters);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Get a Kubernetes Cluster
     *
     * @param kubernetesClusterId Cluster ID
     * @return KubernetesClusterBean
     * @throws RestAPIException
     */
    public static KubernetesClusterBean getKubernetesCluster(String kubernetesClusterId) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        if (cloudControllerServiceClient != null) {
            try {
                KubernetesCluster kubernetesCluster = cloudControllerServiceClient.getKubernetesClusterByTenantId
                        (kubernetesClusterId, carbonContext.getTenantId());
                return ObjectConverter.convertStubKubernetesClusterToKubernetesCluster(kubernetesCluster);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            }
        }
        return null;
    }


    /**
     * Remove Kubernetes Cluster
     *
     * @param kubernetesClusterId kubernetesClusterId
     * @return remove status
     * @throws RestAPIException
     */
    public static boolean removeKubernetesCluster(String kubernetesClusterId,int tenantId) throws RestAPIException,
            CloudControllerServiceNonExistingKubernetesClusterExceptionException {

        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                KubernetesCluster kubernetesCluster=cloudControllerServiceClient.getKubernetesClusterByTenantId(kubernetesClusterId, tenantId);
                cloudControllerServiceClient.undeployKubernetesCluster(kubernetesCluster.getClusterUuid());

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * Remove Kubernetes Host
     *
     * @param kubernetesHostId Kubernetes HostId
     * @return remove status
     * @throws RestAPIException
     */
    public static boolean removeKubernetesHost(String kubernetesHostId) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                return cloudControllerServiceClient.undeployKubernetesHost(kubernetesHostId);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceNonExistingKubernetesHostExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesHostException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return false;
    }

    /**
     * Get Kubernetes Hosts
     *
     * @param kubernetesClusterId kubernetesClusterId
     * @return List of KubernetesHostBeans
     * @throws RestAPIException
     */
    public static KubernetesHostBean[] getKubernetesHosts(String kubernetesClusterId) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost[]
                        kubernetesHosts = cloudControllerServiceClient.getKubernetesHosts(kubernetesClusterId);

                List<KubernetesHostBean> arrayList = ObjectConverter.convertStubKubernetesHostsToKubernetesHosts(
                        kubernetesHosts);
                KubernetesHostBean[] array = new KubernetesHostBean[arrayList.size()];
                array = arrayList.toArray(array);
                return array;
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceNonExistingKubernetesClusterExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesClusterException().getMessage();
                log.error(message);
                throw new RestAPIException(message, e);
            }
        }
        return null;
    }

    /**
     * Get Kubernetes Master
     *
     * @param kubernetesClusterId Kubernetes ClusterId
     * @return KubernetesMasterBean
     * @throws RestAPIException
     */
    public static KubernetesMasterBean getKubernetesMaster(String kubernetesClusterId) throws RestAPIException {
        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesMaster
                        kubernetesMaster = cloudControllerServiceClient.getKubernetesMaster(kubernetesClusterId);
                return ObjectConverter.convertStubKubernetesMasterToKubernetesMaster(kubernetesMaster);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceNonExistingKubernetesClusterExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesClusterException().getMessage();
                log.error(message);
                throw new RestAPIException(message, e);
            }
        }
        return null;
    }

    /**
     * Update KubernetesHost
     *
     * @param kubernetesHostBean KubernetesHostBean
     * @return update status
     * @throws RestAPIException
     */
    public static boolean updateKubernetesHost(KubernetesHostBean kubernetesHostBean,int tenantId) throws
            RestAPIException {
        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost kubernetesHost =
                    ObjectConverter.convertKubernetesHostToStubKubernetesHost(kubernetesHostBean,tenantId);
            try {
                return cloudControllerServiceClient.updateKubernetesHost(kubernetesHost);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceInvalidKubernetesHostExceptionException e) {
                String message = e.getFaultMessage().getInvalidKubernetesHostException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (CloudControllerServiceNonExistingKubernetesHostExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesHostException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return false;
    }

    /**
     * Add Application Signup
     *
     * @param applicationId         applicationId
     * @param applicationSignUpBean ApplicationSignUpBean
     * @throws RestAPIException
     */
    public static void addApplicationSignUp(String applicationId, ApplicationSignUpBean applicationSignUpBean)
            throws RestAPIException {

        if (StringUtils.isBlank(applicationId)) {
            throw new RestAPIException("Application id is null");
        }

        //multi tenant application can be added by only the super tenant.Hence passing the super tenant id to retrieve
        // the application
        ApplicationBean applicationBean = getApplication(applicationId, SUPER_TENANT_ID);
        Application application = ApplicationManager.getApplications().getApplicationByTenant(applicationId, SUPER_TENANT_ID);

        if ((applicationBean == null) || (application == null)) {
            throw new RestAPIException("Application not found: [application-id] " + applicationId);
        }

        if (!APPLICATION_STATUS_DEPLOYED.equals(applicationBean.getStatus())) {
            throw new RestAPIException(String.format("Application has not been deployed: [application-id] %s ",
                    applicationId));
        }

        if (!applicationBean.isMultiTenant()) {
            throw new RestAPIException("Application signups cannot be added to single-tenant applications");
        }

        if (applicationSignUpBean == null) {
            throw new RestAPIException("Application signup is null");
        }

        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Adding applicationBean signup: [application-id] %s", applicationId));
            }

            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

            ApplicationSignUp applicationSignUp = ObjectConverter.convertApplicationSignUpBeanToStubApplicationSignUp(
                    applicationSignUpBean);
            applicationSignUp.setApplicationId(applicationId);
            applicationSignUp.setTenantId(tenantId);
            List<String> clusterIds = findApplicationClusterIds(application);
            String[] clusterIdsArray = clusterIds.toArray(new String[clusterIds.size()]);
            applicationSignUp.setClusterIds(clusterIdsArray);

            // Encrypt artifact repository passwords
            encryptRepositoryPasswords(applicationSignUp, application.getKey());

            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            serviceClient.addApplicationSignUp(applicationSignUp);

            if (log.isInfoEnabled()) {
                log.info(String.format("Application signup added successfully: [tenant-id] %d [application-id] %s ",
                        tenantId, applicationId));
            }

            serviceClient.notifyArtifactUpdatedEventForSignUp(applicationId, tenantId);
            if (log.isInfoEnabled()) {
                log.info(String.format("Artifact updated event sent: [tenant-id] %d [application-id] %s", tenantId,
                        applicationId));
            }
        } catch (Exception e) {
            String message = String.format("Error in applicationBean signup: [application-id] %s", applicationId);
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Find application cluster ids.
     *
     * @param application Application
     * @return list of cluster Ids
     */
    private static List<String> findApplicationClusterIds(Application application) {
        List<String> clusterIds = new ArrayList<String>();
        for (ClusterDataHolder clusterDataHolder : application.getClusterDataRecursively()) {
            clusterIds.add(clusterDataHolder.getClusterId());
        }
        return clusterIds;
    }

    /**
     * Encrypt artifact repository passwords.
     *
     * @param applicationSignUp Application Signup
     * @param applicationKey    Application Key
     */
    private static void encryptRepositoryPasswords(ApplicationSignUp applicationSignUp, String applicationKey) {
        if (applicationSignUp.getArtifactRepositories() != null) {
            for (ArtifactRepository artifactRepository : applicationSignUp.getArtifactRepositories()) {
                if (artifactRepository != null) {
                    String repoPassword = artifactRepository.getRepoPassword();
                    if ((StringUtils.isNotBlank(repoPassword))) {
                        String encryptedRepoPassword = CommonUtil.encryptPassword(repoPassword,
                                applicationKey);
                        artifactRepository.setRepoPassword(encryptedRepoPassword);

                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Artifact repository password encrypted: [application-id] %s " +
                                            "[tenant-id] %d [repo-url] %s", applicationSignUp.getApplicationId(),
                                    applicationSignUp.getTenantId(), artifactRepository.getRepoUrl()));
                        }
                    }
                }
            }
        }
    }

    /**
     * Get Application SignUp
     *
     * @param applicationId applicationId
     * @return ApplicationSignUpBean
     * @throws RestAPIException
     */
    public static ApplicationSignUpBean getApplicationSignUp(String applicationId) throws RestAPIException,
            StratosManagerServiceApplicationSignUpExceptionException {
        if (StringUtils.isBlank(applicationId)) {
            throw new ApplicationSignUpRestAPIException("Application id is null");
        }

        ApplicationBean application = getApplication(applicationId,-1234);
        if (application == null) {
            throw new ApplicationSignUpRestAPIException("Application does not exist: [application-id] " + applicationId);
        }

        if (!application.isMultiTenant()) {
            throw new ApplicationSignUpRestAPIException("Application sign ups not available for single-tenant applications");
        }

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        try {
            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            ApplicationSignUp applicationSignUp = serviceClient.getApplicationSignUp(applicationId, tenantId);
            if (applicationSignUp != null) {
                return ObjectConverter.convertStubApplicationSignUpToApplicationSignUpBean(applicationSignUp);
            }
            return null;
        } catch (RemoteException e) {
            String message = String.format("Could not get application signup: [application-id] %s [tenant-id] %d",
                    applicationId, tenantId);
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Remove Application SignUp
     *
     * @param applicationId applicationId
     * @throws RestAPIException
     */
    public static void removeApplicationSignUp(String applicationId) throws RestAPIException {
        if (StringUtils.isBlank(applicationId)) {
            throw new RestAPIException("Application id is null");
        }

        ApplicationBean application = getApplication(applicationId,-1234);
        if (application == null) {
            throw new RestAPIException("Application does not exist: [application-id] " + applicationId);
        }

        if (!application.isMultiTenant()) {
            throw new RestAPIException("Application signups not available for single-tenant applications");
        }

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        try {
            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            serviceClient.removeApplicationSignUp(applicationId, tenantId);

            if (log.isInfoEnabled()) {
                log.info(String.format("Application signup removed successfully: [application-id] %s" +
                        "[tenant-id] %d", applicationId, tenantId));
            }
        } catch (Exception e) {
            String message = String.format("Could not remove application signup: [application-id] %s [tenant-id] %d ",
                    applicationId, tenantId);
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }


    /**
     * Add Application Domain Mappings
     *
     * @param applicationId      application Id
     * @param domainMappingsBean ApplicationDomainMappingsBean
     * @throws RestAPIException
     */
    public static void addApplicationDomainMappings(
            String applicationId, ApplicationDomainMappingsBean domainMappingsBean) throws RestAPIException,
            StratosManagerServiceDomainMappingExceptionException {

        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            if (domainMappingsBean.getDomainMappings() != null) {
                StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
                String applicationUuid =
                        AutoscalerServiceClient.getInstance().getApplicationByTenant(applicationId, tenantId)
                                .getApplicationUuid();
                for (DomainMappingBean domainMappingBean : domainMappingsBean.getDomainMappings()) {
                    ClusterDataHolder clusterDataHolder = findClusterDataHolder(
                            applicationUuid,
                            domainMappingBean.getCartridgeAlias());

                    DomainMapping domainMapping = ObjectConverter.convertDomainMappingBeanToStubDomainMapping(
                            domainMappingBean);
                    domainMapping.setApplicationId(applicationUuid);
                    domainMapping.setTenantId(tenantId);
                    domainMapping.setServiceName(clusterDataHolder.getServiceType());
                    domainMapping.setClusterId(clusterDataHolder.getClusterId());
                    serviceClient.addDomainMapping(domainMapping);

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Domain mapping added: [application-id] %s [tenant-id] %d " +
                                        "[domain-name] %s [context-path] %s", applicationId, tenantId,
                                domainMapping.getDomainName(), domainMapping.getContextPath()));
                    }
                }
            }
        } catch (RemoteException e) {
            String message = "Could not add domain mappings: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Find Cluster Data Holder
     *
     * @param applicationId  applicationId
     * @param cartridgeAlias cartridge Alias
     * @return ClusterDataHolder
     */
    private static ClusterDataHolder findClusterDataHolder(String applicationId, String cartridgeAlias) {
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        Application application = ApplicationManager.getApplications().getApplicationByTenant(applicationId,tenantId);
        if (application == null) {
            throw new RuntimeException(String.format("Application not found: [application-id] %s", applicationId));
        }

        ClusterDataHolder clusterDataHolder = application.getClusterData(cartridgeAlias);
        if (clusterDataHolder == null) {
            throw new RuntimeException(String.format("Cluster data not found for cartridge alias: [application-id] %s " +
                    "[cartridge-alias] %s", applicationId, cartridgeAlias));
        }
        return clusterDataHolder;
    }

    /**
     * Remove Application Domain Mappings
     *
     * @param applicationId applicationId
     * @param domainName    the domain name
     * @throws RestAPIException
     */
    public static void removeApplicationDomainMapping(String applicationId, String domainName)
            throws RestAPIException, StratosManagerServiceDomainMappingExceptionException {

        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            String applicationUuid =
                    AutoscalerServiceClient.getInstance().getApplicationByTenant(applicationId, tenantId)
                            .getApplicationUuid();
            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();

            if (domainName != null) {
                serviceClient.removeDomainMapping(applicationUuid, tenantId, domainName);

                if (log.isInfoEnabled()) {
                    log.info(String.format("Domain mapping removed: [application-id] %s [tenant-id] %d " +
                            "[domain-name] %s", applicationId, tenantId, domainName));
                }
            }
        } catch (RemoteException e) {
            String message = "Could not remove domain mappings: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Get Application Domain Mappings
     *
     * @param applicationId applicationId
     * @return List of DomainMappingBeans
     * @throws RestAPIException
     */
    public static List<DomainMappingBean> getApplicationDomainMappings(String applicationId) throws RestAPIException,
            StratosManagerServiceDomainMappingExceptionException {
        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            List<DomainMappingBean> domainMappingsBeans = new ArrayList<DomainMappingBean>();
            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            DomainMapping[] domainMappings = serviceClient.getDomainMappings(applicationId, tenantId);
            if (domainMappings != null) {
                for (DomainMapping domainMapping : domainMappings) {
                    if (domainMapping != null) {
                        DomainMappingBean domainMappingBean =
                                ObjectConverter.convertStubDomainMappingToDomainMappingBean(domainMapping);
                        domainMappingsBeans.add(domainMappingBean);
                    }
                }
            }
            return domainMappingsBeans;
        } catch (RemoteException e) {
            String message = "Could not get domain mappings: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Add a Network Partition
     *
     * @param networkPartitionBean NetworkPartitionBean
     */
    public static void addNetworkPartition(NetworkPartitionBean networkPartitionBean, String networkPartitionUuid,
                                           int tenantId) throws RestAPIException,
            CloudControllerServiceNetworkPartitionAlreadyExistsExceptionException,
            CloudControllerServiceInvalidNetworkPartitionExceptionException {
        try {
            CloudControllerServiceClient serviceClient = CloudControllerServiceClient.getInstance();
            serviceClient.addNetworkPartition(ObjectConverter.convertNetworkPartitionToCCStubNetworkPartition
                    (networkPartitionBean, networkPartitionUuid, tenantId));
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Get Network Partitions
     *
     * @return Array of NetworkPartitionBeans
     */
    public static NetworkPartitionBean[] getNetworkPartitions() throws RestAPIException {
        try {
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            CloudControllerServiceClient serviceClient = CloudControllerServiceClient.getInstance();
            org.apache.stratos.cloud.controller.stub.domain.NetworkPartition[] networkPartitions =
                    serviceClient.getNetworkPartitionsByTenant(carbonContext.getTenantId());
            return ObjectConverter.convertCCStubNetworkPartitionsToNetworkPartitions(networkPartitions);
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Remove Network Partition
     *
     * @param networkPartitionId networkPartitionId
     */
    public static void removeNetworkPartition(String networkPartitionId, int tenantId) throws RestAPIException,
            CloudControllerServiceNetworkPartitionNotExistsExceptionException {
        try {

            AutoscalerServiceClient autoscalerServiceClient = AutoscalerServiceClient.getInstance();

            autoscalerServiceClient.validateNetworkPartitionWithApplication(networkPartitionId, tenantId);

            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            CloudControllerServiceClient serviceClient = CloudControllerServiceClient.getInstance();

            org.apache.stratos.cloud.controller.stub.domain.NetworkPartition networkPartition =
                    serviceClient.getNetworkPartitionByTenant(networkPartitionId, tenantId);
            String networkPartitionUuid;
            if (networkPartition != null) {
                networkPartitionUuid = networkPartition.getUuid();
            } else {
                String message = String .format("Network partition not found [network-partition-id] %s", networkPartitionId);
                log.error(message);
                throw new RestAPIException(message);
            }

            DeploymentPolicy[] deploymentPolicies = autoscalerServiceClient.getDeploymentPoliciesByTenant(carbonContext.getTenantId());

            if (deploymentPolicies != null) {
                for (DeploymentPolicy deploymentPolicy : deploymentPolicies) {
                    for (org.apache.stratos.autoscaler.stub.partition.NetworkPartitionRef networkPartitionRef :
                            deploymentPolicy.getNetworkPartitionRefs()) {
                        if (networkPartitionRef.getUuid().equals(networkPartitionUuid)) {
                            String message = String.format("Cannot remove the network partition: " +
                                            "[network-partition-uuid] %s [network-partition-id] %s since" +
                                            " it is used in deployment policy: [deployment-policy-uuid] %s " +
                                            "[deployment-policy-id] %s", networkPartitionUuid, networkPartitionId,
                                    deploymentPolicy.getUuid(), deploymentPolicy.getId());
                            log.error(message);
                            throw new RestAPIException(message);
                        }
                    }
                }
            }

            ApplicationPolicy[] applicationPolicies = autoscalerServiceClient.getApplicationPolicies();

            if (applicationPolicies != null) {
                for (ApplicationPolicy applicationPolicy : applicationPolicies) {
                    for (String networkPartitionUuid1 : applicationPolicy.getNetworkPartitionsUuid()) {
                        if (networkPartitionUuid1.equals(networkPartitionUuid)) {
                            String message = String.format("Cannot remove the network partition: " +
                                            "[network-partition-uuid] %s [network-partition-id] %s since it is used " +
                                            "in application policy: [application-policy-uuid] %s " +
                                            "[application-policy-id] %s", networkPartitionUuid, networkPartitionId,
                                    applicationPolicy.getUuid(), applicationPolicy.getId());
                            log.error(message);
                            throw new RestAPIException(message);
                        }
                    }
                }
            }

            serviceClient.removeNetworkPartition(networkPartitionId, tenantId);
        } catch (AutoscalerServicePartitionValidationExceptionException e) {
            String message = e.getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        }
        catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Get Network Partition
     *
     * @param networkPartitionId networkPartitionId
     * @return NetworkPartitionBean
     */
    public static NetworkPartitionBean getNetworkPartition(String networkPartitionId) throws RestAPIException {
        try {
            CloudControllerServiceClient serviceClient = CloudControllerServiceClient.getInstance();
            org.apache.stratos.cloud.controller.stub.domain.NetworkPartition[] networkPartitions =
                    serviceClient.getNetworkPartitions();

            NetworkPartition networkPartition = null;
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            if(networkPartitions!=null && (networkPartitions.length>0)) {
                for (NetworkPartition networkPartition1 : networkPartitions) {
                    if (carbonContext.getTenantId() == networkPartition1.getTenantId()) {
                        if (networkPartition1.getId().equals(networkPartitionId)) {
                            networkPartition = networkPartition1;
                        }
                    }
                }
            }
            if (networkPartition == null) {
                return null;
            }
            return ObjectConverter.convertCCStubNetworkPartitionToNetworkPartition(networkPartition);
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Update Network Partition
     *
     * @param networkPartitionBean NetworkPartitionBean
     */
    public static void updateNetworkPartition(NetworkPartitionBean networkPartitionBean) throws RestAPIException,
            CloudControllerServiceNetworkPartitionNotExistsExceptionException {
        try {
            CloudControllerServiceClient serviceClient = CloudControllerServiceClient.getInstance();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            NetworkPartition networkPartition = serviceClient.getNetworkPartitionByTenant(networkPartitionBean.getId(),
                    carbonContext.getTenantId());
            serviceClient.updateNetworkPartition(ObjectConverter.
                    convertNetworkPartitionToCCStubNetworkPartition(networkPartitionBean, networkPartition.getUuid(),
                            networkPartition.getTenantId()));
            log.debug(String.format("Updating network partition: [tenant-id] %d [network-partition-uuid] %s, " +
                            "[network-partition-id] %s", networkPartition.getTenantId(), networkPartition.getUuid(),
                    networkPartition.getId()));
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Add deployment policy
     *
     * @param deploymentPolicyDefinitionBean DeploymentPolicyBean
     */
    public static void addDeploymentPolicy(DeploymentPolicyBean deploymentPolicyDefinitionBean,
                                           String deploymentPolicyUuid, int tenantId)
            throws RestAPIException, AutoscalerServiceDeploymentPolicyAlreadyExistsExceptionException,
            AutoscalerServiceInvalidDeploymentPolicyExceptionException {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Adding deployment policy: [tenant-id] %d [deployment-policy-uuid] %s " +
                        "[deployment-policy-id]" +
                        " %s ", tenantId, deploymentPolicyUuid, deploymentPolicyDefinitionBean.getId()));
            }

            NetworkPartitionBean[] networkPartitions = getNetworkPartitions();
            for (NetworkPartitionReferenceBean networkPartitionReferenceBean : deploymentPolicyDefinitionBean
                    .getNetworkPartitions()) {
                NetworkPartition networkPartition = CloudControllerServiceClient.getInstance()
                        .getNetworkPartitionByTenant(networkPartitionReferenceBean.getId(), tenantId);

                if (networkPartition == null) {
                    String message = String.format("Network partition not found: for [deployment-policy-id] %s " +
                            "[network-partition-id] %s" , deploymentPolicyDefinitionBean.getId(),
                            networkPartitionReferenceBean.getId());
                    throw new RestAPIException(message);
                }

                Partition[] partitions = CloudControllerServiceClient.getInstance().getPartitionsByNetworkPartition
                        (networkPartitionReferenceBean.getId(), tenantId);

                for (NetworkPartitionBean networkPartitionBean : networkPartitions) {
                    if (networkPartition.getTenantId() == tenantId && networkPartitionBean.getId().equals
                            (networkPartitionReferenceBean.getId())) {
                        for (PartitionReferenceBean partitionReferenceBean : networkPartitionReferenceBean.getPartitions()) {
                            for (Partition partition : partitions) {
                                if (partition.getTenantId() == tenantId &&
                                        partitionReferenceBean.getId().equals(partition.getId())) {
                                    partitionReferenceBean.setUuid(partition.getUuid());
                                }
                            }
                        }
                    }
                }
            }
            AutoscalerServiceClient.getInstance().addDeploymentPolicy(ObjectConverter
                    .convertDeploymentPolicyBeanToASDeploymentPolicy(deploymentPolicyDefinitionBean,
                            deploymentPolicyUuid, tenantId));

            if (log.isDebugEnabled()) {
                log.debug(String.format("Successfully added deploymentPolicy: [tenant-id] %d [deployment-policy-uuid]" +
                                " %s [deployment-policy-id] %s", tenantId, deploymentPolicyUuid,
                        deploymentPolicyDefinitionBean.getId()));
            }
        } catch (RemoteException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        } catch (AutoscalerServiceRemoteExceptionException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }
    }

    /**
     * Get deployment policy by deployment policy id
     *
     * @param deploymentPolicyId deployment policy id
     * @return {@link DeploymentPolicyBean}
     */
    public static DeploymentPolicyBean getDeploymentPolicy(String deploymentPolicyId) throws RestAPIException {

        DeploymentPolicyBean deploymentPolicyBean;
        try {
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy deploymentPolicy
                    = AutoscalerServiceClient.getInstance().getDeploymentPolicyForTenant(deploymentPolicyId,
                    carbonContext.getTenantId());
            if (deploymentPolicy == null) {
                return null;
            }
            deploymentPolicyBean = ObjectConverter.convertCCStubDeploymentPolicyToDeploymentPolicy(deploymentPolicy);
        } catch (RemoteException e) {
            String msg = "Could not find deployment policy: [deployment-policy-id] " + deploymentPolicyId;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        return deploymentPolicyBean;
    }

    /**
     * Get deployment policies
     *
     * @return array of {@link DeploymentPolicyBean}
     */
    public static DeploymentPolicyBean[] getDeploymentPolicies() throws RestAPIException {
        try {
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy[] deploymentPolicies
                    = AutoscalerServiceClient.getInstance().getDeploymentPoliciesByTenant(carbonContext.getTenantId());
            return ObjectConverter.convertASStubDeploymentPoliciesToDeploymentPolicies(deploymentPolicies);
        } catch (RemoteException e) {
            String message = "Could not get deployment policies";
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Update deployment policy
     *
     * @param deploymentPolicyDefinitionBean DeploymentPolicyBean
     * @throws RestAPIException
     */
    public static void updateDeploymentPolicy(DeploymentPolicyBean deploymentPolicyDefinitionBean)
            throws RestAPIException, AutoscalerServiceInvalidPolicyExceptionException,
            AutoscalerServiceInvalidDeploymentPolicyExceptionException,
            AutoscalerServiceDeploymentPolicyNotExistsExceptionException {
        try {
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            int tenantId=carbonContext.getTenantId();
            NetworkPartitionBean[] networkPartitions = getNetworkPartitions();
            for (NetworkPartitionReferenceBean networkPartitionReferenceBean : deploymentPolicyDefinitionBean
                    .getNetworkPartitions()) {
                NetworkPartition networkPartition = CloudControllerServiceClient.getInstance()
                        .getNetworkPartitionByTenant(networkPartitionReferenceBean.getId(), tenantId);

                if (networkPartition == null) {
                    String message = String.format("Network partition not found: [deployment-policy-id] %s" +
                                    "[network-partition-id] %s" , deploymentPolicyDefinitionBean.getId(),
                            networkPartitionReferenceBean.getId());
                    throw new RestAPIException(message);
                }

                for (NetworkPartitionBean networkPartitionBean : networkPartitions) {
                    if (networkPartition.getTenantId() == tenantId && networkPartitionBean.getId().equals
                            (networkPartitionReferenceBean.getId())) {
                        for (PartitionReferenceBean partition : networkPartitionReferenceBean.getPartitions()) {
                            for (Partition existingPartition : CloudControllerServiceClient
                                    .getInstance().getPartitionsByNetworkPartition(networkPartitionReferenceBean
                                            .getId(), tenantId)) {
                                if (existingPartition.getTenantId() == tenantId &&
                                        partition.getId().equals(existingPartition.getId())) {
                                    partition.setUuid(existingPartition.getUuid());
                                }
                            }
                        }
                    }
                }
            }
            AutoscalerServiceClient serviceClient = AutoscalerServiceClient.getInstance();

            DeploymentPolicy deploymentPolicy = serviceClient.getDeploymentPolicyByTenant
                    (deploymentPolicyDefinitionBean.getId(), carbonContext.getTenantId());
            if (log.isDebugEnabled()) {
                log.debug(String.format("Updating deployment policy: [tenant-id] %d [deployment-policy-uuid] %s " +
                                "[deployment-policy-id] %s ", tenantId, deploymentPolicy.getUuid(),
                        deploymentPolicyDefinitionBean.getId()));
            }
            AutoscalerServiceClient.getInstance().updateDeploymentPolicy(ObjectConverter
                    .convertDeploymentPolicyBeanToASDeploymentPolicy(deploymentPolicyDefinitionBean,
                            deploymentPolicy.getUuid(), carbonContext.getTenantId()));

            if (log.isDebugEnabled()) {
                log.debug(String.format("DeploymentPolicy updated successfully : [tenant-id] " +
                                "%d [deployment-policy-uuid] %s [deployment-policy-id] %s ", tenantId,
                        deploymentPolicy.getUuid(), deploymentPolicyDefinitionBean.getId()));
            }
        } catch (RemoteException e) {

            String msg = "Could not update deployment policy " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (AutoscalerServiceCloudControllerConnectionExceptionException e) {

            String msg = "Could not update deployment policy " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (AutoscalerServiceRemoteExceptionException e) {

            String msg = "Could not update deployment policy " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    /**
     * Remove deployment policy
     *
     * @param deploymentPolicyId Deployment policy Id
     * @throws RestAPIException
     */
    public static void removeDeploymentPolicy(String deploymentPolicyId)
            throws RestAPIException, AutoscalerServiceDeploymentPolicyNotExistsExceptionException,
            AutoscalerServiceUnremovablePolicyExceptionException {

        try {
            AutoscalerServiceClient serviceClient = AutoscalerServiceClient.getInstance();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            DeploymentPolicy deploymentPolicy = serviceClient.getDeploymentPolicyByTenant
                    (deploymentPolicyId, carbonContext.getTenantId());
            AutoscalerServiceClient.getInstance().removeDeploymentPolicy(deploymentPolicy.getUuid());

        } catch (RemoteException e) {
            String msg = "Could not remove deployment policy: [deployment-policy-id] " + deploymentPolicyId + e
                    .getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }


    public static ClusterBean getClusterInfo(String clusterId) throws RestAPIException {
        if (StringUtils.isEmpty(clusterId)) {
            throw new ClusterIdIsEmptyException("Cluster Id can not be empty");
        }

        Cluster cluster = TopologyManager.getTopology().getCluster(clusterId);
        if (cluster == null) {
            return null;
        }

        return ObjectConverter.convertClusterToClusterBean(cluster, clusterId);
    }

    //util methods for Tenants

    /**
     * Add Tenant
     *
     * @param tenantInfoBean TenantInfoBean
     * @throws RestAPIException
     */
    public static void addTenant(org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean) throws RestAPIException,
            InvalidEmailException {

        try {
            CommonUtil.validateEmail(tenantInfoBean.getEmail());
        } catch (Exception e) {
            throw new InvalidEmailException(e.getMessage());
        }

        String tenantDomain = tenantInfoBean.getTenantDomain();
        try {
            TenantMgtUtil.validateDomain(tenantDomain);
        } catch (Exception e) {
            String msg = "Tenant domain validation error for tenant " + tenantDomain;
            log.error(msg, e);
            throw new InvalidDomainException(msg);
        }

        UserRegistry userRegistry = (UserRegistry) PrivilegedCarbonContext.getThreadLocalCarbonContext().
                getRegistry(RegistryType.USER_GOVERNANCE);
        if (userRegistry == null) {
            String msg = "Security alert! User registry is null. A user is trying create a tenant "
                    + " without an authenticated session.";
            log.error(msg);
            throw new RestAPIException("Could not add tenant: Session is not authenticated");
        }

        if (userRegistry.getTenantId() != MultitenantConstants.SUPER_TENANT_ID) {
            String msg = "Security alert! None super tenant trying to create a tenant.";
            log.error(msg);
            throw new RestAPIException(msg);
        }

        Tenant tenant = TenantMgtUtil
                .initializeTenant(ObjectConverter.convertTenantInfoBeanToCarbonTenantInfoBean(tenantInfoBean));
        TenantPersistor persistor = ServiceHolder.getTenantPersistor();
        // not validating the domain ownership, since created by super tenant
        int tenantId; //TODO verify whether this is the correct approach (isSkeleton)
        try {
            tenantId = persistor
                    .persistTenant(tenant, false, tenantInfoBean.getSuccessKey(), tenantInfoBean.getOriginatedService(),
                            false);
        } catch (Exception e) {
            String msg = "Could not add tenant: " + e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        tenantInfoBean.setTenantId(tenantId);

        try {
            TenantMgtUtil.addClaimsToUserStoreManager(tenant);
        } catch (Exception e) {
            String msg = "Error in granting permissions for tenant " + tenantDomain + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        //Notify tenant addition
        try {
            TenantMgtUtil.triggerAddTenant(ObjectConverter.convertTenantInfoBeanToCarbonTenantInfoBean(tenantInfoBean));
        } catch (StratosException e) {
            String msg = "Error in notifying tenant addition.";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        // For the super tenant tenant creation, tenants are always activated as they are created.
        try {
            TenantMgtUtil.activateTenantInitially(
                    ObjectConverter.convertTenantInfoBeanToCarbonTenantInfoBean(tenantInfoBean), tenantId);
        } catch (Exception e) {
            String msg = "Error in initial activation of tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        try {
            TenantMgtUtil.prepareStringToShowThemeMgtPage(tenant.getId());
        } catch (RegistryException e) {
            String msg = "Error in preparing theme mgt page for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    /**
     * @param tenantInfoBean TenantInfoBean
     * @throws RestAPIException
     * @throws InvalidEmailException
     * @throws RegistryException
     */
    public static void updateExistingTenant(org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean) throws
            RestAPIException, RegistryException, InvalidEmailException {

        TenantManager tenantManager = ServiceHolder.getTenantManager();
        UserStoreManager userStoreManager;

        // filling the non-set admin and admin password first
        UserRegistry configSystemRegistry = ServiceHolder.getRegistryService()
                .getConfigSystemRegistry(tenantInfoBean.getTenantId());

        String tenantDomain = tenantInfoBean.getTenantDomain();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
            if (tenantId == -1) {
                String errorMsg = "The tenant with domain name: " + tenantDomain + " does not exist.";
                throw new InvalidDomainException(errorMsg);
            }
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
            if (tenant == null) {
                String errorMsg = "The tenant with tenant id: " + tenantId + " does not exist.";
                throw new TenantNotFoundException(errorMsg);
            }
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant from tenant id: " +
                    tenantId + ".";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        // filling the first and last name values
        if (StringUtils.isBlank(tenantInfoBean.getFirstName())) {
            try {
                CommonUtil.validateName(tenantInfoBean.getFirstName(), "First Name");
            } catch (Exception e) {
                String msg = "Invalid first name is provided.";
                log.error(msg, e);
                throw new RestAPIException(msg, e);
            }
        }
        if (StringUtils.isBlank(tenantInfoBean.getLastName())) {
            try {
                CommonUtil.validateName(tenantInfoBean.getLastName(), "Last Name");
            } catch (Exception e) {
                String msg = "Invalid last name is provided.";
                log.error(msg, e);
                throw new RestAPIException(msg, e);
            }
        }

        tenant.setAdminFirstName(tenantInfoBean.getFirstName());
        tenant.setAdminLastName(tenantInfoBean.getLastName());
        try {
            TenantMgtUtil.addClaimsToUserStoreManager(tenant);
        } catch (Exception e) {
            String msg = "Error in adding claims to the user.";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        // filling the email value
        if (StringUtils.isBlank(tenantInfoBean.getEmail())) {
            // validate the email
            try {
                CommonUtil.validateEmail(tenantInfoBean.getEmail());
            } catch (Exception e) {
                String msg = "Invalid email is provided.";
                log.error(msg, e);
                throw new InvalidEmailException(msg);
            }
            tenant.setEmail(tenantInfoBean.getEmail());
        }

        UserRealm userRealm = configSystemRegistry.getUserRealm();
        try {
            userStoreManager = userRealm.getUserStoreManager();
        } catch (UserStoreException e) {
            String msg = "Error in getting the user store manager for tenant, tenant domain: " +
                    tenantDomain + "." + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        boolean updatePassword = false;
        if (tenantInfoBean.getAdminPassword() != null && !tenantInfoBean.getAdminPassword().equals("")) {
            updatePassword = true;
        }
        try {
            if (!userStoreManager.isReadOnly() && updatePassword) {
                // now we will update the tenant admin with the admin given
                // password.
                try {
                    userStoreManager.updateCredentialByAdmin(tenantInfoBean.getAdmin(), tenantInfoBean.getAdminPassword());
                } catch (UserStoreException e) {
                    String msg = "Error in changing the tenant admin password, tenant domain: " +
                            tenantInfoBean.getTenantDomain() + ". " + e.getMessage() + " for: " +
                            tenantInfoBean.getAdmin();
                    log.error(msg, e);
                    throw new RestAPIException(msg, e);
                }
            } else {
                //Password should be empty since no password update done
                tenantInfoBean.setAdminPassword("");
            }
        } catch (UserStoreException e) {
            String msg = "Error in getting the user store manager is read only " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        try {
            tenantManager.updateTenant(tenant);
        } catch (UserStoreException e) {
            String msg = "Error in updating the tenant for tenant domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        //Notify tenant update to all listeners
        try {
            TenantMgtUtil
                    .triggerUpdateTenant(ObjectConverter.convertTenantInfoBeanToCarbonTenantInfoBean(tenantInfoBean));
        } catch (StratosException e) {
            String msg = "Error in notifying tenant update.";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }
    }

    /**
     * Get a Tenant by Domain
     *
     * @param tenantDomain TenantInfoBean
     * @return TenantInfoBean
     * @throws Exception
     */
    public static org.apache.stratos.common.beans.TenantInfoBean getTenantByDomain(String tenantDomain) throws Exception {

        TenantManager tenantManager = ServiceHolder.getTenantManager();

        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " +
                    tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant from the tenant manager.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        TenantInfoBean bean;
        try {
            bean = ObjectConverter
                    .convertCarbonTenantInfoBeanToTenantInfoBean(TenantMgtUtil.initializeTenantInfoBean(tenantId, tenant));
        } catch (Exception e) {
            log.error(String.format("Couldn't find tenant for provided tenant domain. [Tenant Domain] %s", tenantDomain), e);
            return null;
        }

        // retrieve first and last names from the UserStoreManager
        bean.setFirstName(ClaimsMgtUtil.getFirstNamefromUserStoreManager(ServiceHolder.getRealmService(), tenantId));
        bean.setLastName(ClaimsMgtUtil.getLastNamefromUserStoreManager(ServiceHolder.getRealmService(), tenantId));

        return bean;
    }

    /**
     * Get a list of available Tenants
     *
     * @return list of available Tenants
     * @throws RestAPIException
     */
    public static List<org.apache.stratos.common.beans.TenantInfoBean> getAllTenants() throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        Tenant[] tenants;
        try {
            tenants = (Tenant[]) tenantManager.getAllTenants();
        } catch (Exception e) {
            String msg = "Error in retrieving the tenant information";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        List<org.apache.stratos.common.beans.TenantInfoBean> tenantList = new ArrayList<org.apache.stratos.common.beans.TenantInfoBean>();
        for (Tenant tenant : tenants) {
            org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean = ObjectConverter
                    .convertCarbonTenantInfoBeanToTenantInfoBean(
                            TenantMgtUtil.getTenantInfoBeanfromTenant(tenant.getId(), tenant));
            tenantList.add(tenantInfoBean);
        }
        return tenantList;
    }

    /**
     * Get List of Partial Tenant Domains
     *
     * @param domain domain Name
     * @return List of Partial Tenant Domains
     * @throws RestAPIException
     */
    public static List<org.apache.stratos.common.beans.TenantInfoBean> searchPartialTenantsDomains(String domain)
            throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        Tenant[] tenants;
        try {
            domain = domain.trim();
            tenants = (Tenant[]) tenantManager.getAllTenantsForTenantDomainStr(domain);
        } catch (Exception e) {
            String msg = "Error in retrieving the tenant information.";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        List<org.apache.stratos.common.beans.TenantInfoBean> tenantList = new ArrayList<org.apache.stratos.common.beans.TenantInfoBean>();
        for (Tenant tenant : tenants) {
            org.apache.stratos.common.beans.TenantInfoBean bean = ObjectConverter
                    .convertCarbonTenantInfoBeanToTenantInfoBean(
                            TenantMgtUtil.getTenantInfoBeanfromTenant(tenant.getId(), tenant));
            tenantList.add(bean);
        }
        return tenantList;
    }


    /**
     * Activate a Tenant
     *
     * @param tenantDomain tenantDomainName
     * @throws RestAPIException
     */
    public static void activateTenant(String tenantDomain) throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
            if (tenantId != -1) {
                try {
                    TenantMgtUtil.activateTenant(tenantDomain, tenantManager, tenantId);

                } catch (Exception e) {
                    String msg = "Error in activating Tenant :" + tenantDomain;
                    log.error(msg, e);
                    throw new RestAPIException(msg, e);
                }

                //Notify tenant activation all listeners
                try {
                    TenantMgtUtil.triggerTenantActivation(tenantId);
                } catch (StratosException e) {
                    String msg = "Error in notifying tenant activate.";
                    log.error(msg, e);
                    throw new RestAPIException(msg, e);
                }
            } else {
                String msg = "The tenant with domain name: " + tenantDomain + " does not exist.";
                throw new InvalidDomainException(msg);
            }
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }
    }

    /**
     * Deactivate Tenant
     *
     * @param tenantDomain tenantDomain
     * @throws RestAPIException
     */
    public static void deactivateTenant(String tenantDomain) throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
            if (tenantId != -1) {
                try {
                    TenantMgtUtil.deactivateTenant(tenantDomain, tenantManager, tenantId);
                } catch (Exception e) {
                    String msg = "Error in deactivating Tenant :" + tenantDomain;
                    log.error(msg, e);
                    throw new RestAPIException(msg, e);
                }

                //Notify tenant deactivation all listeners
                try {
                    TenantMgtUtil.triggerTenantDeactivation(tenantId);
                } catch (StratosException e) {
                    String msg = "Error in notifying tenant deactivate.";
                    log.error(msg, e);
                    throw new RestAPIException(msg, e);
                }
            } else {
                String msg = "The tenant with domain name: " + tenantDomain + " does not exist.";
                throw new InvalidDomainException(msg);
            }
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " +
                    tenantDomain + ".";
            log.error(msg, e);
            throw new RestAPIException(msg, e);

        }


    }

    //Util methods for Users

    /**
     * Adds an User
     *
     * @param userInfoBean User Info
     * @throws RestAPIException
     */
    public static void addUser(UserInfoBean userInfoBean) throws RestAPIException {
        try {
            StratosUserManagerUtils.addUser(getTenantUserStoreManager(), userInfoBean);
        } catch (UserManagerException e) {
            String msg = "Error in adding User";
            log.error(msg, e);
            throw new RestAPIException(e.getMessage());
        }
    }


    /**
     * Get Tenant UserStoreManager
     *
     * @return UserStoreManager
     * @throws UserManagerException
     */
    private static UserStoreManager getTenantUserStoreManager() throws UserManagerException {

        CarbonContext carbonContext = CarbonContext.getThreadLocalCarbonContext();
        UserRealm userRealm;
        UserStoreManager userStoreManager;

        try {
            userRealm = carbonContext.getUserRealm();
            userStoreManager = userRealm.getUserStoreManager();

        } catch (UserStoreException e) {
            String msg = "Error in retrieving UserStore Manager";
            log.error(msg, e);
            throw new UserManagerException(msg, e);
        }

        return userStoreManager;
    }

    /**
     * Delete an user
     *
     * @param userName userName
     * @throws RestAPIException
     */
    public static void removeUser(String userName) throws RestAPIException {
        try {
            StratosUserManagerUtils.removeUser(getTenantUserStoreManager(), userName);
        } catch (UserManagerException e) {
            String msg = "Error in removing user :" + userName;
            log.error(msg, e);
            throw new RestAPIException(e.getMessage());
        }
    }

    /**
     * Update User
     *
     * @param userInfoBean UserInfoBean
     * @throws RestAPIException
     */
    public static void updateUser(UserInfoBean userInfoBean) throws RestAPIException {
        try {
            StratosUserManagerUtils.updateUser(getTenantUserStoreManager(), userInfoBean);

        } catch (UserManagerException e) {
            String msg = "Error in updating user";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

    }

    /**
     * Get List of Users
     *
     * @return List of Users
     * @throws RestAPIException
     */
    public static List<UserInfoBean> getUsers() throws RestAPIException {
        List<UserInfoBean> userList;
        try {
            userList = StratosUserManagerUtils.getAllUsers(getTenantUserStoreManager());
        } catch (UserManagerException e) {
            String msg = "Error in retrieving users";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }
        return userList;
    }

    /**
     * This method is to validate the cartridge duplication in the group definition recursively for group within groups
     *
     * @param groupBean - cartridge group definition
     * @throws InvalidCartridgeGroupDefinitionException - throws when the group definition is invalid
     */
    private static void validateCartridgeDuplicationInGroupDefinition(CartridgeGroupBean groupBean)
            throws InvalidCartridgeGroupDefinitionException {
        if (groupBean == null) {
            return;
        }
        List<String> cartridges = new ArrayList<String>();
        if (groupBean.getCartridges() != null) {
            if (groupBean.getCartridges().size() > 1) {
                cartridges.addAll(groupBean.getCartridges());
                validateCartridgeDuplicationInGroup(cartridges);
            }
        }
        if (groupBean.getGroups() != null) {
            //Recursive because to check groups inside groups
            for (CartridgeGroupBean group : groupBean.getGroups()) {
                validateCartridgeDuplicationInGroupDefinition(group);
            }
        }
    }

    /**
     * This method is to validate the duplication of cartridges from the given list
     *
     * @param cartridgeNames - list of strings which holds the cartridgeTypes values
     * @throws InvalidCartridgeGroupDefinitionException - throws when the cartridges are duplicated
     */
    private static void validateCartridgeDuplicationInGroup(List<String> cartridgeNames)
            throws InvalidCartridgeGroupDefinitionException {
        List<String> checkList = new ArrayList<String>();
        for (String cartridgeName : cartridgeNames) {
            if (!checkList.contains(cartridgeName)) {
                checkList.add(cartridgeName);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Duplicate cartridges defined: " + cartridgeName);
                }
                throw new InvalidCartridgeGroupDefinitionException("Invalid cartridge group definition, " +
                        "duplicate cartridges defined: " + cartridgeName);
            }
        }
    }


    /**
     * This is a wrapper method to invoke validateGroupDuplicationInGroupDefinition with a new arraylist of string
     *
     * @param groupBean - cartridge group definition
     * @throws InvalidCartridgeGroupDefinitionException
     */
    private static void validateGroupDuplicationInGroupDefinition(CartridgeGroupBean groupBean)
            throws InvalidCartridgeGroupDefinitionException, RemoteException {
        AutoscalerServiceClient autoscalerServiceClient = AutoscalerServiceClient.getInstance();
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        ServiceGroup serviceGroup = autoscalerServiceClient.getServiceGroupByTenant(groupBean.getName(),
                +                carbonContext.getTenantId());
        validateGroupDuplicationInGroupDefinition(serviceGroup, new ArrayList<String>());
    }

    /**
     * This is to validate the group duplication in the group definition recursively for group within groups
     *
     * @param serviceGroup    - cartridge group definition
     * @param parentGroups - list of string which holds the parent group names (all parents in the hierarchy)
     * @throws InvalidCartridgeGroupDefinitionException - throws when the group definition is invalid
     */
    private static void validateGroupDuplicationInGroupDefinition(ServiceGroup serviceGroup, List<String> parentGroups)
            throws InvalidCartridgeGroupDefinitionException {
        if (serviceGroup == null) {
            return;
        }
        List<String> groups = new ArrayList<String>();
        parentGroups.add(serviceGroup.getUuid());

        if (serviceGroup.getGroups() != null) {
            for (ServiceGroup group : serviceGroup.getGroups()) {
                if (group != null) {
                    groups.add(group.getUuid());
                }
            }
            validateGroupDuplicationInGroup(groups, parentGroups);
        }
        if (serviceGroup.getGroups() != null) {
            //Recursive because to check groups inside groups
            for (ServiceGroup group : serviceGroup.getGroups()) {
                validateGroupDuplicationInGroupDefinition(group, parentGroups);
                parentGroups.remove(serviceGroup.getUuid());
            }
        }
    }

    /**
     * This method is to validate the duplication of groups in the same level and to validate cyclic behaviour of groups
     *
     * @param groups       - cartridge group definition
     * @param parentGroups - list of string which holds the parent group names (all parents in the hierarchy)
     * @throws InvalidCartridgeGroupDefinitionException - throws when group duplicate or when cyclic behaviour occurs
     */
    private static void validateGroupDuplicationInGroup(List<String> groups, List<String> parentGroups)
            throws InvalidCartridgeGroupDefinitionException {
        List<String> checkList = new ArrayList<String>();
        for (String group : groups) {
            if (!checkList.contains(group)) {
                checkList.add(group);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Duplicate group defined: " + group);
                }
                throw new InvalidCartridgeGroupDefinitionException("Invalid cartridge group definition, " +
                        "duplicate groups defined: " + group);
            }
            if (parentGroups.contains(group)) {
                if (log.isDebugEnabled()) {
                    log.debug("Cyclic group behaviour identified [group-name]: " + group);
                }
                throw new InvalidCartridgeGroupDefinitionException("Invalid cartridge group definition, " +
                        "cyclic group behaviour identified: " + group);
            }
        }
    }

    /**
     * Get Iaas Providers
     *
     * @return Array of Strings
     */
    public static IaasProviderInfoBean getIaasProviders() throws RestAPIException {
        try {
            CloudControllerServiceClient serviceClient = CloudControllerServiceClient.getInstance();
            String[] iaasProviders = serviceClient.getIaasProviders();
            return ObjectConverter.convertStringArrayToIaasProviderInfoBean(iaasProviders);
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Get Service Group uuid by TenantId
     *
     * @return String Uuid
     */
    public static String getServiceGroupUuidByTenant(String serviceGroupName,
                                                     int tenantId) throws RestAPIException {
        try {
            AutoscalerServiceClient autoscalerServiceClient = AutoscalerServiceClient.getInstance();
            if(autoscalerServiceClient.getServiceGroupByTenant(serviceGroupName, tenantId)!=null) {
                return (autoscalerServiceClient.getServiceGroupByTenant(serviceGroupName, tenantId).getUuid());
            }
            else{
                return null;
            }
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Get deployment policy uuid by TenantId
     *
     * @return String Uuid
     */
    public static String getDeploymentPolicyUuidByTenant(String deploymentPolicyId,
                                                         int tenantId) throws RestAPIException {
        try {
            AutoscalerServiceClient autoscalerServiceClient = AutoscalerServiceClient.getInstance();
            return (autoscalerServiceClient.getDeploymentPolicyByTenant(deploymentPolicyId, tenantId).getUuid());
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }

    public static String getKubernetesClusterUuidByTenant(String clusterId,int tenantId) throws RestAPIException {

        try {
            return CloudControllerServiceClient.getInstance().getKubernetesClusterByTenantId(clusterId, tenantId)
                    .getClusterUuid();
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }
}
