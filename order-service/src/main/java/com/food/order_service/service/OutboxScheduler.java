package com.food.order_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.order_service.dto.OrderEventDto;
import com.food.order_service.entity.OutboxEvent;
import com.food.order_service.repository.OutboxEventRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxScheduler {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OpenTelemetry openTelemetry;

    private final Tracer tracer;

    @Autowired
    public OutboxScheduler(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("outbox-scheduler");
    }

    @Scheduled(fixedDelay = 2000)
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByStatus("PENDING");

        for (OutboxEvent event : events) {
            // Restore the trace context from the database so this background task links to the original request
            Context parentContext = Context.root();
            if (event.getTraceParent() != null && !event.getTraceParent().isEmpty()) {
                parentContext = openTelemetry.getPropagators().getTextMapPropagator()
                        .extract(Context.root(), event.getTraceParent(), new TextMapGetter<String>() {
                            @Override
                            public Iterable<String> keys(String s) { return List.of("traceparent"); }
                            @Override
                            public String get(String carrier, String key) {
                                if ("traceparent".equals(key)) return carrier;
                                return null;
                            }
                        });
            }

            try (Scope scope = parentContext.makeCurrent()) {
                // Explicitly start a span to force linkage in Jaeger
                Span span = tracer.spanBuilder("outbox-event-publish")
                        .setParent(parentContext)
                        .setSpanKind(SpanKind.PRODUCER)
                        .setAttribute("messaging.destination.name", event.getTopic())
                        .startSpan();

                try (Scope childScope = span.makeCurrent()) {
                    // Convert back to the TRUE DTO class
                    OrderEventDto targetPayload = objectMapper.readValue(event.getPayload(), OrderEventDto.class);
                    kafkaTemplate.send(event.getTopic(), event.getAggregateId(), targetPayload);

                    event.setStatus("PROCESSED");
                    outboxEventRepository.save(event);
                } finally {
                    span.end();
                }
            } catch (Exception e) {
                System.err.println("Failed to parse and publish outbox event: " + event.getId() + " - " + e.getMessage());
            }
        }
    }
}
