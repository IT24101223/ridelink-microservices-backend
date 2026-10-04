package com.ridelink.ride.dto;

import com.ridelink.ride.domain.Ride;
import com.ridelink.ride.domain.RideStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting between {@link Ride} entities and {@link RideResponse} DTOs.
 *
 * <p>Configured with {@code componentModel = "spring"} so it's injectable as a Spring bean.
 * The annotation processor generates the implementation at compile time – no reflection overhead.
 */
@Mapper(componentModel = "spring")
public interface RideMapper {

    /** Maps all fields by name – they are identical between entity and DTO. */
    RideResponse toResponse(Ride ride);

    /**
     * Creates a new {@link Ride} from the request and a set of transient values
     * (passengerId, id, status) supplied by the service layer.
     */
    @Mapping(target = "driverId",     ignore = true)
    @Mapping(target = "estimatedFare",ignore = true)
    @Mapping(target = "finalFare",    ignore = true)
    @Mapping(target = "paymentId",    ignore = true)
    @Mapping(target = "cancelReason", ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "assignedAt",   ignore = true)
    @Mapping(target = "acceptedAt",   ignore = true)
    @Mapping(target = "startedAt",    ignore = true)
    @Mapping(target = "completedAt",  ignore = true)
    @Mapping(target = "cancelledAt",  ignore = true)
    @Mapping(target = "statusHistory",ignore = true)
    Ride toEntity(CreateRideRequest request,
                  java.util.UUID id,
                  java.util.UUID passengerId,
                  RideStatus status);
}
