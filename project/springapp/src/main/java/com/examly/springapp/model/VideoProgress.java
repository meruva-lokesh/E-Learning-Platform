package com.examly.springapp.model;

import jakarta.persistence.*;

/** How far one customer has watched one video (one row per customer and video). */
@Entity
@Table(name = "video_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_video_progress", columnNames = {"customerId", "videoId"}))
public class VideoProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long videoId;

    /** Where the customer stopped, in seconds. */
    @Column(nullable = false)
    private int positionSec;

    /** The furthest point reached, in seconds. Completion is judged on this so skipping back does not undo it. */
    @Column(nullable = false)
    private int maxPositionSec;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private long updatedAtMs;

    public VideoProgress() {}

    public VideoProgress(Long customerId, Long videoId) {
        this.customerId = customerId;
        this.videoId = videoId;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Long getVideoId() { return videoId; }
    public int getPositionSec() { return positionSec; }
    public void setPositionSec(int positionSec) { this.positionSec = positionSec; }
    public int getMaxPositionSec() { return maxPositionSec; }
    public void setMaxPositionSec(int maxPositionSec) { this.maxPositionSec = maxPositionSec; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public long getUpdatedAtMs() { return updatedAtMs; }
    public void setUpdatedAtMs(long updatedAtMs) { this.updatedAtMs = updatedAtMs; }
}
