package com.ridelink.fare_payment_service.repository;

import com.ridelink.fare_payment_service.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    Payment findByRideId(String rideId);
}