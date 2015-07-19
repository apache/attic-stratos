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
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionHandler {
    Connection connection;
    DataSource dataSource = null;
    String stringSql = null;
    ResultSet resultSet = null;
    public ResultSet getsqlConnection(String sql) {

        dataSource = null;
        stringSql = sql;
        resultSet = null;
        try{
            boolean isJndiLookup = true;

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
                Statement statement = connection.createStatement();
                resultSet = statement.executeQuery(stringSql);

            }


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error " + e.getMessage());
        }
        return resultSet;
    }

    public void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }

    }

}

