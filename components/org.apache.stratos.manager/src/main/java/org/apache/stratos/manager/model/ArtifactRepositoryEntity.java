package org.apache.stratos.manager.model;

import javax.persistence.*;
/**
 * Created by aarthy on 6/29/15.
 */
@Entity
@Table(name = "ArtifactRepository", schema = "", catalog = "StratosManager")
public class ArtifactRepositoryEntity {
    private String alias;
    private Byte isPrivateRepo;
    private String repoUserName;
    private String repoPassword;
    private String repoUrl;
    private String catridgeType;

    @Id
    @Column(name = "alias")
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Basic
    @Column(name = "isPrivateRepo")
    public Byte getIsPrivateRepo() {
        return isPrivateRepo;
    }

    public void setIsPrivateRepo(Byte isPrivateRepo) {
        this.isPrivateRepo = isPrivateRepo;
    }

    @Basic
    @Column(name = "repoUserName")
    public String getRepoUserName() {
        return repoUserName;
    }

    public void setRepoUserName(String repoUserName) {
        this.repoUserName = repoUserName;
    }

    @Basic
    @Column(name = "repoPassword")
    public String getRepoPassword() {
        return repoPassword;
    }

    public void setRepoPassword(String repoPassword) {
        this.repoPassword = repoPassword;
    }


    @Basic
    @Column(name = "repoURL")
    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    @Basic
    @Column(name = "catridgeType")
    public String getCatridgeType() {
        return catridgeType;
    }

    public void setCatridgeType(String catridgeType) {
        this.catridgeType = catridgeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArtifactRepositoryEntity that = (ArtifactRepositoryEntity) o;

        if (alias != null ? !alias.equals(that.alias) : that.alias != null) return false;
        if (isPrivateRepo != null ? !isPrivateRepo.equals(that.isPrivateRepo) : that.isPrivateRepo != null)
            return false;
        if (repoUserName != null ? !repoUserName.equals(that.repoUserName) : that.repoUserName != null) return false;
        if (repoPassword != null ? !repoPassword.equals(that.repoPassword) : that.repoPassword != null) return false;
        if (repoUrl != null ? !repoUrl.equals(that.repoUrl) : that.repoUrl != null) return false;
        if (catridgeType != null ? !catridgeType.equals(that.catridgeType) : that.catridgeType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = alias != null ? alias.hashCode() : 0;
        result = 31 * result + (isPrivateRepo != null ? isPrivateRepo.hashCode() : 0);
        result = 31 * result + (repoUserName != null ? repoUserName.hashCode() : 0);
        result = 31 * result + (repoPassword != null ? repoPassword.hashCode() : 0);
        result = 31 * result + (repoUrl != null ? repoUrl.hashCode() : 0);
        result = 31 * result + (catridgeType != null ? catridgeType.hashCode() : 0);
        return result;
    }
}
