package io.ioteca.ingestion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Maps to tsdata.sensor_data â€” TimescaleDB hypertable.
 * One row per metric value (wide payloads are exploded into multiple rows).
 */
@Entity
@Table(schema = "tsdata", name = "sensor_data")
@IdClass(SensorDataId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SensorData {

    @Id
    @Column(nullable = false)
    private OffsetDateTime time;

    @Id
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Id
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Id
    @Column(nullable = false)
    private String metric;

    @Column(name = "value")
    private Double value;

    @Column(name = "value_str")
    private String valueStr;

    // JSONB stored as string to avoid extra dependencies
    @Column(columnDefinition = "jsonb")
    private String tags;

    @Column(name = "ingested_at", nullable = false)
    @Builder.Default
    private OffsetDateTime ingestedAt = OffsetDateTime.now();
}
