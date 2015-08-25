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
import org.wso2.siddhi.core.event.StreamEvent;
import org.wso2.siddhi.core.event.in.InEvent;
import org.wso2.siddhi.core.event.in.InListEvent;
import org.wso2.siddhi.core.event.remove.RemoveEvent;
import org.wso2.siddhi.core.event.remove.RemoveListEvent;
import org.wso2.siddhi.core.query.QueryPostProcessingElement;
import org.wso2.siddhi.core.query.processor.window.RunnableWindowProcessor;
import org.wso2.siddhi.core.query.processor.window.WindowProcessor;
import org.wso2.siddhi.core.snapshot.ThreadBarrier;
import org.wso2.siddhi.core.util.collection.queue.scheduler.ISchedulerSiddhiQueue;
import org.wso2.siddhi.core.util.collection.queue.scheduler.SchedulerSiddhiQueue;
import org.wso2.siddhi.core.util.collection.queue.scheduler.SchedulerSiddhiQueueGrid;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.expression.Expression;
import org.wso2.siddhi.query.api.expression.Variable;
import org.wso2.siddhi.query.api.expression.constant.IntConstant;
import org.wso2.siddhi.query.api.expression.constant.LongConstant;
import org.wso2.siddhi.query.api.extension.annotation.SiddhiExtension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

;

/**
 * your task is to finish this Sliding WIndow, Use Gradient and Second Derivative Window processors for example
 * CurveFitter Finished
 * EMA added
 * You have to implement this processor
 */
@SiddhiExtension(namespace = "stratos", function = "curveFitting")
public class CurveFinderWindowProcessor extends WindowProcessor implements RunnableWindowProcessor {

    /**
     * alpha is used as the smoothing constant in exponential moving average
     */
    public static final double ALPHA = 0.8000;
    static final Logger log = Logger.getLogger(CurveFinderWindowProcessor.class);
    private ScheduledExecutorService eventRemoverScheduler;
    private long timeToKeep;
    private ScheduledFuture<?> lastSchedule = null;
    private long constantSchedulingInterval = -1;
    private ThreadBarrier threadBarrier;
    private int subjectAttrIndex;
    private Attribute.Type subjectAttrType;
    private int coefficientAAttrIndex;
    private Attribute.Type coefficientAAttrType;
    private int coefficientBAttrIndex;
    private Attribute.Type coefficientBAttrType;
    private int coefficientCAttrIndex;
    private Attribute.Type coefficientCAttrType;
    private List<InEvent> newEventList;
    private List<RemoveEvent> oldEventList;
    private ISchedulerSiddhiQueue<StreamEvent> window;
    private long[] timeStamps;
    private double[] dataValues;
    private double[] smoothedValues;
    private int count = 0;


    @Override
    protected void processEvent(InEvent event) {
        acquireLock();
        try {
            newEventList.add(event);
        } finally {
            releaseLock();
        }
    }

