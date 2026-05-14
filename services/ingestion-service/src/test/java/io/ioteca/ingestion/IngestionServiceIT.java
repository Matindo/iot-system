package io.ioteca.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ioteca.ingestion.entity.Device;
import io.ioteca.ingestion.repository.DeviceRepository;
import io.ioteca.ingestion.repository.SensorDataRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IngestionServiceIT {

    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired ObjectMapper objectMapper;
    @Autowired DeviceRepository deviceRepository;
    @Autowired SensorDataRepository sensorDataRepository;
    @Autowired ProcessedCapture processedCapture;
    @Autowired KafkaListenerEndpointRegistry endpointRegistry;

    private static final String RAW_TOPIC = "raw.ingest.af-ke-1";
    private UUID projectId;

    @TestConfiguration
    static class TestConfig {
        @Bean
        ProcessedCapture processedCapture() { return new ProcessedCapture(); }
    }

    static class ProcessedCapture {
        final LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

        @KafkaListener(topics = "processed.ingest.af-ke-1", groupId = "ingestion-it-capture")
        void capture(String msg) { messages.offer(msg); }
    }

    @BeforeAll
    void waitForKafkaConsumers() {
        // Block until every listener container has at least one assigned partition.
        // This prevents NOT_COORDINATOR / rebalance races from causing false timeouts.
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> endpointRegistry.getListenerContainers().stream()
                        .filter(c -> c instanceof ConcurrentMessageListenerContainer)
                        .map(c -> (ConcurrentMessageListenerContainer<?, ?>) c)
                        .allMatch(c -> !c.getAssignedPartitions().isEmpty()));
    }

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        processedCapture.messages.clear();
    }

    @Test
    void rawMessage_isProcessed_deviceRegistered_sensorDataWritten() throws Exception {
        var raw = Map.of(
                "device_id", "sensor-01",
                "project_id", projectId.toString(),
                "metrics", Map.of("temperature", 22.5, "humidity", 60.0)
        );
        kafkaTemplate.send(RAW_TOPIC, projectId.toString(), objectMapper.writeValueAsString(raw));

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(deviceRepository.findByProjectIdAndDeviceId(projectId, "sensor-01")).isPresent()
        );

        List<Device> allDevices = deviceRepository.findAll();
        assertThat(allDevices).anyMatch(d ->
                projectId.equals(d.getProjectId()) && "sensor-01".equals(d.getDeviceId()));

        long sensorRows = sensorDataRepository.findAll().stream()
                .filter(sd -> projectId.equals(sd.getProjectId()))
                .count();
        assertThat(sensorRows).isGreaterThanOrEqualTo(2);

        Awaitility.await().atMost(Duration.ofSeconds(15)).until(() ->
                processedCapture.messages.stream().anyMatch(m -> m.contains(projectId.toString()))
        );
    }

    @Test
    void rawMessage_withoutProjectId_isDropped() throws Exception {
        var raw = Map.of(
                "device_id", "orphan-device",
                "metrics", Map.of("pressure", 101.3)
        );
        kafkaTemplate.send(RAW_TOPIC, "not-a-uuid", objectMapper.writeValueAsString(raw));

        Thread.sleep(5_000);

        assertThat(deviceRepository.findAll()).noneMatch(d -> "orphan-device".equals(d.getDeviceId()));
        assertThat(sensorDataRepository.findAll()).noneMatch(sd -> "orphan-device".equals(sd.getDeviceId()));
    }

    @Test
    void sameDevice_secondMessage_doesNotDuplicateDeviceRecord() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "device_id", "temp-sensor",
                "project_id", projectId.toString(),
                "metrics", Map.of("temperature", 20.0)
        ));

        kafkaTemplate.send(RAW_TOPIC, projectId.toString(), payload);
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(deviceRepository.findByProjectIdAndDeviceId(projectId, "temp-sensor")).isPresent()
        );

        kafkaTemplate.send(RAW_TOPIC, projectId.toString(), payload);
        Thread.sleep(3_000);

        long count = deviceRepository.findAll().stream()
                .filter(d -> projectId.equals(d.getProjectId()) && "temp-sensor".equals(d.getDeviceId()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void multipleMetrics_producedAsIndividualRows() throws Exception {
        var raw = Map.of(
                "device_id", "multi-sensor",
                "project_id", projectId.toString(),
                "metrics", Map.of("co2", 450.0, "voc", 120.0, "pm25", 15.0)
        );
        kafkaTemplate.send(RAW_TOPIC, projectId.toString(), objectMapper.writeValueAsString(raw));

        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            long rows = sensorDataRepository.findAll().stream()
                    .filter(sd -> projectId.equals(sd.getProjectId()))
                    .count();
            assertThat(rows).isGreaterThanOrEqualTo(3);
        });
    }
}
