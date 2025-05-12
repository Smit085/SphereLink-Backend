
package com.spherelink.repository;

import com.spherelink.model.ViewData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ViewRepository extends JpaRepository<ViewData, UUID> {
    @Query("SELECT v FROM ViewData v " +
           "LEFT JOIN FETCH v.panoramaImages p " +
           "LEFT JOIN FETCH p.markers m " +
           "LEFT JOIN FETCH m.markerBannerImages " +
           "WHERE v.userId = :userId")
    List<ViewData> findByUserId(UUID userId);

    @Query("SELECT v FROM ViewData v WHERE v.isPublic = true " +
           "AND (:query IS NULL OR " +
           "v.viewName ILIKE CONCAT('%', :query, '%') OR " +
           "v.cityName ILIKE CONCAT('%', :query, '%') OR " +
           "v.creatorName ILIKE CONCAT('%', :query, '%'))")
    Page<ViewData> findPublicViewsAll(String query, Pageable pageable);

    @Query("SELECT v FROM ViewData v WHERE v.isPublic = true " +
           "AND (:query IS NULL OR " +
           "v.viewName ILIKE CONCAT('%', :query, '%') OR " +
           "v.cityName ILIKE CONCAT('%', :query, '%') OR " +
           "v.creatorName ILIKE CONCAT('%', :query, '%')) " +
           "ORDER BY v.dateTime DESC NULLS LAST")
    Page<ViewData> findPublicViewsRecent(String query, Pageable pageable);

    @Query("SELECT v FROM ViewData v WHERE v.isPublic = true " +
           "AND (:query IS NULL OR " +
           "v.viewName ILIKE CONCAT('%', :query, '%') OR " +
           "v.cityName ILIKE CONCAT('%', :query, '%') OR " +
           "v.creatorName ILIKE CONCAT('%', :query, '%')) " +
           "ORDER BY v.averageRating DESC NULLS LAST")
    Page<ViewData> findPublicViewsMostRated(String query, Pageable pageable);

    @Query(value = "SELECT * FROM views v WHERE v.is_public = true " +
           "AND (:query IS NULL OR v.view_name ILIKE '%' || :query || '%' OR v.city_name ILIKE '%' || :query || '%' OR v.creator_name ILIKE '%' || :query || '%') " +
           "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(v.latitude)) * cos(radians(v.longitude) - radians(:longitude)) + sin(radians(:latitude)) * sin(radians(v.latitude)))) < :radius",
           nativeQuery = true)
    Page<ViewData> findNearbyViews(String query, double latitude, double longitude, double radius, Pageable pageable);
    
    
    @Modifying
    @Query("UPDATE ViewData v SET v.creatorProfileImagePath = :newImagePath WHERE v.userId = :userId")
    int updateCreatorProfileImagePath(@Param("userId") UUID userId, @Param("newImagePath") String newImagePath);
}
