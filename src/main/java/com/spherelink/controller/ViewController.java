package com.spherelink.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet; // Changed from ArrayList to HashSet
import java.util.List;
import java.util.Map;
import java.util.Set; // Added for Set type
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spherelink.exception.ResourceNotFoundException;
import com.spherelink.model.FileRecord;
import com.spherelink.model.Marker;
import com.spherelink.model.MarkerBannerImage;
import com.spherelink.model.PanoramaImage;
import com.spherelink.model.Rating;
import com.spherelink.model.ViewData;
import com.spherelink.service.FileService;
import com.spherelink.service.UserService;
import com.spherelink.service.ViewService;

@RestController
@RequestMapping("/spherelink")
public class ViewController {

	@Autowired
	private ViewService viewService;

	@Autowired
	private UserService userService;

	@Autowired
	private FileService fileService;

	@Autowired
	private ObjectMapper objectMapper;

	private static final Logger logger = LoggerFactory.getLogger(ViewController.class);

	@Value("${app.base-url}")
	private String BASE_URL;

	@PostMapping(path = "/views", consumes = "multipart/form-data")
	public ResponseEntity<Map<String, Object>> uploadViewData(
			@RequestParam("thumbnailImage") MultipartFile thumbnailImage, @RequestParam("metadata") String metadataJson,
			@RequestParam(value = "description", required = false) String description,
			@RequestParam Map<String, String> allParams, @RequestParam Map<String, MultipartFile> fileParams)
			throws Exception {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		logger.debug("Authentication object: {}", authentication);

		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getName())) {
			logger.error("No valid authentication found");
			return ResponseEntity.status(401).body(createErrorResponse(401, "Authentication required"));
		}

		String currentUserEmail = authentication.getName();
		logger.info("Processing request for user: {}", currentUserEmail);

		UUID userId = userService.getUserIdByEmail(currentUserEmail);
		if (userId == null) {
			logger.error("User not found for email: {}", currentUserEmail);
			throw new ResourceNotFoundException("User not found with email: " + currentUserEmail);
		}

		logger.debug("Raw metadata JSON: {}", metadataJson);
		ViewData viewData = objectMapper.readValue(metadataJson, ViewData.class);
		logger.debug("Deserialized ViewData object: {}", viewData);

