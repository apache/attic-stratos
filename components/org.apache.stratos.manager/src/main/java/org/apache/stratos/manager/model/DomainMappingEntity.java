package org.apache.stratos.manager.model;

import javax.persistence.*;
/**
 * Created by aarthy on 6/29/15.
 */

@Entity
@Table(name = "DomainMapping", schema = "", catalog = "StratosManager")
public class DomainMappingEntity {
    private String domainName;
    private String serviceName;
    private String contextPath;

    @Id
    @Column(name = "domainName")
    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
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
    @Column(name = "contextPath")
    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DomainMappingEntity that = (DomainMappingEntity) o;

        if (domainName != null ? !domainName.equals(that.domainName) : that.domainName != null) return false;
        if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) return false;
        if (contextPath != null ? !contextPath.equals(that.contextPath) : that.contextPath != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = domainName != null ? domainName.hashCode() : 0;
        result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
        result = 31 * result + (contextPath != null ? contextPath.hashCode() : 0);
        return result;
    }
}
