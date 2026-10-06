package com.examly.springapp.model;

import jakarta.persistence.*;

/**
 * Details (metadata) of one uploaded lesson video. The video file itself lives on disk under
 * {@code video.storage-dir}; this row says what it is and where it belongs.
 * The course is kept as a plain number on purpose, so the existing Course table is not changed.
 */
@Entity
@Table(name = "course_videos", indexes = @Index(name = "idx_course_videos_course", columnList = "courseId"))
public class CourseVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    /** Random name on disk, never the name the user chose. */
    @Column(nullable = false, unique = true, length = 80)
    private String storedName;

    @Column(length = 200)
    private String originalName;

    @Column(nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    /** Length in seconds as reported by the uploader's browser; 0 when unknown. */
    @Column(nullable = false)
    private int durationSec;

    @Column(nullable = false)
    private Long uploadedByUserId;

    @Column(nullable = false)
    private long uploadedAtMs;

    public CourseVideo() {}

    public CourseVideo(Long courseId, String title, String description, String storedName, String originalName,
                       String contentType, long sizeBytes, int durationSec, Long uploadedByUserId, long uploadedAtMs) {
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.storedName = storedName;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.durationSec = durationSec;
        this.uploadedByUserId = uploadedByUserId;
        this.uploadedAtMs = uploadedAtMs;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStoredName() { return storedName; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public int getDurationSec() { return durationSec; }
    public Long getUploadedByUserId() { return uploadedByUserId; }
    public long getUploadedAtMs() { return uploadedAtMs; }
}
