package com.spherelink.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "ratings")
@Data
public class Rating {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "rating_id")
	@JsonProperty("ratingId")
	private UUID ratingId;

	@Column(name = "stars", nullable = false)
	private Integer stars; // Rating value (1 to 5)

	@Column(name = "comment", length = 800)
	private String comment; // Optional comment, max length 800 as per your TextField

	@Column(name = "user_id", nullable = false)
	private UUID userId; // User who submitted the rating

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt; // Timestamp of the rating

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "view_id", nullable = false)
	@JsonBackReference // Avoid circular reference during serialization
	private ViewData view; // Reference to the associated view

	// Constructor for setting default values
	public Rating() {
		this.createdAt = LocalDateTime.now();
	}

	public Integer getStars() {
		return stars;
	}

	public void setStars(Integer stars) {
		this.stars = stars;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public void setView(ViewData view) {
		this.view = view;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getComment() {
		return comment;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}