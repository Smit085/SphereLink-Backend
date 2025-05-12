
package com.spherelink.service;

import com.spherelink.model.PanoramaImage;
import com.spherelink.model.Rating;
import com.spherelink.model.ViewData;
import com.spherelink.repository.RatingRepository;
import com.spherelink.repository.ViewRepository;
import com.spherelink.exception.ResourceNotFoundException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ViewService {

	@Autowired
	private ViewRepository viewRepository;

	@Autowired
	private RatingRepository ratingRepository;

	public ViewData saveView(ViewData view) {
		if (view.getDateTime() == null) {
			view.setDateTime(LocalDateTime.now());
		}
		return viewRepository.save(view);
	}

	@Transactional(readOnly = true)
	public List<ViewData> getViewsByUserId(UUID userId) {
		List<ViewData> views = viewRepository.findByUserId(userId);
		views.forEach(view -> {
			Hibernate.initialize(view.getPanoramaImages());
			if (view.getPanoramaImages() != null) {
				view.getPanoramaImages().forEach(pano -> {
					Hibernate.initialize(pano.getMarkers());
					if (pano.getMarkers() != null) {
						pano.getMarkers().forEach(marker -> Hibernate.initialize(marker.getMarkerBannerImages()));
					}
				});
			}
		});
		return views;
	}

	@Transactional(readOnly = true)
	public ViewData getViewById(UUID viewId) {
		ViewData view = viewRepository.findById(viewId)
				.orElseThrow(() -> new ResourceNotFoundException("View not found with ID: " + viewId));
		Hibernate.initialize(view.getPanoramaImages());
		if (view.getPanoramaImages() != null) {
			view.getPanoramaImages().forEach(pano -> {
				Hibernate.initialize(pano.getMarkers());
				if (pano.getMarkers() != null) {
					pano.getMarkers().forEach(marker -> Hibernate.initialize(marker.getMarkerBannerImages()));
				}
			});
		}
		return view;
	}

	@Transactional
	public void deleteView(UUID viewId) {
		if (!viewRepository.existsById(viewId)) {
			throw new ResourceNotFoundException("View not found with ID: " + viewId);
		}
		viewRepository.deleteById(viewId);
	}

	@Transactional
	public ViewData updateView(ViewData view) {
		if (!viewRepository.existsById(view.getViewId())) {
			throw new ResourceNotFoundException("View not found with ID: " + view.getViewId());
		}
		return viewRepository.save(view);
	}

	@Transactional(readOnly = true)
	public Page<ViewData> getPublicViews(int page, int size, String query, String filter, Double latitude,
	        Double longitude) {
	    PageRequest pageable = PageRequest.of(page - 1, size);
	    Page<ViewData> views;
	    if (filter.equals("nearby") && latitude != null && longitude != null) {
	        views = viewRepository.findNearbyViews(query, latitude, longitude, 10.0, pageable);
	    } else if (filter.equals("recent")) {
	        views = viewRepository.findPublicViewsRecent(query, pageable);
	    } else if (filter.equals("most_rated")) {
	        views = viewRepository.findPublicViewsMostRated(query, pageable);
	    } else {
	        views = viewRepository.findPublicViewsAll(query, pageable);
	    }
	    views.getContent().forEach(view -> {
	        Hibernate.initialize(view.getPanoramaImages());
	        if (view.getPanoramaImages() != null) {
	            view.getPanoramaImages().forEach(pano -> {
	                Hibernate.initialize(pano.getMarkers());
	                if (pano.getMarkers() != null) {
	                    pano.getMarkers().forEach(marker -> Hibernate.initialize(marker.getMarkerBannerImages()));
	                }
	            });
	        }
	        Hibernate.initialize(view.getRatings());
	    });
	    return views;
	}

	@Transactional
	public boolean addRating(UUID viewId, Integer stars, String comment, UUID userId) {
		// Validate stars (1 to 5)
		if (stars < 1 || stars > 5) {
			return false;
		}

		// Find the view
		Optional<ViewData> viewOptional = viewRepository.findById(viewId);
		if (viewOptional.isEmpty()) {
			return false;
		}

		ViewData view = viewOptional.get();
		Hibernate.initialize(view.getRatings());
		
		// Create a new rating
		Rating rating = new Rating();
		rating.setStars(stars);
		rating.setComment(comment);
		rating.setUserId(userId);
		rating.setView(view);

		// Save the rating
		ratingRepository.save(rating);

		view.calculateAverageRating();
		viewRepository.save(view);

		return true;
	}
	
	@Transactional(readOnly = true)
	public Page<Rating> getRatings(UUID viewId, int page, int size) {
	    PageRequest pageable = PageRequest.of(page, size);
	    return ratingRepository.findByViewId(viewId, pageable);
	}
}