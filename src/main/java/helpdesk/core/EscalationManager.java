package helpdesk.core;

import helpdesk.services.TicketEscalationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the background ticket escalation daemon thread (Task 09).
 */
public class EscalationManager {

    private static final Logger logger = LoggerFactory.getLogger(EscalationManager.class);
    private static Thread escalationThread;
    private static TicketEscalationService service;

    public static synchronized void start() {
        if (escalationThread != null && escalationThread.isAlive()) {
            return;
        }
        service = new TicketEscalationService();
        escalationThread = new Thread(service, "TicketEscalationWorker");
        escalationThread.setDaemon(true);
        escalationThread.start();
        logger.info("EscalationManager background worker thread launched.");
    }

    public static synchronized void stop() {
        if (service != null) {
            service.stop();
        }
        if (escalationThread != null && escalationThread.isAlive()) {
            escalationThread.interrupt();
            try {
                escalationThread.join(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            logger.info("EscalationManager stopped successfully.");
        }
    }
}
