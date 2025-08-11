package com.nipunapps.cardgame.testconfig;

import com.redis.testcontainers.RedisContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractContainerProperties {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer(
            DockerImageName.parse("mongo:6.0")
    );

    @Container
    static final RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis:7.0.5-alpine")
    );

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        mongo.start();
        redis.start();
        registry.add("spring.data.mongodb.host", mongo::getHost);
        registry.add("spring.data.mongodb.database", () -> "dragon-tiger");
        registry.add("spring.data.mongodb.port", mongo::getFirstMappedPort);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
}
