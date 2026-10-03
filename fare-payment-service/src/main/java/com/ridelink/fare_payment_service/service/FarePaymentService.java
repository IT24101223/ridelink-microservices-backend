package com.ridelink.fare_payment_service.service;

import com.ridelink.fare_payment_service.model.Payment;
import com.ridelink.fare_payment_service.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class FarePaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public double calculateFare(double distanceInKm) {
        double baseFare = 150.0;
        double perKmRate = 50.0;
        return baseFare + (distanceInKm * perKmRate);
    }

    public Payment processPayment(String rideId, String passengerId, double distanceInKm) {
        double calculatedAmount = calculateFare(distanceInKm);
        
        Payment payment = new Payment();
        payment.setRideId(rideId);
        payment.setPassengerId(passengerId);
        payment.setAmount(calculatedAmount);
        payment.setStatus(Payment.PaymentStatus.SUCCESSFUL);
        payment.setPaymentTime(LocalDateTime.now());
        
        return paymentRepository.save(payment);
    }

    public Payment getPaymentReceipt(String rideId) {
        return paymentRepository.findByRideId(rideId);
    }
}
