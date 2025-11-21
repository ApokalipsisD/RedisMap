package org.redis;

import lombok.extern.slf4j.Slf4j;
import org.redis.model.Student;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;

@Slf4j
public class Main {
    private static final String HOST = "localhost";
    private static final int PORT = 6379;
    private static final String REDIS_KEY = "students";


    public static void main(String[] args) {

        RedisMap<String, Student> map = getStringStudentRedisMap();

        log.info("user1: {}", map.get("user1"));
        log.info("user2: {}", map.get("user2"));

        log.info("Map size: {}", map.size());
        log.info("Keys: {}", map.keySet());
        log.info("Values: {}", map.values());

        map.forEach((key, value) -> log.info("Entry: key={}, value={}", key, value));
    }

    private static RedisMap<String, Student> getStringStudentRedisMap() {
        JedisPool pool = new JedisPool(new JedisPoolConfig(), HOST, PORT);

        RedisMap<String, Student> map = new RedisMap<>(
                pool,
                REDIS_KEY,
                String.class,
                Student.class
        );

        map.clear();

        Student s1 = new Student("Sergey", 20, List.of("Java", "Spring", "Redis"));
        Student s2 = new Student("Andrew", 22, List.of("Python", "Django"));

        map.put("user1", s1);
        map.put("user2", s2);
        return map;
    }
}