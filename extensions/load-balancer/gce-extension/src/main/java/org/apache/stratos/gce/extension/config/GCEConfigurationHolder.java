package org.apache.stratos.gce.extension.config;

/**
 * GCE config parser parse the gce-configuration.xml and stores
 * the configuration in this singleton class to be used in the runtime.
 */
public class GCEConfigurationHolder {

    private static volatile GCEConfigurationHolder instance;

    private String cepStatsPublisherEnabled;
    private String thriftReceiverIp;
    private String thriftReceiverPort;
    private String namePrefix;
    private String projectName;
    private String projectId;
    private String zoneName;
    private String regionName;
    private String keyFilePath;
    private String gceAccountId;
    private String healthCheckRequestPath;
    private String healthCheckRequestPort;
    private String healthCheckTimeoutSec;
    private String healthCheckUnhealthyThreshold;
    private String networkName;
    private String operationTimeout;

    //private constructor
    private GCEConfigurationHolder(){}

    public static GCEConfigurationHolder getInstance() {
        if (instance == null) {
            synchronized (GCEConfigurationHolder.class) {
                if (instance == null) {
                    instance = new GCEConfigurationHolder();
                }
            }
        }
        return instance;
    }

    public static void setInstance(GCEConfigurationHolder instance) {
        GCEConfigurationHolder.instance = instance;
    }

    public String getCepStatsPublisherEnabled() {
        return cepStatsPublisherEnabled;
    }

    public void setCepStatsPublisherEnabled(String cepStatsPublisherEnabled) {
        this.cepStatsPublisherEnabled = cepStatsPublisherEnabled;
    }

    public String getThriftReceiverIp() {
        return thriftReceiverIp;
    }

    public void setThriftReceiverIp(String thriftReceiverIp) {
        this.thriftReceiverIp = thriftReceiverIp;
    }

    public String getThriftReceiverPort() {
        return thriftReceiverPort;
    }

    public void setThriftReceiverPort(String thriftReceiverPort) {
        this.thriftReceiverPort = thriftReceiverPort;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getKeyFilePath() {
        return keyFilePath;
    }

    public void setKeyFilePath(String keyFilePath) {
        this.keyFilePath = keyFilePath;
    }

    public String getGceAccountId() {
        return gceAccountId;
    }

    public void setGceAccountId(String gceAccountId) {
        this.gceAccountId = gceAccountId;
    }

    public String getHealthCheckRequestPath() {
        return healthCheckRequestPath;
    }

    public void setHealthCheckRequestPath(String healthCheckRequestPath) {
        this.healthCheckRequestPath = healthCheckRequestPath;
    }

    public String getHealthCheckRequestPort() {
        return healthCheckRequestPort;
    }

    public void setHealthCheckRequestPort(String healthCheckRequestPort) {
        this.healthCheckRequestPort = healthCheckRequestPort;
    }

    public String getHealthCheckTimeoutSec() {
        return healthCheckTimeoutSec;
    }

    public void setHealthCheckTimeoutSec(String healthCheckTimeoutSec) {
        this.healthCheckTimeoutSec = healthCheckTimeoutSec;
    }

    public String getHealthCheckUnhealthyThreshold() {
        return healthCheckUnhealthyThreshold;
    }

    public void setHealthCheckUnhealthyThreshold(String healthCheckUnhealthyThreshold) {
        this.healthCheckUnhealthyThreshold = healthCheckUnhealthyThreshold;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getOperationTimeout() {
        return operationTimeout;
    }

    public void setOperationTimeout(String operationTimeout) {
        this.operationTimeout = operationTimeout;
    }
}
