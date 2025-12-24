package org.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class RedisMap<K, V> implements Map<K, V> {
    private static final String KEY_IS_NULL_STRING = "Key is null";

    private final JedisPool pool;
    private final String redisKey;
    private final ObjectMapper mapper;
    private final Class<K> keyClass;
    private final Class<V> valueClass;

    public RedisMap(JedisPool pool, String redisKey, Class<K> keyClass, Class<V> valueClass) {
        this.pool = pool;
        this.redisKey = redisKey;
        this.keyClass = keyClass;
        this.valueClass = valueClass;
        this.mapper = new ObjectMapper();
    }

    private String serialize(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new IllegalStateException("JSON serialization failed: " + value, e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new IllegalStateException("JSON deserialization failed: " + json, e);
        }
    }

    @Override
    public int size() {
        try (Jedis jedis = pool.getResource()) {
            return Math.toIntExact(jedis.hlen(redisKey));
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        Objects.requireNonNull(key, KEY_IS_NULL_STRING);

        try (Jedis jedis = pool.getResource()) {
            return jedis.hexists(redisKey, serialize(key));
        }
    }

    @Override
    public boolean containsValue(Object value) {
        if (!valueClass.isInstance(value)) {
            return false;
        }

        try (Jedis jedis = pool.getResource()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams params = new ScanParams().count(100);

            do {
                ScanResult<Map.Entry<String, String>> scanResult = jedis.hscan(redisKey, cursor, params);

                for (Map.Entry<String, String> entry : scanResult.getResult()) {
                    V val = deserialize(entry.getValue(), valueClass);
                    if (val.equals(value)) {
                        return true;
                    }
                }

                cursor = scanResult.getCursor();
            } while (!cursor.equals(ScanParams.SCAN_POINTER_START));
        }

        return false;
    }

    @Override
    public V get(Object key) {
        Objects.requireNonNull(key, KEY_IS_NULL_STRING);

        try (Jedis jedis = pool.getResource()) {
            String json = jedis.hget(redisKey, serialize(key));
            return json != null ? deserialize(json, valueClass) : null;
        }
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, KEY_IS_NULL_STRING);
        Objects.requireNonNull(value, "Value is null");

        V old = get(key);
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(redisKey, serialize(key), serialize(value));
        }
        return old;
    }

    @Override
    public V remove(Object key) {
        Objects.requireNonNull(key, KEY_IS_NULL_STRING);

        V old = get(key);
        try (Jedis jedis = pool.getResource()) {
            jedis.hdel(redisKey, serialize(key));
        }
        return old;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        Objects.requireNonNull(m, "Map is null");

        Map<String, String> map = m.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> serialize(e.getKey()),
                        e -> serialize(e.getValue())
                ));

        try (Jedis jedis = pool.getResource()) {
            jedis.hset(redisKey, map);
        }
    }

    @Override
    public void clear() {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(redisKey);
        }
    }

    @Override
    public Set<K> keySet() {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hkeys(redisKey)
                    .stream()
                    .map(k -> deserialize(k, keyClass))
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public Collection<V> values() {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hvals(redisKey)
                    .stream()
                    .map(v -> deserialize(v, valueClass))
                    .toList();
        }
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hgetAll(redisKey).entrySet()
                    .stream()
                    .map(e -> new AbstractMap.SimpleEntry<>(
                            deserialize(e.getKey(), keyClass),
                            deserialize(e.getValue(), valueClass)))
                    .collect(Collectors.toSet());
        }
    }
}

