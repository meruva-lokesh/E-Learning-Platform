package com.examly.springapp.repository;

import com.examly.springapp.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Data access for courses.
 *
 * @author Sivamuthu
 * @author Amogh (search query)
 */
@Repository
public interface CourseRepo extends JpaRepository<Course, Long> {

        /**
         * Case-insensitive keyword search on courseType OR courseDetails within a price
         * range, with paging and sorting.
         * The caller passes an already lower-cased LIKE pattern that uses '!' as the
         * escape character
         * (see CourseServiceImpl.likePattern), so user input can never act as a
         * wildcard.
         *
         * @author Amogh
         */
        @Query("SELECT c FROM Course c WHERE "
                        + "(LOWER(c.courseType) LIKE :pattern ESCAPE '!' OR LOWER(c.courseDetails) LIKE :pattern ESCAPE '!') "
                        + "AND c.coursePrice >= :minPrice AND c.coursePrice <= :maxPrice")
        Page<Course> search(@Param("pattern") String pattern,
                        @Param("minPrice") Double minPrice,
                        @Param("maxPrice") Double maxPrice,
                        Pageable pageable);

        /** True when some course still has this type (ignoring case). */
        boolean existsByCourseTypeIgnoreCase(String courseType);
}
