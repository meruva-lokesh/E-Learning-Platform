package com.examly.springapp.service;

import com.examly.springapp.dto.VideoDtos.*;
import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.InvalidRequestException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Course;
import com.examly.springapp.model.CourseVideo;
import com.examly.springapp.model.Orders;
import com.examly.springapp.model.User;
import com.examly.springapp.model.VideoProgress;
import com.examly.springapp.repository.CourseRepo;
import com.examly.springapp.repository.CourseVideoRepo;
import com.examly.springapp.repository.OrderRepo;
import com.examly.springapp.repository.VideoProgressRepo;
import com.examly.springapp.video.VideoStorageService;
import com.examly.springapp.video.VideoTicketService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lesson videos: who may upload, who may watch, and how far each customer has watched.
 * <ul>
 *   <li>ADMIN: any course.</li>
 *   <li>INSTRUCTOR: only their own courses, and only once approved.</li>
 *   <li>CUSTOMER: only courses they bought; they also save their own progress.</li>
 * </ul>
 */
@Service
public class VideoService implements CourseDeleteListener {
    private static final Logger log = LoggerFactory.getLogger(VideoService.class);
    /** A video counts as finished when the viewer reached this share of it. */
    static final int COMPLETE_PERCENT = 90;
    private static final int MAX_SECONDS = 24 * 60 * 60;

    private final AccessService access;
    private final CourseRepo courses;
    private final CourseVideoRepo videos;
    private final VideoProgressRepo progressRepo;
    private final OrderRepo orders;
    private final InstructorService instructors;
    private final InstructorCourseService instructorCourses;
    private final VideoStorageService storage;
    private final VideoTicketService tickets;

    @Autowired
    public VideoService(AccessService access, CourseRepo courses, CourseVideoRepo videos, VideoProgressRepo progressRepo,
                        OrderRepo orders, InstructorService instructors, InstructorCourseService instructorCourses,
                        VideoStorageService storage, VideoTicketService tickets) {
        this.access = access;
        this.courses = courses;
        this.videos = videos;
        this.progressRepo = progressRepo;
        this.orders = orders;
        this.instructors = instructors;
        this.instructorCourses = instructorCourses;
        this.storage = storage;
        this.tickets = tickets;
    }

    // ------------------------------------------------------------------ access rules

    private void requireCourse(Long courseId) {
        if (courseId == null || courseId <= 0 || !DatabaseOperationException.guard("checking the course", () -> courses.existsById(courseId))) {
            throw new ResourceNotFoundException("Course not found");
        }
    }

    /** Throws unless the caller is the admin or the approved instructor who owns the course. */
    private User requireStaffFor(Long courseId) {
        requireCourse(courseId);
        User u = access.currentUser();
        if (access.isAdmin()) return u;
        User approved = instructors.requireApproved(access.currentEmail());
        if (!instructorCourses.owns(approved, courseId)) throw new AccessDeniedException("You can only manage videos of your own courses");
        return approved;
    }

    private boolean isStaff() {
        if (access.isAdmin()) return true;
        return "INSTRUCTOR".equals(access.currentUser().getRole());
    }

    private Set<Long> boughtCourseIds(Long customerId) {
        Set<Long> ids = new LinkedHashSet<>();
        List<Orders> list = DatabaseOperationException.guard("loading your enrollments", () -> orders.findByCustomer_CustomerId(customerId));
        for (Orders o : list) for (Course c : o.getCourses()) ids.add(c.getCourseId());
        return ids;
    }

    /** Allows staff who manage the course, or a customer who bought it. Returns the customer id (null for staff). */
    private Long requireViewer(Long courseId) {
        requireCourse(courseId);
        if (isStaff()) {
            requireStaffFor(courseId);
            return null;
        }
        Long customerId = access.currentCustomer().getCustomerId();
        if (!boughtCourseIds(customerId).contains(courseId)) throw new AccessDeniedException("Enroll in this course to watch its videos");
        return customerId;
    }

    private CourseVideo videoOrThrow(Long videoId) {
        if (videoId == null || videoId <= 0) throw new ResourceNotFoundException("Video not found");
        return DatabaseOperationException.guard("loading the video", () -> videos.findById(videoId))
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
    }

    // ------------------------------------------------------------------ staff: upload, delete

