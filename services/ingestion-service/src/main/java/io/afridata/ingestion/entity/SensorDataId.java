package io.afridata.ingestion.entity;

import lombok.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class SensorDataId implements Serializable {
    private OffsetDateTime time;
    private UUID projectId;
    private String deviceId;
    private String metric;
}
