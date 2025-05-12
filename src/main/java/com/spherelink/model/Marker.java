package com.spherelink.model;

import lombok.Data;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "markers")
@Data
public class Marker {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "marker_id")
    private UUID markerId;

    @ManyToOne
    @JoinColumn(name = "image_id", nullable = false)
    @JsonBackReference // Ignore this side during serialization
    private PanoramaImage panoramaImage;
    
    @OneToMany(mappedBy = "marker", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<MarkerBannerImage> markerBannerImages;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "label")
    private String label;

    @Column(name = "subtitle")
    private String subTitle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address")
    private String address;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "selected_icon_style", nullable = false)
    private String selectedIconStyle;

    @Column(name = "selected_icon", nullable = false)
    private Long selectedIcon;

    @Column(name = "selected_icon_color", nullable = false)
    private Long selectedIconColor;

    @Column(name = "selected_icon_rotation", nullable = false)
    private Double selectedIconRotationRadians;

    @Column(name = "selected_action", nullable = false)
    private String selectedAction;

    @Column(name = "next_image_id")
    private Integer nextImageId;

    @Column(name = "link", columnDefinition = "TEXT")
    private String link;

    @Column(name = "link_label")
    private String linkLabel;

    // Getters and Setters
    public UUID getMarkerId() {
        return markerId;
    }

    public void setMarkerId(UUID markerId) {
        this.markerId = markerId;
    }

    public PanoramaImage getPanoramaImage() {
        return panoramaImage;
    }

    public void setPanoramaImage(PanoramaImage panoramaImage) {
        this.panoramaImage = panoramaImage;
    }

    public Set<MarkerBannerImage> getMarkerBannerImages() {
        return markerBannerImages;
    }

    public void setMarkerBannerImages(Set<MarkerBannerImage> markerBannerImages) {
        this.markerBannerImages = markerBannerImages;
    }
    
    // Helper method to add a banner image
    public void addMarkerBannerImage(MarkerBannerImage bannerImage) {
        if (markerBannerImages == null) {
            markerBannerImages = new HashSet<>();
        }
        markerBannerImages.add(bannerImage);
        bannerImage.setMarker(this); // Ensure bidirectional consistency
    }

    // Deprecated old setter for single banner image
    @Deprecated
    public void setMarkerBannerImage(MarkerBannerImage markerBannerImage) {
        addMarkerBannerImage(markerBannerImage);
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getSelectedIconStyle() {
        return selectedIconStyle;
    }

    public void setSelectedIconStyle(String selectedIconStyle) {
        this.selectedIconStyle = selectedIconStyle;
    }

    public Long getSelectedIcon() {
        return selectedIcon;
    }

    public void setSelectedIcon(Long selectedIcon) {
        this.selectedIcon = selectedIcon;
    }

    public Long getSelectedIconColor() {
        return selectedIconColor;
    }

    public void setSelectedIconColor(Long selectedIconColor) {
        this.selectedIconColor = selectedIconColor;
    }

    public Double getSelectedIconRotationRadians() {
        return selectedIconRotationRadians;
    }

    public void setSelectedIconRotationRadians(Double selectedIconRotationRadians) {
        this.selectedIconRotationRadians = selectedIconRotationRadians;
    }

    public String getSelectedAction() {
        return selectedAction;
    }

    public void setSelectedAction(String selectedAction) {
        this.selectedAction = selectedAction;
    }

    public Integer getNextImageId() {
        return nextImageId;
    }

    public void setNextImageId(Integer nextImageId) {
        this.nextImageId = nextImageId;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLinkLabel() {
        return linkLabel;
    }

    public void setLinkLabel(String linkLabel) {
        this.linkLabel = linkLabel;
    }
}