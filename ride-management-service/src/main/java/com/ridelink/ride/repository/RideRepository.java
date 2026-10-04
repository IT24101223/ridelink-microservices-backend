package com.ridelink.ride.repository;

import com.ridelink.ride.domain.Ride;
import com.ridelink.ride.domain.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data MongoDB repository for {@link Ride}.
 *
 * <p>All queries are derived from method names – no raw query needed for this scope,
 * keeping the repository as thin as possible (Interface Segregation).
 */
public interface RideRepository extends MongoRepository<Ride, UUID> {

    /** Returns all rides owned by the given passenger. */
    List<Ride> findByPassengerId(UUID passengerId);

    /** Returns passenger rides filtered by status. */
    List<Ride> findByPassengerIdAndStatus(UUID passengerId, RideStatus status);

    /** Returns rides assigned to the given driver. */
    List<Ride> findByDriverId(UUID driverId);

    /** Returns rides assigned to a driver filtered by status. */
    List<Ride> findByDriverIdAndStatus(UUID driverId, RideStatus status);

    /** Admin paged view – all rides. */
    Page<Ride> findAll(Pageable pageable);

    /** Admin paged view filtered by status. */
    Page<Ride> findByStatus(RideStatus status, Pageable pageable);

    /** Used for ownership check: find a ride the passenger owns. */
    Optional<Ride> findByIdAndPassengerId(UUID id, UUID passengerId);

    /** Used for ownership check: find a ride assigned to the driver. */
    Optional<Ride> findByIdAndDriverId(UUID id, UUID driverId);
}
