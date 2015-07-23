/*
 *     Licensed to the Apache Software Foundation (ASF) under one
 *     or more contributor license agreements.  See the NOTICE file
 *     distributed with this work for additional information
 *     regarding copyright ownership.  The ASF licenses this file
 *     to you under the Apache License, Version 2.0 (the
 *     "License"); you may not use this file except in compliance
 *     with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing,
 *     software distributed under the License is distributed on an
 *     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *     KIND, either express or implied.  See the License for the
 *     specific language governing permissions and limitations
 *     under the License.
 */
package org.apache.stratos.cep.extension;

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.core.event.AtomicEvent;
import org.wso2.siddhi.core.event.ListEvent;
import org.wso2.siddhi.core.event.StreamEvent;
import org.wso2.siddhi.core.event.in.InEvent;
import org.wso2.siddhi.core.event.in.InListEvent;
import org.wso2.siddhi.core.event.remove.RemoveEvent;
import org.wso2.siddhi.core.event.remove.RemoveListEvent;
import org.wso2.siddhi.core.event.remove.RemoveStream;
import org.wso2.siddhi.core.persistence.ThreadBarrier;
import org.wso2.siddhi.core.query.QueryPostProcessingElement;
import org.wso2.siddhi.core.query.processor.window.RunnableWindowProcessor;
import org.wso2.siddhi.core.query.processor.window.WindowProcessor;
import org.wso2.siddhi.core.util.EventConverter;
import org.wso2.siddhi.core.util.collection.queue.scheduler.SchedulerSiddhiQueueGrid;
import org.wso2.siddhi.core.util.collection.queue.scheduler.timestamp.ISchedulerTimestampSiddhiQueue;
import org.wso2.siddhi.core.util.collection.queue.scheduler.timestamp.SchedulerTimestampSiddhiQueue;
import org.wso2.siddhi.core.util.collection.queue.scheduler.timestamp.SchedulerTimestampSiddhiQueueGrid;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.expression.Expression;;
import org.wso2.siddhi.query.api.expression.constant.IntConstant;
import org.wso2.siddhi.query.api.expression.constant.LongConstant;
import org.wso2.siddhi.query.api.extension.annotation.SiddhiExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SiddhiExtension(namespace = "stratos", function = "curveFitting")
public class CurveFinderWindowProcessor extends WindowProcessor implements RunnableWindowProcessor{

    static final Logger log = Logger.getLogger(CurveFinderWindowProcessor.class);
    /**
     * alpha is used as the smoothing constant in exponential moving average
     */
    public static final double ALPHA = 0.8000;
    private ScheduledExecutorService eventRemoverScheduler;
    private long timeToKeep;
    private ScheduledFuture<?> lastSchedule = null;
    private long constantSchedulingInterval = -1;
    private List<InEvent> newEventList;
    private List<RemoveEvent> oldEventList;
    private ThreadBarrier threadBarrier;
    private ISchedulerTimestampSiddhiQueue<StreamEvent> window;
    private long[] timeStamps;
    private double[] dataValues;
    private double[] smoothedValues;


    @Override
    protected void processEvent(InEvent event) {
        acquireLock();
        try {
            window.put(new RemoveEvent(event, System.currentTimeMillis() + timeToKeep));
            nextProcessor.process(event);
        } finally {
            releaseLock();
        }
    }

    @Override
    protected void processEvent(InListEvent listEvent) {
        acquireLock();
        try {
            if (!async && siddhiContext.isDistributedProcessingEnabled()) {
                long expireTime = System.currentTimeMillis() + timeToKeep;
                for (int i = 0, activeEvents = listEvent.getActiveEvents(); i < activeEvents; i++) {
                    window.put(new RemoveEvent(listEvent.getEvent(i), expireTime));
                }
            } else {
                window.put(new RemoveListEvent(EventConverter.toRemoveEventArray(listEvent.getEvents(), listEvent.getActiveEvents(), System.currentTimeMillis() + timeToKeep)));
            }
            nextProcessor.process(listEvent);
        } finally {
            releaseLock();
        }
    }

    @Override
    public Iterator<StreamEvent> iterator() {
        return window.iterator();
    }

    @Override
    public Iterator<StreamEvent> iterator(String predicate) {
        if (siddhiContext.isDistributedProcessingEnabled()) {
            return ((SchedulerSiddhiQueueGrid<StreamEvent>) window).iterator(predicate);
        } else {
            return window.iterator();
        }
    }


    @Override
    protected Object[] currentState() {
        return new Object[]{window.currentState(), oldEventList, newEventList};
    }

