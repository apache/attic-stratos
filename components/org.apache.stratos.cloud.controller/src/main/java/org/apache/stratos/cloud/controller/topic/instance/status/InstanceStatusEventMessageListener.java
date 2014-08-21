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
package org.apache.stratos.cloud.controller.topic.instance.status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * this is to handle the topology subscription
 */
public class InstanceStatusEventMessageListener implements MessageListener{
    private static final Log log = LogFactory.getLog(InstanceStatusEventMessageListener.class);

    @Override
    public void onMessage(Message message) {
        TextMessage receivedMessage = (TextMessage) message;
        InstanceStatusEventMessageQueue.getInstance().add(receivedMessage);
        if(log.isDebugEnabled()) {
            try {
                log.debug(String.format("Instance status message added to queue: %s", receivedMessage.getText()));
            } catch (JMSException e) {
                log.error(e);
            }
        }
    }
}
