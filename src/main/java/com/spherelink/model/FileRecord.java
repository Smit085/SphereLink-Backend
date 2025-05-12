package com.spherelink.model;

import jakarta.persistence.*;

@Entity
@Table(name = "file_records") // Explicit table name
public class FileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false) // Ensures column name consistency and prevents null values
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    // Default constructor (required for JPA)
    public FileRecord() {
    }

    // Parameterized constructor for easy object creation
    public FileRecord(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }

    // Getters only (Immutable object, prevents accidental modifications)
    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }
}
