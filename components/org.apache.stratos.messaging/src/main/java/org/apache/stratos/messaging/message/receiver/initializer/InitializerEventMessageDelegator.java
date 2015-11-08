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
package org.apache.stratos.messaging.message.receiver.initializer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.Message;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;
import org.apache.stratos.messaging.message.processor.initializer.InitializerMessageProcessorChain;

public class InitializerEventMessageDelegator implements Runnable {
    private static final Log log = LogFactory.getLog(InitializerEventMessageDelegator.class);

    private MessageProcessorChain processorChain;
    private InitializerEventMessageQueue messageQueue;
    private boolean terminated;

    public InitializerEventMessageDelegator(InitializerEventMessageQueue initializerEventMessageQueue) {
        this.messageQueue = initializerEventMessageQueue;
        this.processorChain = new InitializerMessageProcessorChain();
    }

    public void addEventListener(EventListener eventListener) {
        processorChain.addEventListener(eventListener);
    }

    @Override
    public void run() {
        try {
            if (log.isInfoEnabled()) {
                log.info("Initializer event message delegator started");
            }

            while (!terminated) {
                try {
                    Message message = messageQueue.take();
                    String type = message.getEventClassName();

                    // Retrieve the actual message
                    String json = message.getText();

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Initializer event message [%s] received from queue: %s", type,
                                messageQueue.getClass()));
                    }

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Delegating initializer event message: %s", type));
                    }
                    processorChain.process(type, json, null);
                } catch (InterruptedException ignore) {
                    log.info("Shutting down initializer event message delegator...");
                    terminate();
                } catch (Exception e) {
                    log.error("Failed to retrieve initializer event message", e);
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Initializer event message delegator failed", e);
            }
        }
    }

    /**
     * Terminate initializer event message delegator thread.
     */
    public void terminate() {
        terminated = true;
    }
}
