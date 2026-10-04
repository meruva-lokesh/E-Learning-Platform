package com.examly.springapp.service;

import com.examly.springapp.model.Course;
import com.examly.springapp.repository.CourseRepo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AI course search: Gemini embeddings with a lexical fallback.
 *
 * @author Sumit
 */
@Service
public class CourseAiService {
    private static final Logger log = LoggerFactory.getLogger(CourseAiService.class);
    private static final int MIN_RESULTS = 3;

    private record CacheEntry(int hash, float[] vector) {}
    private record Scored(Course course, double score) {}

    private final CourseRepo courseRepo;
    private final GeminiService gemini;
    private final double threshold;
    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    // recent query embeddings, so repeating a search costs no Gemini call
    private final Map<String, float[]> queryCache = Collections.synchronizedMap(
            new LinkedHashMap<String, float[]>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, float[]> eldest) {
                    return size() > 200;
                }
            });

    // circuit breaker: after 3 Gemini failures in a row, skip Gemini for a minute
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile long pausedUntil = 0;

    public CourseAiService(CourseRepo courseRepo, GeminiService gemini,
                           @Value("${course.ai.similarity-threshold:0.55}") double threshold) {
        this.courseRepo = courseRepo;
        this.gemini = gemini;
        this.threshold = threshold;
    }

    public List<Course> searchCourses(String query) {
        List<Course> courses = courseRepo.findAll();
        if (query == null || query.isBlank() || courses.isEmpty()) {
            return courses;
        }
        if (gemini.isEnabled() && System.currentTimeMillis() >= pausedUntil) {
            try {
                float[] queryVec = queryVector(query);
                if (queryVec != null) {
                    List<Course> ranked = semanticRank(courses, queryVec);
                    consecutiveFailures.set(0);
                    return ranked;
                }
            } catch (RuntimeException e) {
                log.warn("Gemini search failed, using lexical fallback: {}", e.getMessage());
                if (consecutiveFailures.incrementAndGet() >= 3) {
                    pausedUntil = System.currentTimeMillis() + 60_000L;
                    consecutiveFailures.set(0);
                    log.warn("Gemini paused for 60 seconds after repeated failures");
                }
            }
        }
        return lexicalRank(courses, query);
    }

    private float[] queryVector(String query) {
        String key = query.trim().toLowerCase();
        float[] cached = queryCache.get(key);
        if (cached != null) return cached;
        float[] vec = gemini.embed(query);
        if (vec != null) queryCache.put(key, vec);
        return vec;
    }

    private List<Course> semanticRank(List<Course> courses, float[] queryVec) {
        List<Scored> scored = new ArrayList<>();
        for (Course c : courses) {
            float[] vec = courseVector(c);
            scored.add(new Scored(c, vec == null ? 0 : cosine(queryVec, vec)));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        List<Course> hits = scored.stream().filter(s -> s.score() >= threshold).map(Scored::course).toList();
        if (!hits.isEmpty()) return hits;
        // nothing cleared the threshold: still show the closest matches rather than an empty page
        return scored.stream().limit(MIN_RESULTS).map(Scored::course).toList();
    }

    private float[] courseVector(Course c) {
        String profile = profileText(c);
        int hash = profile.hashCode();
        CacheEntry cached = cache.get(c.getCourseId());
        if (cached != null && cached.hash() == hash) return cached.vector();
        float[] vec = gemini.embed(profile);
        if (vec != null) cache.put(c.getCourseId(), new CacheEntry(hash, vec));
        return vec;
    }

    private List<Course> lexicalRank(List<Course> courses, String query) {
        Set<String> q = tokens(query);
        return courses.stream()
                .map(c -> new Scored(c, jaccard(q, tokens(profileText(c)))))
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .map(Scored::course)
                .toList();
    }

    static String profileText(Course c) {
        return "Course type: " + nullToEmpty(c.getCourseType())
                + ". Details: " + nullToEmpty(c.getCourseDetails())
                + ". Price: " + (c.getCoursePrice() == null ? "" : c.getCoursePrice());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    static Set<String> tokens(String text) {
        Set<String> out = new HashSet<>();
        for (String t : text.toLowerCase().split("[^a-z0-9+#]+")) {
            if (!t.isBlank()) out.add(t);
        }
        return out;
    }

    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) inter.size() / union.size();
    }

    static double cosine(float[] a, float[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
