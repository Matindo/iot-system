package io.ioteca.ingestion.repository;

import io.ioteca.ingestion.entity.SensorData;
import io.ioteca.ingestion.entity.SensorDataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SensorDataRepository extends JpaRepository<SensorData, SensorDataId> {

    @Modifying
    @Query(value = """
            INSERT INTO tsdata.sensor_data
              (time, project_id, device_id, metric, value, value_str, tags, ingested_at)
            VALUES
              (:#{#sd.time}, :#{#sd.projectId}, :#{#sd.deviceId}, :#{#sd.metric},
               :#{#sd.value}, :#{#sd.valueStr}, CAST(:#{#sd.tags} AS jsonb), :#{#sd.ingestedAt})
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void insertRaw(SensorData sd);

    @Modifying
    @Query(value = """
            INSERT INTO tsdata.sensor_data
              (time, project_id, device_id, metric, value, value_str, tags, ingested_at)
            SELECT r.time, r.project_id, r.device_id, r.metric,
                   r.value, r.value_str, CAST(r.tags AS jsonb), r.ingested_at
            FROM unnest(:rows) r
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void batchInsert(List<SensorData> rows);
}
