package org.apache.stratos.cep.extension;

import org.wso2.siddhi.core.persistence.ThreadBarrier;
import org.wso2.siddhi.core.query.processor.window.RunnableWindowProcessor;
import org.wso2.siddhi.core.query.processor.window.WindowProcessor;

import java.util.concurrent.ScheduledExecutorService;

public class CurveFinderWindowProcessor extends WindowProcessor implements RunnableWindowProcessor{
    @Override
    public void run() {

    }

    @Override
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {

    }

    @Override
    public void setThreadBarrier(ThreadBarrier threadBarrier) {

    }
}
