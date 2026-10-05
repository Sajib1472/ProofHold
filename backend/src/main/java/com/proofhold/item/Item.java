package com.proofhold.item;

import com.proofhold.domain.ItemCategory;
import com.proofhold.domain.ItemStatus;
import com.proofhold.location.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemCategory category;

    @Column(name = "found_at", nullable = false)
    private Instant foundAt;

    @Column(name = "hold_until", nullable = false)
    private Instant holdUntil;

    @Column(name = "where_found", nullable = false)
    private String whereFound;

    @Version
    @Column(nullable = false)
    private Integer version;

    public Long getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public void setCategory(ItemCategory category) {
        this.category = category;
    }

    public Instant getFoundAt() {
        return foundAt;
    }

    public void setFoundAt(Instant foundAt) {
        this.foundAt = foundAt;
    }

    public Instant getHoldUntil() {
        return holdUntil;
    }

    public void setHoldUntil(Instant holdUntil) {
        this.holdUntil = holdUntil;
    }

    public String getWhereFound() {
        return whereFound;
    }

    public void setWhereFound(String whereFound) {
        this.whereFound = whereFound;
    }

    public Integer getVersion() {
        return version;
    }
}
