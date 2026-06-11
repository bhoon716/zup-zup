package bhoon.sugang_helper.domain.course.enums;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import bhoon.sugang_helper.domain.course.repository.CourseRepository;
import java.util.stream.Collectors;

@SpringBootTest
class CourseClassificationTest {

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void debugDatabaseClassifications() {
        var allCourses = courseRepository.findAll();
        System.out.println("==================================================");
        System.out.println("TOTAL COURSES IN DB: " + allCourses.size());
        
        var classificationCounts = allCourses.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getClassification() != null ? c.getClassification().name() : "NULL",
                        Collectors.counting()
                ));
        
        classificationCounts.forEach((key, val) -> {
            System.out.println("CLASSIFICATION [" + key + "]: " + val);
        });

        // Also check some courses with MAJOR
        var majorCourses = allCourses.stream()
                .filter(c -> c.getClassification() == CourseClassification.MAJOR)
                .limit(10)
                .toList();
        System.out.println("MAJOR COURSES SAMPLES:");
        majorCourses.forEach(c -> {
            System.out.println(" - Key: " + c.getCourseKey() + ", Name: " + c.getName() + ", TargetGrade: " + c.getTargetGrade() + ", Dept: " + c.getDepartment());
        });
        System.out.println("==================================================");
    }
}
