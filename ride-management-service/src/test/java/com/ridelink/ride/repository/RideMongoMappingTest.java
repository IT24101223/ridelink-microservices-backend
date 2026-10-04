package com.ridelink.ride.repository;

import com.ridelink.ride.domain.Ride;
import com.ridelink.ride.domain.RideStatus;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RideMongoMappingTest {

    @Test
    void rideAndStatusHistoryRoundTripAsMongoDocument() throws Exception {
        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setInitialEntitySet(Set.of(Ride.class));
        MongoCustomConversions conversions = MongoCustomConversions.create(adapter -> {
        });
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();

        UUID rideId = UUID.randomUUID();
        Ride ride = Ride.builder()
                .id(rideId)
                .passengerId(UUID.randomUUID())
                .driverId(UUID.randomUUID())
                .pickupAddress("University Gate")
                .pickupLat(51.5074)
                .pickupLng(-0.1278)
                .destinationAddress("King's Cross")
                .destinationLat(51.5309)
                .destinationLng(-0.1233)
                .status(RideStatus.ASSIGNED)
                .estimatedFare(new BigDecimal("8.50"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        ride.addHistory(RideStatus.REQUESTED, RideStatus.ASSIGNED, ride.getPassengerId().toString());

        Document document = new Document();
        converter.write(ride, document);
        Ride restored = converter.read(Ride.class, document);

        assertThat(document.get("statusHistory", List.class)).hasSize(1);
        assertThat(restored.getId()).isEqualTo(rideId);
        assertThat(restored.getStatus()).isEqualTo(RideStatus.ASSIGNED);
        assertThat(restored.getEstimatedFare()).isEqualByComparingTo("8.50");
        assertThat(restored.getStatusHistory()).singleElement().satisfies(history -> {
            assertThat(history.getFromStatus()).isEqualTo(RideStatus.REQUESTED);
            assertThat(history.getToStatus()).isEqualTo(RideStatus.ASSIGNED);
        });
    }
}
