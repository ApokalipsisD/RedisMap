package org.redis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redis.model.Student;
import redis.clients.jedis.JedisPool;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisMapTest {

    private static JedisPool jedisPool;
    private RedisMap<String, Student> map;

    @BeforeAll
    static void beforeAll() {
        jedisPool = new JedisPool("localhost", 6379);
    }

    @BeforeEach
    void setUp() {
        map = new RedisMap<>(
                jedisPool,
                "test:students",
                String.class,
                Student.class
        );

        map.clear();
    }

    @Test
    void testPutAndGet() {
        Student s = new Student("A", 22, List.of("Java", "Redis"));
        map.put("user1", s);

        Student actual = map.get("user1");

        assertNotNull(actual);
        assertEquals("A", actual.getName());
    }

    @Test
    void testSize() {
        map.put("u1", new Student("A", 15, List.of()));
        map.put("u2", new Student("B", 25, List.of()));

        assertEquals(2, map.size());
    }

    @Test
    void testContainsKey() {
        map.put("exists", new Student("A", 1, List.of()));

        assertTrue(map.containsKey("exists"));
        assertFalse(map.containsKey("none"));
    }

    @Test
    void testContainsValue() {
        Student s1 = new Student("A", 10, List.of("A"));
        Student s2 = new Student("B", 20, List.of("B"));

        map.put("1", s1);
        map.put("2", s2);

        assertTrue(map.containsValue(s1));
        assertFalse(map.containsValue(new Student("A", 999, List.of())));
    }

    @Test
    void testRemove() {
        Student s = new Student("toDelete", 999, List.of());
        map.put("del", s);

        Student removed = map.remove("del");

        assertEquals("toDelete", removed.getName());
        assertNull(map.get("del"));
    }

    @Test
    void testKeySet() {
        map.put("a", new Student("A", 10, List.of()));
        map.put("b", new Student("B", 20, List.of()));

        Set<String> keys = map.keySet();

        assertEquals(Set.of("a", "b"), keys);
    }

    @Test
    void testValues() {
        Student s1 = new Student("A", 10, List.of("A"));
        Student s2 = new Student("B", 20, List.of("B"));

        map.put("x1", s1);
        map.put("x2", s2);

        Collection<Student> values = map.values();

        assertTrue(values.contains(s1));
        assertTrue(values.contains(s2));
        assertEquals(2, values.size());
    }

    @Test
    void testEntrySet() {
        Student s1 = new Student("A", 10, List.of("A"));
        Student s2 = new Student("B", 20, List.of("B"));

        map.put("k1", s1);
        map.put("k2", s2);

        Set<Map.Entry<String, Student>> entries = map.entrySet();

        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(e -> e.getKey().equals("k1")));
        assertTrue(entries.stream().anyMatch(e -> e.getKey().equals("k2")));
    }

    @Test
    void testPutAll() {
        Map<String, Student> source = new HashMap<>();
        source.put("s1", new Student("A", 1, List.of()));
        source.put("s2", new Student("B", 2, List.of()));

        map.putAll(source);

        assertEquals(2, map.size());
    }

    @Test
    void testClear() {
        map.put("1", new Student("A", 1, List.of()));
        map.put("2", new Student("B", 2, List.of()));

        map.clear();

        assertTrue(map.isEmpty());
    }
}