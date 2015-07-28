package org.apache.stratos.manager.Persistence;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.persistence.*;

import java.util.*;
/**
 * Created by aarthy on 7/27/15.
 */
public class PersistenceManager {

    private static final Log log;

    static {
        log = LogFactory.getLog(PersistenceManager.class);
    }

    private EntityManagerFactory entitymanagerFactory = Persistence.createEntityManagerFactory("PersistenceUnit");

    private static final PersistenceManager instance = new PersistenceManager();

    EntityManager entityManager = null;


    public static PersistenceManager getInstance() {
        return instance;
    }


    /**
     *	Add  object to persist
     *  @param object
     *  @throws PersistenceException
     */
    public void add(Object object)
    {
        System.out.printf("entered");

        System.out.printf("true");
        try {
            entityManager = this.entitymanagerFactory.createEntityManager();
            entityManager.getTransaction().begin();
            entityManager.persist(object);
            entityManager.flush();
            entityManager.getTransaction().commit();
            String msg="Added Successfully";
            log.info(msg);
        }
        catch (PersistenceException e)
        {
            String msg="Error while adding";
            log.error(msg);
        }

    }


    /**
     *  remove  object by primary key
     * @param object
     * @param primaryKey
     * @throws PersistenceException
     */

    public void remove(Object object,Object primaryKey)
    {
        try {
            entityManager = this.entitymanagerFactory.createEntityManager();
            entityManager.getTransaction().begin();
            Object found=entityManager.find(object.getClass(), primaryKey);
            if(found!=null) {
                entityManager.remove(found);
                entityManager.getTransaction().commit();
                String msg = "Deleted sucessfully";
                log.info(msg);
            }
            else
            {
                String msg ="Object does not exists";
                log.error(msg);
            }
        }
        catch (PersistenceException e)
        {

            String msg="Error while Deleting";
            log.error(msg);
        }

    }

    /**
     * retrieve an object by primary key
     * @param object
     * @param primaryKey
     * @return
     */

    public Object retrieve(Object object,Object primaryKey)
    {
        Object found=null;
        try{

            entityManager=this.entitymanagerFactory.createEntityManager();
            entityManager.getTransaction().begin();
            found=  entityManager.find(found.getClass(), primaryKey);
            if(found!=null)
                log.info("Object Found");
            else
                log.error("Object not Found");
            return found;

        }
        catch (PersistenceException e)
        {

            String msg="Error while retrieving";
            log.error(msg);
            return found;
        }

    }

    /**
     *
     * @param tableName
     * @return
     */


    public List retrieveAll(String tableName)
    {
        List objectList=new ArrayList<Object>();
        try{

            entityManager=this.entitymanagerFactory.createEntityManager();
            entityManager.getTransaction().begin();
            String msg="Successfully retrieved";

            objectList= entityManager.createQuery("select obj  from "+tableName+" obj" ).getResultList();

            if(objectList!=null)
            {
                log.info(msg);
                return objectList;
            }
            else
                return null;

        }
        catch (javax.persistence.PersistenceException e)
        {
            String msg="Object not found";
            System.out.println(msg);
            return null;
        }

    }

    /**
     *
     * @param object
     * @param primaryKey
     * @param setValues
     * @param whereValues
     */

    public void update(Object object,Object primaryKey,Map<String,Object> setValues,Map<String,Object> whereValues)
    {
        String setQuery="";
        String updateQuery="";
        String whereQuery="";

        try {

            Object foundObject =retrieve(object,primaryKey);

            if(foundObject!=null)
            {

                String query ="Update "+ object +" obj Set ";
                int count =0;
                for (String key : setValues.keySet()) {
                    setQuery += "obj."+key + "=";

                    if(setValues.get(key)instanceof String)
                    {
                        setQuery+="'"+setValues.get(key)+"'";
                    }
                    else
                    {
                        setQuery+=setValues.get(key);
                    }

                    count++;
                    if(setValues.size()>1 && count!=setValues.size())
                    {
                        setQuery+=",";
                    }
                }
                updateQuery=query+setQuery+" Where ";

                int pkCount=0;
                for (String key : whereValues.keySet()){

                    whereQuery+="obj."+key+"=";
                    if(whereValues.get(key)instanceof String)
                    { whereQuery+="'"+whereValues.get(key)+"'";
                    }
                    else
                    {   whereQuery+=whereValues.get(key);
                    }
                    pkCount++;
                    if(whereValues.size()>1 && pkCount!=whereValues.size())
                    { whereQuery+=" and ";
                    }
                }

                updateQuery+=whereQuery;
                entityManager=this.entitymanagerFactory.createEntityManager();

                entityManager.getTransaction().begin();

                Query queryString= entityManager.createQuery(updateQuery);
                int updatedCount =queryString.executeUpdate();

                if (updatedCount==1)
                {
                    entityManager.getTransaction().commit();
                    String msg="updated Successfully";
                    log.info(msg);
                }

                else
                {
                    String msg= "Error while Updating";
                    log.error(msg);
                }
            }
            else {
                String msg="Object not found";
                log.error(msg);
            }

        }
        catch (PersistenceException e)
        {
            String msg="Error while updating";
            log.error(msg);
        }



    }


}
