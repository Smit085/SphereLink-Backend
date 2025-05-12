package com.spherelink.model;

import lombok.Data;
import jakarta.persistence.*;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "marker_banner_images")
@Data
public class MarkerBannerImage {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "image_id")
    private UUID imageId;

    @ManyToOne
    @JoinColumn(name = "marker_id", nullable = false)
    @JsonBackReference // Ignore this side
    private Marker marker;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    // Explicit getter for marker
    public Marker getMarker() {
        return marker;
    }

    // Existing setters (kept for completeness)
    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public void setImagePath(String bannerImagePath) {
        this.imagePath = bannerImagePath;
    }

	public String getImagePath() {
		return imagePath;
	}
}