# Assumed Contracts – Ride Service Dependencies

> **Status:** ASSUMPTIONS – pending team agreement. All upstream contracts documented here  
> are guesses based on the project brief. Teammates should confirm or correct these.

---

## 1. Driver & Vehicle Service (port 8082)

### `GET /api/drivers/eligible`

Fetches available drivers near a pickup location.

**Query params:**

| Param      | Type   | Description             |
|------------|--------|-------------------------|
| `lat`      | double | Pickup latitude         |
| `lng`      | double | Pickup longitude        |
| `radiusKm` | double | Search radius (km)      |

**Response 200 OK:**
```json
[
  {
    "driverId":    "bbbb0000-0000-0000-0000-000000000001",
    "currentLat":  51.512,
    "currentLng":  -0.130,
    "displayName": "Alice Smith"
  }
]
```
Returns empty array `[]` if no drivers available.

---

### `PATCH /api/drivers/{driverId}/availability`

Marks a driver as busy or available.

**Request body:**
```json
{ "available": false }
```

**Response:** 200 OK (no body)

---

## 2. Fare & Payment Service (port 8084)

### `GET /api/fares/estimate`

Returns a fare estimate before the ride starts.

**Query params:**

| Param       | Type   | Description            |
|-------------|--------|------------------------|
| `pickupLat` | double | Pickup latitude        |
| `pickupLng` | double | Pickup longitude       |
| `destLat`   | double | Destination latitude   |
| `destLng`   | double | Destination longitude  |

**Response 200 OK:**
```json
{ "estimatedFare": 8.50 }
```

---

### `POST /api/fares/finalize`

Finalizes fare and records simulated payment on ride completion.

**Request body:**
```json
{
  "rideId":      "cccc0000-0000-0000-0000-000000000003",
  "passengerId": "aaaa0000-0000-0000-0000-000000000001",
  "driverId":    "bbbb0000-0000-0000-0000-000000000001",
  "pickupLat":   51.5074,
  "pickupLng":   -0.1278,
  "destLat":     51.5155,
  "destLng":     -0.1415
}
```

**Response 200 OK:**
```json
{
  "finalFare": 9.25,
  "paymentId": "00000000-0000-0000-0000-aabbccddeeff"
}
```

---

## Open Questions for Team

1. Does `GET /api/drivers/eligible` require an `Authorization: Bearer <admin-token>` header?
2. Is the availability endpoint body `{ "available": true/false }` or a different shape?
3. Does the Fare Service call `GET /api/fares/estimate` or `POST /api/fares/estimate`?
4. What HTTP status does Fare Service return when coordinates are invalid?
5. Should `finalizeAndPay` be idempotent (i.e. safe to retry)?
