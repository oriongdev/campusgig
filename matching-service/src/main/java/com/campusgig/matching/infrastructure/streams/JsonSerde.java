package com.campusgig.matching.infrastructure.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

/**
 * Minimal JSON serde so Kafka Streams can read the events the other services publish.
 * Subclasses just fix the event type and keep a no-argument constructor, which is what the
 * Spring Cloud Stream Kafka Streams binder needs to instantiate them from configuration.
 */
public abstract class JsonSerde<T> implements Serde<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Class<T> type;

    protected JsonSerde(Class<T> type) {
        this.type = type;
    }

    @Override
    public Serializer<T> serializer() {
        return (topic, data) -> {
            if (data == null) {
                return null;
            }
            try {
                return MAPPER.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new IllegalStateException("Could not serialize " + type.getSimpleName(), e);
            }
        };
    }

    @Override
    public Deserializer<T> deserializer() {
        return (topic, bytes) -> {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            try {
                return MAPPER.readValue(new String(bytes, StandardCharsets.UTF_8), type);
            } catch (Exception e) {
                throw new IllegalStateException("Could not deserialize " + type.getSimpleName(), e);
            }
        };
    }
}