//        // Ensure NOT NULL fields are set
//        if (viewData.getViewName() == null) {
//            logger.warn("viewName is null, setting default");
//            viewData.setViewName("Untitled View");
//        }
//        if (viewData.getDescription() == null) {
//            logger.warn("description is null, setting default");
//            viewData.setDescription(description != null ? description : "");
//        }
//        if (viewData.getLatitude() == null) {
//            logger.warn("Latitude is null or invalid, setting default [0.0, 0.0]");
//            viewData.setLatitude(0.0);
//        }
//        if (viewData.getLocation() == null || viewData.getLocation().length != 2) {
//            logger.warn("location is null or invalid, setting default [0.0, 0.0]");
//            viewData.setLocation(new Double[]{0.0, 0.0});
//        }
//        if (viewData.getDateTime() == null) {
//            logger.warn("dateTime is null, setting current time");
//            viewData.setDateTime(LocalDateTime.now());
//        }

		viewData.setUserId(userId);
		FileRecord fileRecord = fileService.saveFile(thumbnailImage, "thumb_", false);
		viewData.setThumbnailImagePath(fileRecord.getFilePath());

		// Changed from List to Set
		Set<PanoramaImage> panoramaImages = new HashSet<>();
		int i = 0;
		while (allParams.containsKey("panorama[" + i + "][imageName]")) {
			PanoramaImage panorama = new PanoramaImage();
			panorama.setView(viewData);
			String imageName = allParams.get("panorama[" + i + "][imageName]");
			MultipartFile panoFile = fileParams.get("panoramaImage_" + i);
			if (panoFile != null) {
				FileRecord panoRecord = fileService.saveFile(panoFile, "pano_", false);
				panorama.setImagePath(panoRecord.getFilePath());
			}
			panorama.setImageName(imageName);

			String markersJson = allParams.get("panorama[" + i + "][markers]");
			List<Marker> markerList = objectMapper.readValue(markersJson,
					objectMapper.getTypeFactory().constructCollectionType(List.class, Marker.class));
			Set<Marker> markers = new HashSet<>(markerList);

			for (int j = 0; j < markerList.size(); j++) {
				Marker marker = markerList.get(j);

				int bannerIndex = 0;
				while (fileParams.containsKey("bannerImage_" + i + "_" + j + "_" + bannerIndex)) {
					MultipartFile bannerFile = fileParams.get("bannerImage_" + i + "_" + j + "_" + bannerIndex);
					if (bannerFile != null) {
						FileRecord bannerRecord = fileService.saveFile(bannerFile, "banner_", false);
						MarkerBannerImage markerBannerImage = new MarkerBannerImage();
						markerBannerImage.setImagePath(bannerRecord.getFilePath());
						marker.addMarkerBannerImage(markerBannerImage); // Add to the Set
					}
					bannerIndex++;
				}
			}

			markers.forEach(marker -> marker.setPanoramaImage(panorama));
			panorama.setMarkers(markers);
			panoramaImages.add(panorama);
			i++;
		}
		viewData.setPanoramaImages(panoramaImages);

		viewService.saveView(viewData);
		logger.info("View data uploaded successfully for user: {}", currentUserEmail);

		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
		response.put("message", "View data uploaded successfully");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/views")
	public ResponseEntity<Map<String, Object>> getPublishedViews() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getName())) {
			logger.error("No valid authentication found");
			return ResponseEntity.status(401).body(createErrorResponse(401, "Authentication required"));
		}

		String currentUserEmail = authentication.getName();
		UUID userId = userService.getUserIdByEmail(currentUserEmail);
		if (userId == null) {
			logger.error("User not found for email: {}", currentUserEmail);
			return ResponseEntity.status(404).body(createErrorResponse(404, "User not found"));
		}

		List<ViewData> views = viewService.getViewsByUserId(userId);
		logger.info("Fetched {} views for user: {}", views.size(), currentUserEmail);
		for (ViewData view : views) {
			// Normalize paths and prepend BASE_URL
			String thumbnailPath = view.getThumbnailImagePath().replace("\\", "/");
			view.setThumbnailImagePath(BASE_URL + "/" + thumbnailPath);
			view.getPanoramaImages().forEach(pano -> {
				String panoPath = pano.getImagePath().replace("\\", "/");
				pano.setImagePath(BASE_URL + "/" + panoPath);
				pano.getMarkers().forEach(marker -> {
					logger.debug("Marker {} has {} banner images", marker.getMarkerId(),
							marker.getMarkerBannerImages().size());
					marker.getMarkerBannerImages().forEach(banner -> {
						String bannerPath = banner.getImagePath().replace("\\", "/");
						banner.setImagePath(BASE_URL + "/" + bannerPath);
						logger.debug("Banner imagePath: {}", banner.getImagePath());
					});
				});
			});
		}

		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);
		response.put("message", "Published views retrieved successfully");
		response.put("data", views);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/views/{viewId}")
	public ResponseEntity<Map<String, Object>> deleteView(@PathVariable UUID viewId) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication == null || !authentication.isAuthenticated()
					|| "anonymousUser".equals(authentication.getName())) {
				logger.error("No valid authentication found");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(createErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Authentication required"));
			}

			String currentUserEmail = authentication.getName();
			logger.info("Deleting view {} for user: {}", viewId, currentUserEmail);

			UUID userId = userService.getUserIdByEmail(currentUserEmail);
			if (userId == null) {
				logger.error("User not found for email: {}", currentUserEmail);
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(createErrorResponse(HttpStatus.NOT_FOUND.value(), "User not found"));
			}

			ViewData view = viewService.getViewById(viewId);

			if (!view.getUserId().equals(userId)) {
				logger.error("User {} not authorized to delete view {}", currentUserEmail, viewId);
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(createErrorResponse(HttpStatus.FORBIDDEN.value(), "Not authorized to delete this view"));
			}

			// Delete associated files
			fileService.deleteFile(view.getThumbnailImagePath());
			if (view.getPanoramaImages() != null) {
				for (PanoramaImage pano : view.getPanoramaImages()) {
					fileService.deleteFile(pano.getImagePath());
					if (pano.getMarkers() != null) {
						for (Marker marker : pano.getMarkers()) {
							if (marker.getMarkerBannerImages() != null) {
								for (MarkerBannerImage banner : marker.getMarkerBannerImages()) {
									fileService.deleteFile(banner.getImagePath());
								}
							}
						}
					}
				}
			}

			// Delete view from DB
			viewService.deleteView(viewId);
			logger.info("View {} deleted successfully for user: {}", viewId, currentUserEmail);

			Map<String, Object> response = new HashMap<>();
			response.put("status", HttpStatus.OK.value());
			response.put("message", "View deleted successfully");
			return ResponseEntity.ok(response);
		} catch (ResourceNotFoundException e) {
			logger.error("View not found: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(createErrorResponse(HttpStatus.NOT_FOUND.value(), e.getMessage()));
		} catch (Exception e) {
			logger.error("Error deleting view: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
					HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error deleting view: " + e.getMessage()));
		}
	}

	@PutMapping(value = "/views/{viewId}", consumes = "multipart/form-data")
	public ResponseEntity<Map<String, Object>> updateView(@PathVariable UUID viewId,
			@RequestParam(value = "viewName", required = false) String viewName,
			@RequestParam(value = "description", required = false) String description,
			@RequestParam(value = "latitude", required = false) Double latitude,
			@RequestParam(value = "longitude", required = false) Double longitude,
			@RequestParam(value = "thumbnailImage", required = false) MultipartFile thumbnailImage) {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication == null || !authentication.isAuthenticated()
					|| "anonymousUser".equals(authentication.getName())) {
				logger.error("No valid authentication found");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(createErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Authentication required"));
			}

			ViewData view = viewService.getViewById(viewId);
			String currentUserEmail = authentication.getName();
			logger.info("Updating view {} for user: {}", viewId, currentUserEmail);

			UUID userId = userService.getUserIdByEmail(currentUserEmail);
			if (userId == null) {
				logger.error("User not found for email: {}", currentUserEmail);
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(createErrorResponse(HttpStatus.NOT_FOUND.value(), "User not found"));
			}

			// Update fields
			if (viewName != null && !viewName.trim().isEmpty()) {
				view.setViewName(viewName);
			}
			if (description != null && !description.trim().isEmpty()) {
				view.setDescription(description);
			}
			if (latitude != null) {
				view.setLatitude(latitude);
			}
			if (longitude != null) {
				view.setLongitude(longitude);
			}
			// Handle thumbnail update
			if (thumbnailImage != null && !thumbnailImage.isEmpty()) {
				// Delete old thumbnail if exists
				if (view.getThumbnailImagePath() != null) {
					fileService.deleteFile(view.getThumbnailImagePath());
				}
				FileRecord fileRecord = fileService.saveFile(thumbnailImage, "thumb_", false);
				if (fileRecord != null) {
					view.setThumbnailImagePath(fileRecord.getFilePath());
					logger.info("Updated thumbnail for view {}: {}", viewId, fileRecord.getFilePath());
				} else {
					logger.warn("Failed to save thumbnail for view: {}", viewId);
				}
			}

			if (view.getDateTime() == null) {
				logger.warn("dateTime is null, setting current time");
				view.setDateTime(LocalDateTime.now());
			}

			// Save updated view
			viewService.updateView(view);
			logger.info("View {} updated successfully for user: {}", viewId, currentUserEmail);

			Map<String, Object> response = new HashMap<>();
			response.put("status", HttpStatus.OK.value());
			response.put("message", "View updated successfully");
			response.put("data", view);
			return ResponseEntity.ok(response);
		} catch (ResourceNotFoundException e) {
			logger.error("View not found: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(createErrorResponse(HttpStatus.NOT_FOUND.value(), e.getMessage()));
		} catch (Exception e) {
			logger.error("Error updating view: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse(
					HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error updating view: " + e.getMessage()));
		}
	}

	@GetMapping("/views/public")
	public ResponseEntity<Map<String, Object>> getPublicViews(@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String query,
			@RequestParam(defaultValue = "all") String filter, @RequestParam(required = false) Double latitude,
			@RequestParam(required = false) Double longitude) {
		try {
			Page<ViewData> views = viewService.getPublicViews(page, size, query, filter, latitude, longitude);
			for (ViewData view : views.getContent()) {
				if (view.getThumbnailImagePath() != null) {
					view.setThumbnailImagePath(BASE_URL + "/" + view.getThumbnailImagePath().replace("\\", "/"));
				}
				if (view.getCreatorProfileImagePath() != null && view.getCreatorProfileImagePath().startsWith("http")) {
					view.setCreatorProfileImagePath(view.getCreatorProfileImagePath().replace("\\", "/"));
				} else if (view.getCreatorProfileImagePath() != null) {
					view.setCreatorProfileImagePath(
							BASE_URL + "/" + view.getCreatorProfileImagePath().replace("\\", "/"));
				}
				if (view.getPanoramaImages() != null) {
					for (PanoramaImage pano : view.getPanoramaImages()) {
						if (pano.getImagePath() != null) {
							pano.setImagePath(BASE_URL + "/" + pano.getImagePath().replace("\\", "/"));
						}
						pano.setMarkers(null);
					}
				}
			}

			Map<String, Object> response = new HashMap<>();
			response.put("status", HttpStatus.OK.value());
			response.put("message", "Public views retrieved successfully");
			response.put("data", views.getContent());
			response.put("totalPages", views.getTotalPages());
			response.put("totalElements", views.getTotalElements());
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			logger.error("Error fetching public views: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error fetching public views"));
		}
	}

	@PostMapping("/views/{viewId}/ratings")
	public ResponseEntity<Map<String, Object>> addRating(
	        @PathVariable UUID viewId,
	        @RequestParam Integer stars,
	        @RequestParam(required = false) String comment) {  // Remove userId parameter
	    try {
	        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	        if (authentication == null || !authentication.isAuthenticated()
	                || "anonymousUser".equals(authentication.getName())) {
	            logger.error("No valid authentication found");
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                    .body(createErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Authentication required"));
	        }

	        String currentUserEmail = authentication.getName();
	        UUID userId = userService.getUserIdByEmail(currentUserEmail);
	        if (userId == null) {
	            logger.error("User not found for email: {}", currentUserEmail);
	            return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                    .body(createErrorResponse(HttpStatus.NOT_FOUND.value(), "User not found"));
	        }

	        boolean success = viewService.addRating(viewId, stars, comment, userId);
	        if (success) {
	            Map<String, Object> response = new HashMap<>();
	            response.put("status", HttpStatus.OK.value());
	            response.put("message", "Rating submitted successfully");
	            return ResponseEntity.ok(response);
	        } else {
	            return ResponseEntity.badRequest()
	                    .body(createErrorResponse(HttpStatus.BAD_REQUEST.value(), "Failed to submit rating"));
	        }
	    } catch (Exception e) {
	        logger.error("Error adding rating for view {}: {}", viewId, e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error adding rating: " + e.getMessage()));
	    }
	}
	
	@GetMapping("/views/{viewId}/ratings")
	public ResponseEntity<Map<String, Object>> getRatings(
	        @PathVariable UUID viewId,
	        @RequestParam(defaultValue = "1") int page,
	        @RequestParam(defaultValue = "10") int size) {
	    try {
	        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	        if (authentication == null || !authentication.isAuthenticated()
	                || "anonymousUser".equals(authentication.getName())) {
	            logger.error("No valid authentication found");
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                    .body(createErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Authentication required"));
	        }

	        Page<Rating> ratings = viewService.getRatings(viewId, page - 1, size); // Adjust page to 0-based
	        List<Map<String, Object>> ratingList = ratings.getContent().stream().map(rating -> {
	            Map<String, Object> ratingMap = new HashMap<>();
	            ratingMap.put("userName", userService.getUserNameById(rating.getUserId()));
	            ratingMap.put("stars", rating.getStars());
	            ratingMap.put("comment", rating.getComment());
	            ratingMap.put("createdAt", rating.getCreatedAt().toString());
	            return ratingMap;
	        }).collect(Collectors.toList());

	        Map<String, Object> response = new HashMap<>();
	        response.put("status", HttpStatus.OK.value());
	        response.put("message", "Ratings retrieved successfully");
	        response.put("data", ratingList);
	        response.put("totalPages", ratings.getTotalPages());
	        response.put("totalElements", ratings.getTotalElements());
	        return ResponseEntity.ok(response);
	    } catch (Exception e) {
	        logger.error("Error fetching ratings for view {}: {}", viewId, e.getMessage());
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error fetching ratings: " + e.getMessage()));
	    }
	}
	
	// Helper method for error responses
	private Map<String, Object> createErrorResponse(int status, String message) {
		Map<String, Object> response = new HashMap<>();
		response.put("status", status);
		response.put("message", message);
		return response;
	}

	@RestControllerAdvice
	public class GlobalExceptionHandler {

		@ExceptionHandler(MaxUploadSizeExceededException.class)
		public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
			Map<String, Object> response = new HashMap<>();
			response.put("status", 413); // Payload Too Large
			response.put("message", "Upload size exceeds the maximum limit of " + ex.getMaxUploadSize() + " bytes");
			return ResponseEntity.status(413).body(response);
		}
	}
}