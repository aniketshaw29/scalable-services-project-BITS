package com.campuseventhub.resource.repository;

import com.campuseventhub.resource.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByEventId(Long eventId);
    Optional<Resource> findByStorageKey(String storageKey);
}
