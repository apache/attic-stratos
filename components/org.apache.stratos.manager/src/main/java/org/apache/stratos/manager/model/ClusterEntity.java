package org.apache.stratos.manager.model;

import javax.persistence.*;
/**
 * Created by aarthy on 6/29/15.
 */
@Entity
@Table(name = "Cluster", schema = "", catalog = "StratosManager")
public class ClusterEntity {
    private String clusterId;
    private String alias;
    private String serviceName;
    private String tenantRange;
    private String isLbCluster;

    @Id
    @Column(name = "clusterId")
    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    @Basic
    @Column(name = "alias")
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Basic
    @Column(name = "serviceName")
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Basic
    @Column(name = "tenantRange")
    public String getTenantRange() {
        return tenantRange;
    }

    public void setTenantRange(String tenantRange) {
        this.tenantRange = tenantRange;
    }


    @Basic
    @Column(name = "isLbCluster")
    public String getIsLbCluster() {
        return isLbCluster;
    }

    public void setIsLbCluster(String isLbCluster) {
        this.isLbCluster = isLbCluster;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClusterEntity that = (ClusterEntity) o;

        if (clusterId != null ? !clusterId.equals(that.clusterId) : that.clusterId != null) return false;
        if (alias != null ? !alias.equals(that.alias) : that.alias != null) return false;
        if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;
        if (tenantRange != null ? !tenantRange.equals(that.tenantRange) : that.tenantRange != null) return false;
        if (isLbCluster != null ? !isLbCluster.equals(that.isLbCluster) : that.isLbCluster != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = clusterId != null ? clusterId.hashCode() : 0;
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
        result = 31 * result + (tenantRange != null ? tenantRange.hashCode() : 0);
        result = 31 * result + (isLbCluster != null ? isLbCluster.hashCode() : 0);
        return result;
    }
}
