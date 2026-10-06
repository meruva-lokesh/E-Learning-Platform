package com.examly.springapp.repository;

import com.examly.springapp.model.VideoProgress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for watch progress. */
@Repository
public interface VideoProgressRepo extends JpaRepository<VideoProgress, Long> {
    Optional<VideoProgress> findByCustomerIdAndVideoId(Long customerId, Long videoId);
    List<VideoProgress> findByCustomerIdAndVideoIdIn(Long customerId, List<Long> videoIds);
    List<VideoProgress> findByVideoIdIn(List<Long> videoIds);
    void deleteByVideoId(Long videoId);
}
