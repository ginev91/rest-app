package org.example.main.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.example.main.model.enums.TableStatus;

@Entity
@Table(name = "restaurant_tables")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantTable {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code; // T1, T2

    @Column(nullable = false)
    private Integer seats; // how many people it fits

    @Column(nullable = false)
    private Integer currentOccupancy = 0; // how many people currently seated

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(nullable = false)
    private String pinCode;

    @Column(name = "table_number", unique = true)
    private Integer tableNumber;

    // Occupied until; null when not occupied
    @Column(name = "occupied_until")
    private OffsetDateTime occupiedUntil;
}