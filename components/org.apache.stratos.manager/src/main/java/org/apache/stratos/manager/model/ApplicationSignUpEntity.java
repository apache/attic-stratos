package org.apache.stratos.manager.model;

import javax.persistence.*;
/**
 * Created by aarthy on 6/29/15.
 */
@Entity
@Table(name = "ApplicationSignUp", schema = "", catalog = "StratosManager")
@IdClass(ApplicationSignUpEntityPK.class)
public class ApplicationSignUpEntity {
    private String applicationId;
    private int tenantId;
    private String clusterClusterId;
    private String domainMappingDomainName;
    private String artifactRepositoryAlias;

    @Id
    @Column(name = "applicationId")
    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    @Id
    @Column(name = "tenantId")
    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    @Id
    @Column(name = "Cluster_clusterId")
    public String getClusterClusterId() {
        return clusterClusterId;
    }

    public void setClusterClusterId(String clusterClusterId) {
        this.clusterClusterId = clusterClusterId;
    }

    @Id
    @Column(name = "DomainMapping_domainName")
    public String getDomainMappingDomainName() {
        return domainMappingDomainName;
    }

    public void setDomainMappingDomainName(String domainMappingDomainName) {
        this.domainMappingDomainName = domainMappingDomainName;
    }

    @Id
    @Column(name = "ArtifactRepository_alias")
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

        ApplicationSignUpEntity that = (ApplicationSignUpEntity) o;

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
