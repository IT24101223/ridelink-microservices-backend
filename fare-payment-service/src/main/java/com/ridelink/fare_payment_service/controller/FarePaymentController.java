package com.ridelink.fare_payment_service.controller;

import com.ridelink.fare_payment_service.model.Payment;
import com.ridelink.fare_payment_service.service.FarePaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class FarePaymentController {

    @Autowired
    private FarePaymentService farePaymentService;

    @PostMapping("/process")
    public ResponseEntity<Payment> processPayment(@RequestParam String rideId, 
                                                  @RequestParam String passengerId, 
                                                  @RequestParam double distanceInKm) {
        Payment payment = farePaymentService.processPayment(rideId, passengerId, distanceInKm);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/receipt/{rideId}")
    public ResponseEntity<Payment> getReceipt(@PathVariable String rideId) {
        Payment payment = farePaymentService.getPaymentReceipt(rideId);
        if (payment != null) {
            return ResponseEntity.ok(payment);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}