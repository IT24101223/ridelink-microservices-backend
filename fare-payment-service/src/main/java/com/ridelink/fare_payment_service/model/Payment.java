package com.ridelink.fare_payment_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "payments")
public class Payment {
    
    @Id
    private String id;
    
    private String rideId;
    private String passengerId;
    private double amount;
    private PaymentStatus status;
    private LocalDateTime paymentTime;

    public enum PaymentStatus {
        PENDING, SUCCESSFUL, FAILED
    }
}