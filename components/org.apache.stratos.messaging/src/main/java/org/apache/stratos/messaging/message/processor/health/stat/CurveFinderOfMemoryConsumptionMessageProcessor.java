package org.apache.stratos.messaging.message.processor.health.stat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.health.stat.CurveFinderOfMemoryConsumptionEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * Created by pranavan on 8/9/15.
 */
public class CurveFinderOfMemoryConsumptionMessageProcessor extends MessageProcessor {
    private static final Log log = LogFactory.getLog(CurveFinderOfLoadAverageMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        if (CurveFinderOfMemoryConsumptionEvent.class.getName().equals(type)) {

            // Parse complete message and build event
            CurveFinderOfMemoryConsumptionEvent event = (CurveFinderOfMemoryConsumptionEvent) MessagingUtil.jsonToObject
                    (message, CurveFinderOfMemoryConsumptionEvent.class);

            // Notify event listeners
            notifyEventListeners(event);

            if (log.isDebugEnabled()) {
                log.debug(String.format("%s event processor notified listeners ... ", type));
            }
            return true;
        } else {
            if (nextProcessor != null) {
                return nextProcessor.process(type, message, object);
            } else {
                throw new RuntimeException(String.format("Failed to process health stat message using available message processors: [type] %s [body] %s", type, message));
            }
        }
    }
}
