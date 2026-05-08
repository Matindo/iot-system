package io.ioteca.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.ingestion.dto.ProcessedMessage;
import io.ioteca.ingestion.dto.RawMessage;
import io.ioteca.ingestion.entity.SensorData;
import io.ioteca.ingestion.repository.DeviceRepository;
import io.ioteca.ingestion.repository.SensorDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock SensorDataRepository sensorDataRepository;
    @Mock DeviceRepository deviceRepository;
    @Mock ObjectMapper objectMapper;
    @InjectMocks IngestionService ingestionService;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
    }

    // ── validation ────────────────────────────────────────────────────────────

    @Test
    void ingest_throwsWhenDeviceIdIsNull() {
        RawMessage msg = new RawMessage(null, null, Map.of("temperature", 25.0), null, null);

        assertThatThrownBy(() -> ingestionService.ingest(projectId, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("device_id");
    }

    @Test
    void ingest_throwsWhenDeviceIdIsBlank() {
        RawMessage msg = new RawMessage("   ", null, Map.of("temperature", 25.0), null, null);

        assertThatThrownBy(() -> ingestionService.ingest(projectId, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("device_id");
    }

    @Test
    void ingest_throwsWhenMetricsIsNull() {
        RawMessage msg = new RawMessage("sensor_01", null, null, null, null);

        assertThatThrownBy(() -> ingestionService.ingest(projectId, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metrics");
    }

    @Test
    void ingest_throwsWhenMetricsIsEmpty() {
        RawMessage msg = new RawMessage("sensor_01", null, Map.of(), null, null);

        assertThatThrownBy(() -> ingestionService.ingest(projectId, msg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metrics");
    }

    // ── metric explosion (wide → narrow) ─────────────────────────────────────

    @Test
    void ingest_savesSeparateRowPerMetric() {
        RawMessage msg = new RawMessage("sensor_01", null,
                Map.of("temperature", 24.5, "humidity", 65.0, "battery_v", 3.7), null, null);
        when(deviceRepository.touchLastSeen(any(), any(), any())).thenReturn(1);

        ingestionService.ingest(projectId, msg);

        verify(sensorDataRepository, times(3)).insertRaw(any());
    }

    @Test
    void ingest_numericMetric_setsValueField() {
        RawMessage msg = new RawMessage("sensor_01", null, Map.of("temperature", 24.5), null, null);
        when(deviceRepository.touchLastSeen(any(), any(), any())).thenReturn(1);
        ArgumentCaptor<SensorData> captor = ArgumentCaptor.forClass(SensorData.class);

        ingestionService.ingest(projectId, msg);

        verify(sensorDataRepository).insertRaw(captor.capture());
        assertThat(captor.getValue().getValue()).isEqualTo(24.5);
        assertThat(captor.getValue().getValueStr()).isNull();
    }

    @Test
    void ingest_stringMetric_setsValueStrField() {
        RawMessage msg = new RawMessage("sensor_01", null, Map.of("status", "online"), null, null);
        when(deviceRepository.touchLastSeen(any(), any(), any())).thenReturn(1);
        ArgumentCaptor<SensorData> captor = ArgumentCaptor.forClass(SensorData.class);

        ingestionService.ingest(projectId, msg);

        verify(sensorDataRepository).insertRaw(captor.capture());
        assertThat(captor.getValue().getValueStr()).isEqualTo("online");
        assertThat(captor.getValue().getValue()).isNull();
    }

    // ── timestamp resolution ──────────────────────────────────────────────────

    @Test
    void ingest_nullTimestamp_usesServerTime() {
        RawMessage msg = new RawMessage("sensor_01", null, Map.of("temp", 25.0), null, null);
        when(deviceRepository.touchLastSeen(any(), any(), any())).thenReturn(1);
        ArgumentCaptor<SensorData> captor = ArgumentCaptor.forClass(SensorData.class);

        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        ingestionService.ingest(projectId, msg);
        OffsetDateTime after  = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(1);

        verify(sensorDataRepository).insertRaw(captor.capture());
        OffsetDateTime captured = captor.getValue().getTime();
        assertThat(captured).isAfter(before).isBefore(after);
    }

    @Test
    void ingest_epochMsTimestamp_convertedCorrectly() {
        long epochMs = 1_700_000_000_000L; // 2023-11-14T22:13:20Z
        RawMessage msg = new RawMessage("sensor_01", epochMs, Map.of("temp", 25.0), null, null);
        when(deviceRepository.touchLastSeen(any(), any(), any())).thenReturn(1);
        ArgumentCaptor<SensorData> captor = ArgumentCaptor.forClass(SensorData.class);

        ingestionService.ingest(projectId, msg);

        verify(sensorDataRepository).insertRaw(captor.capture());
        assertThat(captor.getValue().getTime().toInstant().toEpochMilli()).isEqualTo(epochMs);
    }

    // ── device auto-registration ──────────────────────────────────────────────

    @Test
    void ingest_existingDevice_updatesLastSeen() {
        RawMessage msg = new RawMessage("sensor_01", null, Map.of("temp", 25.0), null, null);
        when(deviceRepository.touchLastSeen(any(), eq("sensor_01"), any())).thenReturn(1);

        ingestionService.ingest(projectId, msg);

        verify(deviceRepository, never()).save(any());
    }

    @Test
    void ingest_newDevice_autoRegisters() {
        RawMessage msg = new RawMessage("brand_new_device", null, Map.of("temp", 25.0), null, null);
        when(deviceRepository.touchLastSeen(any(), eq("brand_new_device"), any())).thenReturn(0);
        when(deviceRepository.save(any())).thenReturn(null);

        ingestionService.ingest(projectId, msg);

        verify(deviceRepository).save(argThat(d ->
                "brand_new_device".equals(d.getDeviceId()) && d.getProjectId().equals(projectId)));
    }

    // ── returned ProcessedMessage ─────────────────────────────────────────────

    @Test
    void ingest_returnedMessageHasCorrectFields() {
        Map<String, Object> metrics = Map.of("temperature", 24.5);
        RawMessage msg = new RawMessage("sensor_01", null, metrics, null, null);
        when(deviceRepository.touchLastSeen(any(), any(), any())).thenReturn(1);

        ProcessedMessage result = ingestionService.ingest(projectId, msg);

        assertThat(result.projectId()).isEqualTo(projectId);
        assertThat(result.deviceId()).isEqualTo("sensor_01");
        assertThat(result.metrics()).isEqualTo(metrics);
        assertThat(result.time()).isNotNull();
        assertThat(result.ingestedAt()).isNotNull();
    }
}
