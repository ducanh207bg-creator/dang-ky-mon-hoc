package service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import model.Course;
import model.Student;

public class RegistrationService {
    private final Map<String, Course> courses = new LinkedHashMap<>();
    private final Map<String, Set<Course>> enrollments = new LinkedHashMap<>();

    private final List<CourseValidator> validators = java.util.Arrays.asList(
        (service, student, course) -> {
            if (service.enrolledCount(course) >= course.getCapacity()) {
                throw new RegistrationException("Môn học đã đủ số lượng.");
            }
        },
        (service, student, course) -> {
            if (service.totalCredits(student) + course.getCredits() > student.getMaxCredits()) {
                throw new RegistrationException("Vượt giới hạn tín chỉ.");
            }
        },
        (service, student, course) -> {
            for (Course existing : service.registeredCourses(student)) {
                if (course.getSchedule().overlaps(existing.getSchedule())) {
                    throw new RegistrationException("Lịch học trùng với môn " + existing.getId() + ".");
                }
            }
        }
    );

    public RegistrationService(List<Course> catalog) {
        for (Course course : catalog) {
            courses.put(normalize(course.getId()), course);
        }
    }

    public List<Course> search(String query) {
        String keyword = normalize(query);
        List<Course> matches = new ArrayList<>();
        for (Course course : courses.values()) {
            if (normalize(course.getId()).contains(keyword) || normalize(course.getName()).contains(keyword)) {
                matches.add(course);
            }
        }
        return matches;
    }

    public Collection<Course> registeredCourses(Student student) {
        return new ArrayList<>(enrollments.getOrDefault(student.getId(), Collections.emptySet()));
    }

    public int totalCredits(Student student) {
        int total = 0;
        for (Course course : registeredCourses(student)) {
            total += course.getCredits();
        }
        return total;
    }

    public int enrolledCount(Course course) {
        // Tính sĩ số từ danh sách đăng ký để tránh lệch khi hủy môn.
        int count = 0;
        for (Set<Course> registered : enrollments.values()) {
            if (registered.contains(course)) {
                count++;
            }
        }
        return count;
    }

    public void register(Student student, String courseId) throws RegistrationException {
        Course course = requireCourse(courseId);
        Collection<Course> registered = registeredCourses(student);
        if (registered.contains(course)) {
            throw new RegistrationException("Bạn đã đăng ký môn này.");
        }
        for (CourseValidator validator : validators) {
            validator.validate(this, student, course);
        }
        restoreRegistration(student.getId(), course.getId());
    }

    // Khôi phục bản ghi đã lưu, không áp dụng lại quy tắc đăng ký mới.
    public void restoreRegistration(String studentId, String courseId) throws RegistrationException {
        Course course = requireCourse(courseId);
        enrollments.computeIfAbsent(studentId, id -> new LinkedHashSet<>()).add(course);
    }

    public void drop(Student student, String courseId) throws RegistrationException {
        Course course = requireCourse(courseId);
        Set<Course> registered = enrollments.get(student.getId());
        if (registered == null || !registered.remove(course)) {
            throw new RegistrationException("Bạn chưa đăng ký môn này.");
        }
    }

    private Course requireCourse(String courseId) throws RegistrationException {
        Course course = courses.get(normalize(courseId));
        if (course == null) {
            throw new RegistrationException("Không tìm thấy môn học.");
        }
        return course;
    }

    private static String normalize(String input) {
        return input.trim().toLowerCase(Locale.ROOT);
    }
}