    public VideoView upload(Long courseId, String titleRaw, String descriptionRaw, Integer durationSec, MultipartFile file) {
        User u = requireStaffFor(courseId);
        String title = titleRaw == null ? "" : titleRaw.trim();
        if (title.length() < 2 || title.length() > 150) throw new InvalidRequestException("Title must be 2 to 150 characters");
        String description = descriptionRaw == null ? "" : descriptionRaw.trim();
        if (description.length() > 500) throw new InvalidRequestException("Description can have at most 500 characters");
        if (file == null || file.isEmpty()) throw new InvalidRequestException("Choose a video file to upload");
        int duration = durationSec == null ? 0 : Math.max(0, Math.min(MAX_SECONDS, durationSec));

        VideoStorageService.Stored stored;
        try {
            stored = storage.store(file.getInputStream(), file.getSize());
        } catch (java.io.IOException e) {
            log.error("Could not read the uploaded video", e);
            throw new InvalidRequestException("The upload could not be read. Please try again.");
        }
        String original = file.getOriginalFilename();
        if (original != null && original.length() > 200) original = original.substring(0, 200);
        CourseVideo v = new CourseVideo(courseId, title, description.isEmpty() ? null : description, stored.storedName, original,
                stored.contentType, stored.sizeBytes, duration, u.getUserId(), System.currentTimeMillis());
        try {
            return toView(DatabaseOperationException.guard("saving the video", () -> videos.save(v)), null);
        } catch (RuntimeException e) {
            storage.delete(stored.storedName); // do not leave a file with no row
            throw e;
        }
    }

    public void delete(Long videoId) {
        CourseVideo v = videoOrThrow(videoId);
        requireStaffFor(v.getCourseId());
        removeVideo(v);
    }

    private void removeVideo(CourseVideo v) {
        DatabaseOperationException.guardVoid("removing the watch progress", () -> progressRepo.deleteByVideoId(v.getId()));
        DatabaseOperationException.guardVoid("removing the video", () -> videos.delete(v));
        storage.delete(v.getStoredName());
    }

