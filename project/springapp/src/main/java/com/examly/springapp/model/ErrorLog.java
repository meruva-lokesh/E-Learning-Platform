package com.examly.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One row of the ErrorLogs table, written by GlobalExceptionHandler for every handled error.
 *
 * @author Team Lead
 */
@Entity
@Table(name = "ErrorLogs")
public class ErrorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int status;
    private String exceptionType;

    @Column(length = 2000)
    private String message;

    private String path;
    private LocalDateTime loggedAt;

    /** No-arg constructor required by JPA. */
    public ErrorLog() {}

    /** Convenience constructor used by the exception handler: no id, logged "now". Chains to the all-fields one. */
    public ErrorLog(int status, String exceptionType, String message, String path) {
        this(null, status, exceptionType, message, path, LocalDateTime.now());
    }

    /** All-fields constructor; chains to the no-arg constructor. Long messages are cut to fit the column. */
    public ErrorLog(Long id, int status, String exceptionType, String message, String path, LocalDateTime loggedAt) {
        this();
        this.id = id;
        this.status = status;
        this.exceptionType = exceptionType;
        this.message = message != null && message.length() > 1900 ? message.substring(0, 1900) : message;
        this.path = path;
        this.loggedAt = loggedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
}
