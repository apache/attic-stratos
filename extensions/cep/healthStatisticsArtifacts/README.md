CEP Configuration For Health Stat UI
====================================


CEP Configuration
=================

Prerequisites:

Download MySQL JDBC connector for Java add it to [Apache Stratos] OR
[External CEP HOME ]/repository/components/lib/ directory.

Configuring the datasource
==========================

You can find the master-datasource.xml file in [Apache Stratos] OR
[External CEP HOME ]/repository/conf/datasources.Create a datasource to connect with the
MYSQL server as u prefer or you can copy paste this into master-datasource.xml

<datasource>
   <name>DataSourcetoPublishHealthStatRDBMS</name>
   <description>The datasource used for registry and user manager</description>
   <jndiConfig>
      <name>jdbc/[Your DB:DataSourcetoPublishHealthStatRDBMS]</name>
   </jndiConfig>
   <definition type="RDBMS">
    <configuration>
      <url>jdbc:mysql://localhost:3306/[Your DB:DataSourcetoPublishHealthStatRDBMS]</url>
      <username>wso2carbon</username>
      <password>wso2carbon</password>
      <driverClassName>com.mysql.jdbc.Driver</driverClassName>
      <maxActive>50</maxActive>
      <maxWait>60000</maxWait>
      <testOnBorrow>true</testOnBorrow>
      <validationQuery>SELECT 1</validationQuery>
      <validationInterval>30000</validationInterval>
    </configuration>
   </definition>
</datasource>

Configuring output: MySQL event Adapter to publish health stats from CEP to RDBMS
=================================================================================

As the very first step we have to create the data source.I will explain how to configure output
MySQL event adapter using the management console. Deploy the event adapter which is in
artifacts/outputeventadaptors/OutPutadaptorRDBMStoPublishHealthStat.xml into
[Apache Stratos] OR [External CEP HOME ]/repository/deployment/server/outputeventadaptors/ directory.
Please find OutPutadaptorRDBMStoPublishHealthStat here[2] and paste it to the above directory.

Configuring Event fomratters.
=============================

You can specify an event formatter configuration using an XML file and save it in
[Apache Stratos] OR [External CEP HOME ]/repository/deployment/server/eventformatters directory.
In here you can get the 3 formatters artifacts/eventformatters/

Names,

MemberAverageLoadAverageEventFormatterHealthStat
MemberAverageMemoryAverageEventFormatterHealthStat
FlightRequestEventFormatterHealthStat

depoly them in the previously mentioned directory.