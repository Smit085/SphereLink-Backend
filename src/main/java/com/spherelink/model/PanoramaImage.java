package com.spherelink.model;

import lombok.Data;
import jakarta.persistence.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "panorama_images")
@Data
public class PanoramaImage {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "image_id")
    private UUID imageId;

    @ManyToOne
    @JoinColumn(name = "view_id", nullable = false)
    @JsonBackReference // Ignore this side during serialization
    private ViewData view;

    @Column(name = "image_name", nullable = false)
    private String imageName;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

//    @OneToMany(mappedBy = "panoramaImage", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<Marker> markers;
    
    @OneToMany(mappedBy = "panoramaImage", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // Serialize this side
    private Set<Marker> markers; // Change from List to Set

    public String getImagePath() {
        return imagePath;
    }
    
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setMarkers(Set<Marker> markers) {
        this.markers = markers;
    }
    
    public void setView(ViewData view) {  // Added missing setter
        this.view = view;
    }

	public String getImageName() {
		return imageName;
	}

	public Set<Marker> getMarkers() {
		return markers;
	}
}