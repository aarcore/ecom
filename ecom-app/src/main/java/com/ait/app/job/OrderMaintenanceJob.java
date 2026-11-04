package com.ait.app.job;

import com.ait.mall.core.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Housekeeping that keeps order states honest without manual intervention.
 *
 * <ul>
 *   <li>every 5 minutes: cancel unpaid orders past the 45 minute deadline and
 *       return their stock</li>
 *   <li>every hour: auto-confirm receipt 7 days after shipment</li>
 * </ul>
 *
 * <p>Both are idempotent, so running multiple application instances behind a
 * load balancer is safe (worst case one instance logs a no-op pass).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMaintenanceJob {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${ecom.jobs.close-unpaid-ms:300000}", initialDelay = 60000)
    public void closeUnpaidOrders() {
        try {
            int closed = orderService.closeExpiredOrders();
            if (closed > 0) {
                log.info("[job] closed {} expired unpaid orders", closed);
            }
        } catch (Exception ex) {
            log.error("[job] closeUnpaidOrders failed", ex);
        }
    }

    @Scheduled(fixedDelayString = "${ecom.jobs.auto-complete-ms:3600000}", initialDelay = 300000)
    public void autoCompleteShipped() {
        try {
            int completed = orderService.autoCompleteShipped();
            if (completed > 0) {
                log.info("[job] auto completed {} shipped orders", completed);
            }
        } catch (Exception ex) {
            log.error("[job] autoCompleteShipped failed", ex);
        }
    }
}