    /** Called when an instructor deletes a course: its videos and their progress go too. Never blocks the delete. */
    @Override
    public void onCourseDeleted(Long courseId) {
        try {
            for (CourseVideo v : videos.findByCourseIdOrderByIdAsc(courseId)) removeVideo(v);
        } catch (RuntimeException e) {
            log.warn("Could not remove all videos of course {}: {}", courseId, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ list, ticket, stream

    public List<VideoView> list(Long courseId) {
        Long customerId = requireViewer(courseId);
        List<CourseVideo> rows = DatabaseOperationException.guard("loading the videos", () -> videos.findByCourseIdOrderByIdAsc(courseId));
        Map<Long, VideoProgress> mine = new HashMap<>();
        if (customerId != null && !rows.isEmpty()) {
            List<Long> ids = rows.stream().map(CourseVideo::getId).toList();
            for (VideoProgress p : progressRepo.findByCustomerIdAndVideoIdIn(customerId, ids)) mine.put(p.getVideoId(), p);
        }
        List<VideoView> out = new ArrayList<>();
        for (CourseVideo v : rows) out.add(toView(v, mine.get(v.getId())));
        return out;
    }

    public TicketView ticket(Long videoId) {
        CourseVideo v = videoOrThrow(videoId);
        requireViewer(v.getCourseId());
        TicketView t = new TicketView();
        t.ticket = tickets.issue(v.getId(), access.currentUser().getUserId());
        t.url = "/api/video/stream/" + v.getId() + "?t=" + t.ticket;
        t.expiresInSeconds = (int) tickets.ttlSeconds();
        return t;
    }

    /** For the stream endpoint: no login header is possible there, so the ticket is the proof. */
    public CourseVideo videoForStream(Long videoId, String ticket) {
        if (!tickets.isValid(ticket, videoId)) throw new AccessDeniedException("This video link has expired. Open the lesson again.");
        return videoOrThrow(videoId);
    }

    // ------------------------------------------------------------------ progress

    public ProgressView saveProgress(Long videoId, ProgressRequest req) {
        if (req == null || req.positionSec == null) throw new InvalidRequestException("positionSec is required");
        CourseVideo v = videoOrThrow(videoId);
        Long customerId = requireCustomerViewer(v.getCourseId());
        int duration = v.getDurationSec() > 0 ? v.getDurationSec()
                : (req.durationSec == null ? 0 : Math.max(0, Math.min(MAX_SECONDS, req.durationSec)));
        int pos = Math.max(0, Math.min(req.positionSec, duration > 0 ? duration : MAX_SECONDS));

        VideoProgress p = progressRepo.findByCustomerIdAndVideoId(customerId, videoId).orElseGet(() -> new VideoProgress(customerId, videoId));
        p.setPositionSec(pos);
        p.setMaxPositionSec(Math.max(p.getMaxPositionSec(), pos));
        if (!p.isCompleted() && duration > 0 && (long) p.getMaxPositionSec() * 100 >= (long) duration * COMPLETE_PERCENT) p.setCompleted(true);
        p.setUpdatedAtMs(System.currentTimeMillis());
        VideoProgress saved = DatabaseOperationException.guard("saving your progress", () -> progressRepo.save(p));
        return toProgress(saved, duration);
    }

    private Long requireCustomerViewer(Long courseId) {
        Long customerId = requireViewer(courseId);
        if (customerId == null) throw new AccessDeniedException("Only customers have watch progress");
        return customerId;
    }

    // ------------------------------------------------------------------ staff: progress of the customers

    public CourseStats stats(Long courseId) {
        requireStaffFor(courseId);
        List<CourseVideo> rows = videos.findByCourseIdOrderByIdAsc(courseId);
        Set<Long> enrolled = new HashSet<>();
        for (Orders o : DatabaseOperationException.guard("loading the enrollments", () -> orders.findByCourses_CourseId(courseId))) {
            if (o.getCustomer() != null) enrolled.add(o.getCustomer().getCustomerId());
        }
        CourseStats s = new CourseStats();
        s.courseId = courseId;
        s.enrolledCustomers = enrolled.size();
        s.videos = new ArrayList<>();
        Map<Long, List<VideoProgress>> byVideo = new HashMap<>();
        if (!rows.isEmpty()) {
            for (VideoProgress p : progressRepo.findByVideoIdIn(rows.stream().map(CourseVideo::getId).toList())) {
                byVideo.computeIfAbsent(p.getVideoId(), k -> new ArrayList<>()).add(p);
            }
        }
        for (CourseVideo v : rows) {
            VideoStat st = new VideoStat();
            st.videoId = v.getId();
            st.title = v.getTitle();
            st.durationSec = v.getDurationSec();
            int viewers = 0, done = 0, sum = 0;
            for (VideoProgress p : byVideo.getOrDefault(v.getId(), List.of())) {
                if (p.getMaxPositionSec() <= 0 && !p.isCompleted()) continue;
                viewers++;
                if (p.isCompleted()) done++;
                sum += percentOf(p, v.getDurationSec());
            }
            st.viewers = viewers;
            st.completedCount = done;
            st.averagePercent = viewers == 0 ? 0 : sum / viewers;
            s.videos.add(st);
        }
        return s;
    }

    // ------------------------------------------------------------------ customer: my learning

    public List<LearningItem> myLearning() {
        Long customerId = access.currentCustomer().getCustomerId();
        Set<Long> courseIds = boughtCourseIds(customerId);
        List<LearningItem> out = new ArrayList<>();
        if (courseIds.isEmpty()) return out;
        List<CourseVideo> all = videos.findByCourseIdIn(new ArrayList<>(courseIds));
        if (all.isEmpty()) return out;
        Map<Long, VideoProgress> mine = new HashMap<>();
        for (VideoProgress p : progressRepo.findByCustomerIdAndVideoIdIn(customerId, all.stream().map(CourseVideo::getId).toList())) mine.put(p.getVideoId(), p);
        Map<Long, Course> byId = new HashMap<>();
        for (Course c : courses.findAllById(courseIds)) byId.put(c.getCourseId(), c);

        for (Long courseId : courseIds) {
            List<CourseVideo> list = all.stream().filter(v -> v.getCourseId().equals(courseId)).sorted(Comparator.comparing(CourseVideo::getId)).toList();
            if (list.isEmpty() || !byId.containsKey(courseId)) continue;
            LearningItem it = new LearningItem();
            it.courseId = courseId;
            it.courseType = byId.get(courseId).getCourseType();
            it.totalVideos = list.size();
            long newest = -1;
            for (CourseVideo v : list) {
                VideoProgress p = mine.get(v.getId());
                if (p != null && p.isCompleted()) { it.completedVideos++; continue; }
                if (p != null && p.getUpdatedAtMs() > newest) { newest = p.getUpdatedAtMs(); it.resumeVideoId = v.getId(); it.resumePositionSec = p.getPositionSec(); }
            }
            if (it.resumeVideoId == null) {
                for (CourseVideo v : list) {
                    VideoProgress p = mine.get(v.getId());
                    if (p == null || !p.isCompleted()) { it.resumeVideoId = v.getId(); it.resumePositionSec = 0; break; }
                }
            }
            it.percent = it.completedVideos * 100 / it.totalVideos;
            out.add(it);
        }
        return out;
    }

    // ------------------------------------------------------------------ mapping

    private static int percentOf(VideoProgress p, int duration) {
        if (p.isCompleted()) return 100;
        if (duration <= 0) return 0;
        return (int) Math.min(100, (long) p.getMaxPositionSec() * 100 / duration);
    }

    private static ProgressView toProgress(VideoProgress p, int duration) {
        ProgressView v = new ProgressView();
        v.positionSec = p.getPositionSec();
        v.completed = p.isCompleted();
        v.percent = percentOf(p, duration);
        return v;
    }

    private static VideoView toView(CourseVideo v, VideoProgress p) {
        VideoView out = new VideoView();
        out.id = v.getId();
        out.courseId = v.getCourseId();
        out.title = v.getTitle();
        out.description = v.getDescription();
        out.sizeBytes = v.getSizeBytes();
        out.durationSec = v.getDurationSec();
        out.uploadedAtMs = v.getUploadedAtMs();
        if (p != null) out.progress = toProgress(p, v.getDurationSec());
        return out;
    }
}
