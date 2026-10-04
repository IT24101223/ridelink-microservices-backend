package com.ridelink.ride.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables MongoDB auditing for ride creation and update timestamps.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
