package com.spamascotas.spa_mascotas_api.service.sse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class StockEventService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void notifyStockChange()    { scheduleOrSend("stock-update");   }
    public void notifyCitaChange()     { scheduleOrSend("cita-update");    }
    public void notifyPedidoChange()   { scheduleOrSend("pedido-update");  }
    public void notifyInsumoChange()   { scheduleOrSend("insumo-update");  }
    public void notifyGroomingChange() { scheduleOrSend("grooming-update");}

    private void scheduleOrSend(String eventName) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(eventName);
                }
            });
        } else {
            send(eventName);
        }
    }

    private void send(String eventName) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data("refresh"));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}
