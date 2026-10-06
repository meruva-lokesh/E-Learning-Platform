package com.examly.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One finished AI quiz: who took it, on which course and the score. Only the result is stored,
 * never the questions.
 */
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attemptId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(length = 20)
    private String difficulty;

    private int score;
    private int total;
    private LocalDateTime takenAt;

    public QuizAttempt() {}

    public QuizAttempt(Customer customer, Course course, String difficulty, int score, int total) {
        this.customer = customer;
        this.course = course;
        this.difficulty = difficulty;
        this.score = score;
        this.total = total;
        this.takenAt = LocalDateTime.now();
    }

    public Long getAttemptId() { return attemptId; }
    public Customer getCustomer() { return customer; }
    public Course getCourse() { return course; }
    public String getDifficulty() { return difficulty; }
    public int getScore() { return score; }
    public int getTotal() { return total; }
    public LocalDateTime getTakenAt() { return takenAt; }
}
