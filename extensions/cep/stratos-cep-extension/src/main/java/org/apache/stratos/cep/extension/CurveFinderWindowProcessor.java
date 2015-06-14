package org.apache.stratos.cep.extension;

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.core.event.StreamEvent;
import org.wso2.siddhi.core.event.in.InEvent;
import org.wso2.siddhi.core.event.in.InListEvent;
import org.wso2.siddhi.core.event.remove.RemoveEvent;
import org.wso2.siddhi.core.event.remove.RemoveListEvent;
import org.wso2.siddhi.core.persistence.ThreadBarrier;
import org.wso2.siddhi.core.query.QueryPostProcessingElement;
import org.wso2.siddhi.core.query.processor.window.RunnableWindowProcessor;
import org.wso2.siddhi.core.query.processor.window.WindowProcessor;
import org.wso2.siddhi.core.util.collection.queue.scheduler.ISchedulerSiddhiQueue;
import org.wso2.siddhi.core.util.collection.queue.scheduler.SchedulerSiddhiQueue;
import org.wso2.siddhi.core.util.collection.queue.scheduler.SchedulerSiddhiQueueGrid;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.definition.Attribute.Type;
import org.wso2.siddhi.query.api.expression.Expression;
import org.wso2.siddhi.query.api.expression.Variable;
import org.wso2.siddhi.query.api.expression.constant.IntConstant;
import org.wso2.siddhi.query.api.expression.constant.LongConstant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CurveFinderWindowProcessor extends WindowProcessor implements RunnableWindowProcessor{

    static final Logger log = Logger.getLogger(CurveFinderWindowProcessor.class);
    /**
     * alpha is used as the smoothing constant in exponential moving average
     */
    public static final double ALPHA = 0.8000;
    private ScheduledExecutorService eventRemoverScheduler;
    private long timeToKeep;
    private int subjectedAttrIndex;
    private Attribute.Type subjectedAttrType;
    private List<InEvent> newEventList;
    private List<RemoveEvent> oldEventList;
    private ThreadBarrier threadBarrier;
    private ISchedulerSiddhiQueue<StreamEvent> window;
    private double[] timeStampValues;
    private double[] dataValues;


    @Override
    protected void processEvent(InEvent inEvent) {
        acquireLock();
        try {
            newEventList.add(inEvent);
        } finally {
            releaseLock();
        }
    }

    @Override
    protected void processEvent(InListEvent inListEvent) {
        acquireLock();
        try {
            System.out.println(inListEvent);
            for (int i = 0, size = inListEvent.getActiveEvents(); i < size; i++) {
                newEventList.add((InEvent) inListEvent.getEvent(i));
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

        String subjectedAttr = ((Variable)parameters[1]).getAttributeName();
        subjectedAttrIndex = streamDefinition.getAttributePosition(subjectedAttr);
        subjectedAttrType = streamDefinition.getAttributeType(subjectedAttr);

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
    public void run() {
        acquireLock();
        try {
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

                            // in order to find second order polynomial, we need at least 3 events.
                            if (newEventList.size() > 2) {

                            } else {
                                log.debug("Insufficient events to calculate second derivative. We need at least 3 events. Current event count: " +
                                        newEventList.size());
                            }

                            newEventList.clear();
                        }

                        long diff = timeToKeep - (System.currentTimeMillis() - scheduledTime);
                        if (diff > 0) {
                            try {
                                eventRemoverScheduler.schedule(this, diff, TimeUnit.MILLISECONDS);
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
     * need to implement
     * @param inEvents
     * @return
     */
    public InEvent[] curvePredictor(InEvent[] inEvents){
        return null;
    }

    /**
     * need to implement find exponential moving average
     */
    public void findEMA(){

    }

    @Override
    public void schedule() {
        eventRemoverScheduler.schedule(this, timeToKeep, TimeUnit.MILLISECONDS);
    }

    @Override
    public void scheduleNow() {
        eventRemoverScheduler.schedule(this, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.eventRemoverScheduler = scheduledExecutorService;
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
