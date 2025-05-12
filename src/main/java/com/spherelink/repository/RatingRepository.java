package com.spherelink.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.spherelink.model.Rating;

public interface RatingRepository extends JpaRepository<Rating, UUID> {
	@Query("SELECT r FROM Rating r WHERE r.view.viewId = :viewId")
    Page<Rating> findByViewId(@Param("viewId") UUID viewId, Pageable pageable);
}