    @Override
    protected void restoreState(Object[] objects) {
        window.restoreState(objects);
        window.restoreState((Object[]) objects[0]);
        oldEventList = ((ArrayList<RemoveEvent>) objects[1]);
        newEventList = ((ArrayList<InEvent>) objects[2]);
        window.reSchedule();
    }

    @Override
    protected void init(Expression[] expressions, QueryPostProcessingElement queryPostProcessingElement, AbstractDefinition streamDefinition, String s, boolean b, SiddhiContext siddhiContext) {
        if (parameters[0] instanceof IntConstant) {
            timeToKeep = ((IntConstant) parameters[0]).getValue();
        } else {
            timeToKeep = ((LongConstant) parameters[0]).getValue();
        }

        if (this.siddhiContext.isDistributedProcessingEnabled()) {
            window = new SchedulerTimestampSiddhiQueueGrid<StreamEvent>(elementId, this, this.siddhiContext, this.async);
        } else {
            window = new SchedulerTimestampSiddhiQueue<StreamEvent>(this);
        }

    }

    @Override
    public void run() {
        acquireLock();
        try {
            while (true) {
                threadBarrier.pass();
                StreamEvent streamEvent = window.peek();
                try {
                    if (streamEvent == null) {
                        break;
                    }
                    long timeDiff = ((RemoveStream) streamEvent).getExpiryTime() - System.currentTimeMillis();
                    try {
                        if (timeDiff > 0) {

                            if (siddhiContext.isDistributedProcessingEnabled()) {
                                if (lastSchedule != null) {
                                    lastSchedule.cancel(false);
                                }
                                //should not use sleep as it will not release the lock, hence it will fail in distributed case
                                lastSchedule = eventRemoverScheduler.schedule(this, timeDiff, TimeUnit.MILLISECONDS);
                                break;

                            } else {
                                //this cannot be used for distributed case as it will course concurrency issues
                                releaseLock();
                                Thread.sleep(timeDiff);
                                acquireLock();
                            }
                        }
                        Collection<StreamEvent> resultList = window.poll(System.currentTimeMillis());
                        if (resultList != null) {
                            for (StreamEvent event : resultList) {
                                if (streamEvent instanceof AtomicEvent) {
                                    nextProcessor.process((AtomicEvent) event);
                                } else {
                                    nextProcessor.process((ListEvent) event);
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        log.warn("Time window sleep interrupted at elementId " + elementId);
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
        } finally {
            releaseLock();
        }
    }


    /**
     * need to implement
     * @param inEvents
     * @return
     */
    public InEvent[] curvePredictor(InEvent firstEvent){
        /**
         * to get the smoothed values
         */
        findEMA();

        /**
         * fit the curve and return the coefficients as events
         */
        CurveFitter curveFitter = new CurveFitter(timeStamps, smoothedValues);
        double[] coefficients = curveFitter.fit();


        InEvent[] inEvents = new InEvent[3];
        inEvents[0] = new InEvent(firstEvent.getStreamId(), firstEvent.getTimeStamp(), firstEvent.getD);
        return inEvents;
    }

    /**
     * need to implement find exponential moving average
     */
    private void findEMA(){
        timeStamps = new long[newEventList.size()];
        dataValues = new double[newEventList.size()];
        smoothedValues = new double[newEventList.size()];

        for (int i = 0; i < newEventList.size() ; i++){
            timeStamps[i] = newEventList.get(i).getTimeStamp();
            //Problem need to ask question
            //dataValues[i] = newEventList.get(i).
        }

        if(timeStamps.length > 2){
            smoothedValues[0] = 0.0D;
            smoothedValues[1] = dataValues[1];
           for(int i = 2 ; i < newEventList.size() ; i++){
               smoothedValues[i] = ALPHA * dataValues[i-1] + (1.0 - ALPHA) * smoothedValues[i-1];
           }
        }
    }

    @Override
    public void schedule() {
        if (lastSchedule != null) {
            lastSchedule.cancel(false);
        }
        lastSchedule= eventRemoverScheduler.schedule(this, timeToKeep, TimeUnit.MILLISECONDS);
    }

    @Override
    public void scheduleNow() {
        if (lastSchedule != null) {
            lastSchedule.cancel(false);
        } else {
            lastSchedule = null;
        }
        eventRemoverScheduler.execute(this);
    }

    @Override
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.eventRemoverScheduler = scheduledExecutorService;
    }

    public void scheduleConstantTime() {
        eventRemoverScheduler.schedule(this, constantSchedulingInterval, TimeUnit.MILLISECONDS);
    }
    @Override
    public void setThreadBarrier(ThreadBarrier threadBarrier) {
        this.threadBarrier = threadBarrier;
    }

    @Override
    public void destroy() {
        oldEventList = null;
        newEventList = null;
        window = null;
    }

}
