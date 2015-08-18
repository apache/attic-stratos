/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.stratos.rest.endpoint.handlers;

import org.wso2.carbon.ndatasource.core.DataSourceManager;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/*This class creates the connection to the database using the datasource Name DataSourcetoPublishHealthStatRDBMS
* and closes the connection after quering the data.
* It will throws the exceptions if occured while creating database connection.
* */
public class ConnectionHandler {

    Connection connection;
    DataSource dataSource = null;
    boolean isJndiLookup = true;

    public Connection getsqlConnection() {

        dataSource = null;

        try{

            if(isJndiLookup) {

                // Obtain the datasource via a JNDI lookup, by passing the jndi config name
                dataSource = (DataSource) InitialContext.doLookup("jdbc/DataSourcetoPublishHealthStatRDBMS");

            } else {

                // Obtain the datasource by passing the data source name
                dataSource = (DataSource) DataSourceManager.getInstance()
                        .getDataSourceRepository().getDataSource("DataSourcetoPublishHealthStatRDBMS")
                        .getDSObject();
            }

            if (dataSource != null) {
                connection = dataSource.getConnection();

            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error " + e.getMessage());
        }
        return connection;
    }

    public void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }

    }

}

