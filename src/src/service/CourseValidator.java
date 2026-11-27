package service;

import model.Course;
import model.Student;

@FunctionalInterface
public interface CourseValidator {
    void validate(RegistrationService service, Student student, Course course) throws RegistrationException;
}
