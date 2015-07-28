package org.apache.stratos.manager.persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.exception.ApplicationSignUpException;
import org.apache.stratos.manager.model.ApplicationSignUpEntity;
import org.apache.stratos.manager.model.ApplicationSignUpEntityPK;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import javax.persistence.Persistence;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by aarthy on 7/6/15.
 */
public class PersistenceManager implements ApplicationSignUp {

    private static final Log log = LogFactory.getLog(PersistenceManager.class);

    private EntityManagerFactory entitymanagerFactory = Persistence.createEntityManagerFactory("PersistenceUnit");

    EntityManager entityManager = null;


    //region ApplicationSignUp Implementation
    @Override
    public void addApplicationSignUp(ApplicationSignUpEntity applicationSignUpEntity) throws ApplicationSignUpException{



        if (applicationSignUpEntity == null) {
            throw new ApplicationSignUpException("Application signup is null");
        }

        String applicationId = applicationSignUpEntity.getApplicationId();
        int tenantId = applicationSignUpEntity.getTenantId();
        String clusterId = applicationSignUpEntity.getClusterClusterId();
        List<String> clusterIdList = new ArrayList<String>();
        if (clusterId != null) {
                clusterIdList.add(clusterId);

        }

        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Adding application signup: [application-id] %s [tenant-id] %d",
                        applicationId, tenantId));
            }
            if (applicationSignUpExists(applicationSignUpEntity)) {
                throw new RuntimeException(String.format("Tenant has already signed up for application: " +
                        "[application-id] %s [tenant-id] %d", applicationId, tenantId));

            }

            entityManager = this.entitymanagerFactory.createEntityManager();
            entityManager.getTransaction().begin();
            entityManager.persist(applicationSignUpEntity);
            entityManager.flush();
            entityManager.getTransaction().commit();

            if (log.isInfoEnabled()) {
                log.info(String.format("Application signup added successfully: [application-id] %s [tenant-id] %d",
                        applicationId, tenantId));


            }
        } catch (Exception e) {

            entityManager.getTransaction().rollback();
            String message = "Could not add application signup";
            log.error(message, e);
            throw new ApplicationSignUpException(message,e);
        }

        finally {
            if(entityManager!=null)
            {
                entityManager.close();
            }
        }
    }

    @Override
    public boolean applicationSignUpExists(ApplicationSignUpEntity applicationSignUpEntity) {

        entityManager=this.entitymanagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        ApplicationSignUpEntityPK pk=new ApplicationSignUpEntityPK();
        pk.setApplicationId(applicationSignUpEntity.getApplicationId());
        pk.setTenantId(applicationSignUpEntity.getTenantId());
        pk.setClusterClusterId(applicationSignUpEntity.getClusterClusterId());
        pk.setArtifactRepositoryAlias(applicationSignUpEntity.getArtifactRepositoryAlias());
        pk.setDomainMappingDomainName(applicationSignUpEntity.getDomainMappingDomainName());

        ApplicationSignUpEntity applicationSignUp =entityManager.find(ApplicationSignUpEntity.class,pk);
        entityManager.flush();
        entityManager.getTransaction().commit();
        return (applicationSignUp!=null);
    }


    @Override
    public boolean deleteApplicationSignUp(String applicationId, int tenantId) {
        return false;
    }
    //endregion
}


