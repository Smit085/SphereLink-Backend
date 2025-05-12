package com.spherelink.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spherelink.model.FileRecord;

@Repository
public interface FileRepository extends JpaRepository<FileRecord, Long> {
}