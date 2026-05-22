package com.spamascotas.spa_mascotas_api.controller;

import com.spamascotas.spa_mascotas_api.service.sse.StockEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class StockSseController {

    private final StockEventService stockEventService;

    @GetMapping(value = "/stock", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stockEvents() {
        return stockEventService.subscribe();
    }
}
