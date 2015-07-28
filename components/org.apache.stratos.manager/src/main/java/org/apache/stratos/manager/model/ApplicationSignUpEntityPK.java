package org.apache.stratos.manager.model;

import javax.persistence.*;

import java.io.Serializable;

/**
 * Created by aarthy on 6/29/15.
 */
public class ApplicationSignUpEntityPK implements Serializable {
    private String applicationId;
    private int tenantId;
    private String clusterClusterId;
    private String domainMappingDomainName;
    private String artifactRepositoryAlias;

    @Column(name = "applicationId")
    @Id
    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    @Column(name = "tenantId")
    @Id
    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    @Column(name = "Cluster_clusterId")
    @Id
    public String getClusterClusterId() {
        return clusterClusterId;
    }

    public void setClusterClusterId(String clusterClusterId) {
        this.clusterClusterId = clusterClusterId;
    }

    @Column(name = "DomainMapping_domainName")
    @Id
    public String getDomainMappingDomainName() {
        return domainMappingDomainName;
    }

    public void setDomainMappingDomainName(String domainMappingDomainName) {
        this.domainMappingDomainName = domainMappingDomainName;
    }

    @Column(name = "ArtifactRepository_alias")
    @Id
    public String getArtifactRepositoryAlias() {
        return artifactRepositoryAlias;
    }

    public void setArtifactRepositoryAlias(String artifactRepositoryAlias) {
        this.artifactRepositoryAlias = artifactRepositoryAlias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApplicationSignUpEntityPK that = (ApplicationSignUpEntityPK) o;

        if (tenantId != that.tenantId) return false;
        if (applicationId != null ? !applicationId.equals(that.applicationId) : that.applicationId != null)
            return false;
        if (clusterClusterId != null ? !clusterClusterId.equals(that.clusterClusterId) : that.clusterClusterId != null)
            return false;
        if (domainMappingDomainName != null ? !domainMappingDomainName.equals(that.domainMappingDomainName) : that.domainMappingDomainName != null)
            return false;
        if (artifactRepositoryAlias != null ? !artifactRepositoryAlias.equals(that.artifactRepositoryAlias) : that.artifactRepositoryAlias != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = applicationId != null ? applicationId.hashCode() : 0;
        result = 31 * result + tenantId;
        result = 31 * result + (clusterClusterId != null ? clusterClusterId.hashCode() : 0);
        result = 31 * result + (domainMappingDomainName != null ? domainMappingDomainName.hashCode() : 0);
        result = 31 * result + (artifactRepositoryAlias != null ? artifactRepositoryAlias.hashCode() : 0);
        return result;
    }
}
