package com.examly.springapp.dto;

import java.util.List;

/** Shapes of the video API. */
public final class VideoDtos {
    private VideoDtos() {}

    /** One video in a course. {@code progress} is filled only for a customer. */
    public static class VideoView {
        public Long id;
        public Long courseId;
        public String title;
        public String description;
        public long sizeBytes;
        public int durationSec;
        public long uploadedAtMs;
        public ProgressView progress;
    }

    public static class ProgressView {
        public int positionSec;
        public int percent;
        public boolean completed;
    }

    public static class ProgressRequest {
        public Integer positionSec;
        public Integer durationSec;
    }

    /** A short-lived pass that lets the browser's video player fetch one video. */
    public static class TicketView {
        public String ticket;
        public String url;
        public int expiresInSeconds;
    }

    /** What the staff see for one video: how many customers started and finished it. */
    public static class VideoStat {
        public Long videoId;
        public String title;
        public int durationSec;
        public int viewers;
        public int completedCount;
        public int averagePercent;
    }

    public static class CourseStats {
        public Long courseId;
        public int enrolledCustomers;
        public List<VideoStat> videos;
    }

    /** One course in a customer's "my learning" page. */
    public static class LearningItem {
        public Long courseId;
        public String courseType;
        public int totalVideos;
        public int completedVideos;
        public int percent;
        public Long resumeVideoId;
        public int resumePositionSec;
    }
}
