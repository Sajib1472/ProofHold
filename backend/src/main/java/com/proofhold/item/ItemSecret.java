package com.proofhold.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_secrets")
public class ItemSecret {

    @Id
    @Column(name = "item_id")
    private Long itemId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "photo_url")
    private String photoUrl;

    private String serial;

    @Column(name = "unique_marks")
    private String uniqueMarks;

    @Column(name = "full_description", nullable = false)
    private String fullDescription;

    public Long getItemId() {
        return itemId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getUniqueMarks() {
        return uniqueMarks;
    }

    public void setUniqueMarks(String uniqueMarks) {
        this.uniqueMarks = uniqueMarks;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }
}
