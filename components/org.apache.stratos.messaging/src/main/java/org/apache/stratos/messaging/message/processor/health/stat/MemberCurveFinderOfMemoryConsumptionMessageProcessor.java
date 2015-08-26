package org.apache.stratos.messaging.message.processor.health.stat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.health.stat.MemberCurveFinderOfMemoryConsumptionEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * Created by pranavan on 8/9/15.
 */
public class MemberCurveFinderOfMemoryConsumptionMessageProcessor extends MessageProcessor{
    private static  final Log log = LogFactory.getLog(MemberCurveFinderOfMemoryConsumptionMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        if(MemberCurveFinderOfMemoryConsumptionEvent.class.getName().equals(type)){

            MemberCurveFinderOfMemoryConsumptionEvent event = (MemberCurveFinderOfMemoryConsumptionEvent) MessagingUtil.jsonToObject(message, MemberCurveFinderOfMemoryConsumptionEvent.class);
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
