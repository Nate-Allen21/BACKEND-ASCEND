package br.com.ascend.the_system.service;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class RecoveryDeliveryWorker {
    private static final Logger LOG = LoggerFactory.getLogger(RecoveryDeliveryWorker.class);
    private final RecoveryStore store;
    private final RecoveryMailer mailer;

    RecoveryDeliveryWorker(RecoveryStore store, RecoveryMailer mailer) {
        this.store = store;
        this.mailer = mailer;
    }

    @Scheduled(fixedDelay = 2000, initialDelayString = "${ascend.recovery.worker-initial-delay-ms:2000}")
    public void dispatch() {
        if (!mailer.configured()) return;
        for (long id : store.due()) {
            store.claim(id).ifPresent(this::deliver);
            if (Thread.currentThread().isInterrupted()) return;
        }
    }

    private void deliver(RecoveryStore.Delivery delivery) {
        boolean sent = false;
        try {
            sent = mailer.send(delivery);
        } catch (IOException ignored) {
            // The next scheduled attempt generates a fresh token.
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        if (!sent) LOG.warn("Falha no envio de recuperação de senha.");
        store.finish(delivery, sent);
    }

    @Scheduled(fixedDelay = 900000, initialDelay = 60000)
    public void prune() {
        if (mailer.configured()) store.prune();
    }
}