    @Override
    protected void processEvent(InListEvent listEvent) {
        acquireLock();
        try {
            System.out.println(listEvent);
            for (int i = 0, size = listEvent.getActiveEvents(); i < size; i++) {
                newEventList.add((InEvent) listEvent.getEvent(i));
            }
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
    public void run() {
        acquireLock();
        try {
            count++;
            long scheduledTime = System.currentTimeMillis();
            try {
                oldEventList.clear();
                while (true) {
                    threadBarrier.pass();
                    RemoveEvent removeEvent = (RemoveEvent) window.poll();


                    if (removeEvent == null) {
                        if (oldEventList.size() > 0) {
                            nextProcessor.process(new RemoveListEvent(
                                    oldEventList.toArray(new RemoveEvent[oldEventList.size()])));
                            oldEventList.clear();
                        }

                        if (newEventList.size() > 0) {
                            InEvent[] inEvents =
                                    newEventList.toArray(new InEvent[newEventList.size()]);
                            for (InEvent inEvent : inEvents) {
                                window.put(new RemoveEvent(inEvent, -1));
                            }

                            InEvent[] curveFittingEvents = curvePredictor();

                            for (InEvent inEvent : curveFittingEvents) {
                                window.put(new RemoveEvent(inEvent, -1));
                            }
                            nextProcessor.process(new InListEvent(curveFittingEvents));

                            newEventList.clear();
                        }

                        long diff = timeToKeep - (System.currentTimeMillis() - scheduledTime);
                        if (diff > 0) {
                            try {
                                if (lastSchedule != null) {
                                    lastSchedule.cancel(false);
                                }
                                lastSchedule = eventRemoverScheduler.schedule(this, diff, TimeUnit.MILLISECONDS);
                            } catch (RejectedExecutionException ex) {
                                log.warn("scheduling cannot be accepted for execution: elementID " +
                                        elementId);
                            }
                            break;
                        }
                        scheduledTime = System.currentTimeMillis();
                    } else {
                        oldEventList.add(new RemoveEvent(removeEvent, System.currentTimeMillis()));
                    }
                }
            } catch (Throwable t) {
                log.error(t.getMessage(), t);
            }
        } finally {
            releaseLock();
        }
    }


    /**
     * Curve predictor method
     *
     * @return event which contains the curve fitting coefficient
     */
    public InEvent[] curvePredictor() {
        /**
         * to get the smoothed values
         */
        findEMA();

        /**
         * fit the curve and return the coefficients as events
         */
        CurveFitter curveFitter = new CurveFitter(timeStamps, smoothedValues);
        Double[] coefficients = curveFitter.fit();

        log.info("a : " + coefficients[0] + " b : " + coefficients[1] + " c : " + coefficients[2]);

        Object[] data = newEventList.get(0).getData().clone();

        data[coefficientAAttrIndex] = coefficients[0];
        data[coefficientBAttrIndex] = coefficients[1];
        data[coefficientCAttrIndex] = coefficients[2];

        InEvent[] inEvents = new InEvent[1];
        inEvents[0] = new InEvent(newEventList.get(0).getStreamId(), newEventList.get(0).getTimeStamp(), data);
        return inEvents;
    }

    /**
     * need to implement find exponential moving average
     */
    private void findEMA() {
        Attribute.Type attrType = subjectAttrType;

        timeStamps = new long[newEventList.size()];
        dataValues = new double[newEventList.size()];
        smoothedValues = new double[newEventList.size()];

        int indexOfEvent = 0;
        for (indexOfEvent = 0; indexOfEvent < newEventList.size(); indexOfEvent++) {
            InEvent eventToPredict = newEventList.get(indexOfEvent);
            timeStamps[indexOfEvent] = eventToPredict.getTimeStamp();

            if (Attribute.Type.DOUBLE.equals(attrType)) {
                dataValues[indexOfEvent] = (Double) eventToPredict.getData()[subjectAttrIndex];
            } else if (Attribute.Type.INT.equals(attrType)) {
                dataValues[indexOfEvent] = (Integer) eventToPredict.getData()[subjectAttrIndex];
            } else if (Attribute.Type.LONG.equals(attrType)) {
                dataValues[indexOfEvent] = (Long) eventToPredict.getData()[subjectAttrIndex];
            } else if (Attribute.Type.FLOAT.equals(attrType)) {
                dataValues[indexOfEvent] = (Float) eventToPredict.getData()[subjectAttrIndex];
            }

        }

        if (timeStamps.length > 2) {
            for (int i = 0; i < timeStamps.length; i++) {
                smoothedValues[i] = dataValues[i];
            }
        }
    }

    @Override
    protected Object[] currentState() {
        return new Object[]{window.currentState(), oldEventList, newEventList};
    }

    @Override
    protected void restoreState(Object[] data) {
        window.restoreState(data);
        window.restoreState((Object[]) data[0]);
        oldEventList = ((ArrayList<RemoveEvent>) data[1]);
        newEventList = ((ArrayList<InEvent>) data[2]);
        window.reSchedule();
    }

    @Override
    protected void init(Expression[] parameters, QueryPostProcessingElement nextProcessor, AbstractDefinition streamDefinition, String elementId, boolean async, SiddhiContext siddhiContext) {
        if (parameters[0] instanceof IntConstant) {
            timeToKeep = ((IntConstant) parameters[0]).getValue();
        } else {
            timeToKeep = ((LongConstant) parameters[0]).getValue();
        }


        String subjectedAttr = ((Variable) parameters[1]).getAttributeName();
        subjectAttrIndex = streamDefinition.getAttributePosition(subjectedAttr);
        subjectAttrType = streamDefinition.getAttributeType(subjectedAttr);

        subjectedAttr = ((Variable) parameters[2]).getAttributeName();
        coefficientAAttrIndex = streamDefinition.getAttributePosition(subjectedAttr);
        coefficientAAttrType = streamDefinition.getAttributeType(subjectedAttr);

        subjectedAttr = ((Variable) parameters[3]).getAttributeName();
        coefficientBAttrIndex = streamDefinition.getAttributePosition(subjectedAttr);
        coefficientBAttrType = streamDefinition.getAttributeType(subjectedAttr);

        subjectedAttr = ((Variable) parameters[4]).getAttributeName();
        coefficientCAttrIndex = streamDefinition.getAttributePosition(subjectedAttr);
        coefficientCAttrType = streamDefinition.getAttributeType(subjectedAttr);

        oldEventList = new ArrayList<RemoveEvent>();
        if (this.siddhiContext.isDistributedProcessingEnabled()) {
            newEventList = this.siddhiContext.getHazelcastInstance().getList(elementId + "-newEventList");
        } else {
            newEventList = new ArrayList<InEvent>();
        }

        if (this.siddhiContext.isDistributedProcessingEnabled()) {
            window = new SchedulerSiddhiQueueGrid<StreamEvent>(elementId, this, this.siddhiContext, this.async);
        } else {
            window = new SchedulerSiddhiQueue<StreamEvent>(this);
        }
        //Ordinary scheduling
        window.schedule();

    }

    @Override
    public void schedule() {
        if (lastSchedule != null) {
            lastSchedule.cancel(false);
        }
        lastSchedule = eventRemoverScheduler.schedule(this, timeToKeep, TimeUnit.MILLISECONDS);
    }

    public void scheduleNow() {
        if (lastSchedule != null) {
            lastSchedule.cancel(false);
        }
        lastSchedule = eventRemoverScheduler.schedule(this, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.eventRemoverScheduler = scheduledExecutorService;
    }

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