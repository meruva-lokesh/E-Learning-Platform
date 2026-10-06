package com.examly.springapp.repository;

import com.examly.springapp.model.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for the ErrorLogs table.
 *
 * @author Team Lead
 */
@Repository
public interface ErrorLogRepo extends JpaRepository<ErrorLog, Long> {
}
