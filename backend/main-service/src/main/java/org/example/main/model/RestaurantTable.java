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
    private String code; 

    @Column(nullable = false)
    private Integer seats; 

    @Column(nullable = false)
    private Integer currentOccupancy = 0; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(nullable = false)
    private String pinCode;

    @Column(name = "table_number", unique = true)
    private Integer tableNumber;

    
    @Column(name = "occupied_until")
    private OffsetDateTime occupiedUntil;
}