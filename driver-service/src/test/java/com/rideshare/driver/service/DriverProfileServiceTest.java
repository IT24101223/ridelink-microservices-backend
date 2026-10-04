package com.rideshare.driver.service;

import com.rideshare.driver.dto.CreateDriverProfileRequest;
import com.rideshare.driver.dto.DriverProfileResponse;
import com.rideshare.driver.dto.VehicleRequest;
import com.rideshare.driver.model.DriverProfile;
import com.rideshare.driver.model.DriverStatus;
import com.rideshare.driver.model.Vehicle;
import com.rideshare.driver.model.VehicleType;
import com.rideshare.driver.repository.DriverProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverProfileServiceTest {

    @Mock
    private DriverProfileRepository driverProfileRepository;

    @InjectMocks
    private DriverProfileService driverProfileService;

    @Test
    void createProfile_whenValidRequest_thenReturnsPersistedProfile() {
        CreateDriverProfileRequest request = new CreateDriverProfileRequest();
        request.setDriverId("driver-101");
        request.setLicenseNumber("DL-12345");
        request.setServiceArea("Downtown");

        VehicleRequest vehicleRequest = new VehicleRequest();
        vehicleRequest.setModel("Toyota Corolla");
        vehicleRequest.setVehicleType("CAR");
        vehicleRequest.setLicensePlate("ABC-123");
        vehicleRequest.setCapacity(4);
        vehicleRequest.setColor("Blue");
        request.setVehicle(vehicleRequest);

        DriverProfile persisted = new DriverProfile();
        persisted.setId("profile-1");
        persisted.setDriverId("driver-101");
        persisted.setLicenseNumber("DL-12345");
        persisted.setServiceArea("Downtown");
        persisted.setAvailabilityStatus(DriverStatus.OFFLINE);
        persisted.setVehicle(new Vehicle("Toyota Corolla", VehicleType.CAR, "ABC-123", 4, "Blue"));
        persisted.setCreatedAt(new Date());
        persisted.setUpdatedAt(new Date());

        when(driverProfileRepository.save(any(DriverProfile.class))).thenReturn(persisted);

        DriverProfileResponse response = driverProfileService.createDriverProfile(request);

        assertNotNull(response);
        assertEquals("driver-101", response.getDriverId());
        assertEquals("Downtown", response.getServiceArea());
        assertEquals("ABC-123", response.getVehicle().getLicensePlate());
        verify(driverProfileRepository).save(any(DriverProfile.class));
    }

    @Test
    void getAvailableDrivers_whenMatchingDriversExists_thenReturnsEligibleDrivers() {
        DriverProfile first = new DriverProfile();
        first.setId("profile-1");
        first.setDriverId("driver-1");
        first.setServiceArea("Downtown");
        first.setAvailabilityStatus(DriverStatus.AVAILABLE);
        first.setVehicle(new Vehicle("Toyota Corolla", VehicleType.CAR, "ABC-123", 4, "Blue"));

        DriverProfile second = new DriverProfile();
        second.setId("profile-2");
        second.setDriverId("driver-2");
        second.setServiceArea("Downtown");
        second.setAvailabilityStatus(DriverStatus.AVAILABLE);
        second.setVehicle(new Vehicle("Honda Civic", VehicleType.CAR, "XYZ-999", 4, "Silver"));

        when(driverProfileRepository.findByAvailabilityStatusAndServiceAreaAndVehicle_VehicleType(
                DriverStatus.AVAILABLE, "Downtown", VehicleType.CAR)).thenReturn(List.of(first, second));

        List<DriverProfileResponse> response = driverProfileService.getAvailableDrivers("Downtown", "CAR");

        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("driver-1", response.get(0).getDriverId());
        assertEquals("driver-2", response.get(1).getDriverId());
    }
}
