package com.food.order_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String aggregateId;
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private String status; // "PENDING" or "PROCESSED"
    private Date createdAt;

    @Column(columnDefinition = "TEXT")
    private String traceParent; // Stores the W3C trace context

    public OutboxEvent(String aggregateId, String topic, String payload, String traceParent) {
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.payload = payload;
        this.traceParent = traceParent;
        this.status = "PENDING";
        this.createdAt = new Date();
    }
}