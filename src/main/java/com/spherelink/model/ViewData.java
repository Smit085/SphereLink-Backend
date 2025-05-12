package com.spherelink.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "views")
@Data
public class ViewData {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "view_id")
	@JsonProperty("viewId")
	private UUID viewId;

	@Column(name = "view_name", nullable = false)
	private String viewName;

	@Column(name = "description", nullable = false)
	private String description;

	@Column(name = "creator_name", nullable = false)
	private String creatorName;

	@Column(name = "city_name", nullable = false)
	private String cityName;

	@Column(name = "creator_profile_image_path", nullable = false)
	private String creatorProfileImagePath;

	@Column(name = "latitude", nullable = false, columnDefinition = "double precision")
	private Double latitude;

	@Column(name = "longitude", nullable = false, columnDefinition = "double precision")
	private Double longitude;

	@Column(name = "thumbnail_image")
	private String thumbnailImagePath;

	@Column(name = "is_public", columnDefinition = "BOOLEAN DEFAULT TRUE")
	private boolean isPublic;

	@Column(name = "date_time", nullable = false)
	private LocalDateTime dateTime;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

//    @OneToMany(mappedBy = "view", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<PanoramaImage> panoramaImages;

	@OneToMany(mappedBy = "view", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference // Serialize this side
	private Set<PanoramaImage> panoramaImages;

	@Column(name = "average_rating", columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
	private Double averageRating;

	@OneToMany(mappedBy = "view", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	private Set<Rating> ratings;

	// Method to calculate and update the average rating
	public void calculateAverageRating() {
		if (ratings == null || ratings.isEmpty()) {
			this.averageRating = 0.0;
		} else {
			double totalStars = ratings.stream().mapToInt(Rating::getStars).sum();
			this.averageRating = totalStars / ratings.size();
		}
	}

	public String getThumbnailImagePath() {
		return thumbnailImagePath;
	}

	public Set<PanoramaImage> getPanoramaImages() {
		return panoramaImages;
	}

	public void setThumbnailImagePath(String thumbnailPath) {
		this.thumbnailImagePath = thumbnailPath;
	}

	public void setPanoramaImages(Set<PanoramaImage> panoramaImages) {
		this.panoramaImages = panoramaImages;
	}

	public LocalDateTime getDateTime() {
		return this.dateTime;
	}

	public void setDateTime(LocalDateTime dateTime) {
		this.dateTime = dateTime;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	public String getViewName() {
		return this.viewName;
	}

	public String getDescription() {
		return this.description;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLatitude() {
		return this.latitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getLongitude() {
		return this.longitude;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public String getCityName() {
		return this.cityName;
	}

	public void setCreatorName(String creatorName) {
		this.creatorName = creatorName;
	}

	public String getcreatorName() {
		return this.creatorName;
	}

	public void setCreatorProfileImagePath(String creatorProfileImagePath) {
		this.creatorProfileImagePath = creatorProfileImagePath;
	}

	public String getCreatorProfileImagePath() {
		return this.creatorProfileImagePath;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getViewId() {
		return viewId;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public Double getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(Double averageRating) {
		this.averageRating = averageRating;
	}

	public Set<Rating> getRatings() {
		return ratings;
	}

	public void setRatings(Set<Rating> ratings) {
		this.ratings = ratings;
	}

}