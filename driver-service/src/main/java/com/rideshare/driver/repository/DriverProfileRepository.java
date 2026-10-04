package com.rideshare.driver.repository;

import com.rideshare.driver.model.DriverProfile;
import com.rideshare.driver.model.DriverStatus;
import com.rideshare.driver.model.VehicleType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DriverProfileRepository extends MongoRepository<DriverProfile, String> {
    Optional<DriverProfile> findByDriverId(String driverId);

    List<DriverProfile> findByAvailabilityStatusAndServiceAreaAndVehicle_VehicleType(
            DriverStatus availabilityStatus,
            String serviceArea,
            VehicleType vehicleType
    );
}
