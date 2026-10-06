package com.examly.springapp.controller;

import com.examly.springapp.config.RateLimiter;
import com.examly.springapp.dto.VideoDtos.*;
import com.examly.springapp.exception.RateLimitExceededException;
import com.examly.springapp.model.CourseVideo;
import com.examly.springapp.service.AccessService;
import com.examly.springapp.service.VideoService;
import com.examly.springapp.video.RangeParser;
import com.examly.springapp.video.VideoStorageService;
import com.examly.springapp.video.VideoStreamer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Lesson videos: upload (admin / owning instructor), watch (staff or enrolled customer) and progress. */
@RestController
@RequestMapping("/api/video")
@Tag(name = "Video", description = "Lesson videos, streaming and watch progress")
public class VideoController {
    private final VideoService service;
    private final VideoStorageService storage;
    private final AccessService access;
    private final RateLimiter limiter;

    @Autowired
    public VideoController(VideoService service, VideoStorageService storage, AccessService access, RateLimiter limiter) {
        this.service = service;
        this.storage = storage;
        this.access = access;
        this.limiter = limiter;
    }

    @Operation(summary = "Upload a video to a course (admin, or the approved instructor who owns it)")
    @PostMapping("/course/{courseId}")
    public ResponseEntity<VideoView> upload(@PathVariable Long courseId,
                                            @RequestParam("title") String title,
                                            @RequestParam(value = "description", required = false) String description,
                                            @RequestParam(value = "durationSec", required = false) Integer durationSec,
                                            @RequestParam("file") MultipartFile file) {
        String key = "video-upload:" + access.currentEmail();
        if (!limiter.tryAcquire(key, 30, 60 * 60_000L)) {
            throw new RateLimitExceededException("Too many uploads. Try again later.", limiter.retryAfterSeconds(key, 60 * 60_000L));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(courseId, title, description, durationSec, file));
    }

    @Operation(summary = "Videos of a course (customers also get their own progress)")
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<VideoView>> list(@PathVariable Long courseId) {
        return ResponseEntity.ok(service.list(courseId));
    }

    @Operation(summary = "Customers' progress on a course's videos (admin or owning instructor)")
    @GetMapping("/course/{courseId}/stats")
    public ResponseEntity<CourseStats> stats(@PathVariable Long courseId) {
        return ResponseEntity.ok(service.stats(courseId));
    }

    @Operation(summary = "My courses with video progress and where to resume (customer)")
    @GetMapping("/my-learning")
    public ResponseEntity<List<LearningItem>> myLearning() {
        return ResponseEntity.ok(service.myLearning());
    }

    @Operation(summary = "Get a short-lived address for the video player")
    @GetMapping("/{videoId}/ticket")
    public ResponseEntity<TicketView> ticket(@PathVariable Long videoId) {
        return ResponseEntity.ok(service.ticket(videoId));
    }

    @Operation(summary = "Save how far I have watched (customer)")
    @PutMapping("/{videoId}/progress")
    public ResponseEntity<ProgressView> progress(@PathVariable Long videoId, @RequestBody ProgressRequest body) {
        return ResponseEntity.ok(service.saveProgress(videoId, body));
    }

    @Operation(summary = "Delete a video (admin or owning instructor)")
    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> delete(@PathVariable Long videoId) {
        service.delete(videoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * The video bytes. Open to the player without a login header, but only with a valid ticket for this exact video.
     * Supports the Range header so the viewer can jump around without downloading everything first.
     */
    @Operation(summary = "Stream a video (needs a ticket from the ticket endpoint)")
    @GetMapping("/stream/{videoId}")
    public void stream(@PathVariable Long videoId, @RequestParam(value = "t", required = false) String ticket,
                       HttpServletRequest request, HttpServletResponse response) throws IOException {
        CourseVideo v = service.videoForStream(videoId, ticket);
        Path file = storage.resolve(v.getStoredName());
        if (file == null) {
            response.setStatus(404);
            return;
        }
        try {
            VideoStreamer.write(file, v.getContentType(), request.getHeader("Range"), response);
        } catch (RangeParser.NotSatisfiableException e) {
            response.setStatus(416);
            response.setHeader("Content-Range", "bytes */" + file.toFile().length());
        }
    }
}
