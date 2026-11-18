package helpdesk.services;

import helpdesk.core.AppConfig;
import helpdesk.models.Ticket;
import helpdesk.repositories.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Background worker thread that periodically inspects unresolved tickets
 * and escalates priority based on age (Task 09).
 * Rules:
 *  - Low -> Medium after 72h
 *  - Medium -> High after 48h
 *  - High -> Critical after 24h
 *  - Critical: no further escalation
 */
public class TicketEscalationService implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TicketEscalationService.class);
    private final TicketRepository ticketRepository;
    private final long intervalMillis;
    private volatile boolean running = true;

    public TicketEscalationService() {
        this(new TicketRepository(), AppConfig.getEscalationCheckIntervalMinutes() * 60 * 1000L);
    }

    public TicketEscalationService(TicketRepository repository, long intervalMillis) {
        this.ticketRepository = repository;
        this.intervalMillis = Math.max(intervalMillis, 5000L);
    }

    @Override
    public void run() {
        logger.info("TicketEscalationService started. Interval: {} ms", intervalMillis);
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                escalateOldTickets();
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                logger.info("TicketEscalationService thread interrupted, exiting.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in escalation cycle: {}", e.getMessage());
            }
        }
    }

    public void stop() {
        this.running = false;
    }

    public int escalateOldTickets() {
        List<Ticket> unresolved = ticketRepository.findUnresolved();
        Instant now = Instant.now();
        int count = 0;

        for (Ticket t : unresolved) {
            Timestamp createdAt = t.getCreatedAt();
            if (createdAt == null) continue;

            long hoursOld = Duration.between(createdAt.toInstant(), now).toHours();
            String curPri = t.getPriority();
            String newPri = null;

            if ("Low".equalsIgnoreCase(curPri) && hoursOld >= 72) {
                newPri = "Medium";
            } else if ("Medium".equalsIgnoreCase(curPri) && hoursOld >= 48) {
                newPri = "High";
            } else if ("High".equalsIgnoreCase(curPri) && hoursOld >= 24) {
                newPri = "Critical";
            }

            if (newPri != null) {
                logger.info("[ESCALATION] Ticket #{} ({}) escalated from {} to {} (age: {}h)",
                        t.getTicketId(), t.getTitle(), curPri, newPri, hoursOld);
                ticketRepository.updatePriority(t.getTicketId(), newPri);
                t.setPriority(newPri);
                count++;
            }
        }
        return count;
    }
}
