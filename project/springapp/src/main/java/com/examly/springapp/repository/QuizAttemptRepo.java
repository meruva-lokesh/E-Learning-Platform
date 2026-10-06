package com.examly.springapp.repository;

import com.examly.springapp.model.QuizAttempt;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Data access for finished AI quizzes. */
@Repository
public interface QuizAttemptRepo extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findTop20ByCustomer_CustomerIdOrderByTakenAtDesc(Long customerId);
}
