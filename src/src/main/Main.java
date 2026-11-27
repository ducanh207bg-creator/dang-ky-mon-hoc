package main;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;
import model.Course;
import model.Schedule;
import model.Student;
import service.RegistrationException;
import service.RegistrationService;

public class Main {
    public static void main(String[] args) throws RegistrationException {
        Student student = new Student("SV01", "Trần Văn Đức Anh", "CNTT01", "Công nghệ thông tin", 18);
        RegistrationService service = new RegistrationService(Arrays.asList(
                new Course("IT101", "Lập trình Java", 3, "Nguyễn An", 20,
                        new Schedule(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                new Course("IT102", "Cơ sở dữ liệu", 3, "Trần Bình", 20,
                        new Schedule(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))),
                new Course("MA101", "Toán rời rạc", 3, "Lê Chi", 20,
                        new Schedule(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                new Course("EN101", "Tiếng Anh", 2, "Phạm Dung", 1,
                        new Schedule(DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                new Course("IT103", "Mạng máy tính", 2, "Vũ Hà", 20,
                        new Schedule(DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                new Course("IT104", "Trí tuệ nhân tạo", 3, "Phạm Văn Dũng", 20,
                        new Schedule(DayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(15, 30))),
                new Course("IT105", "Cấu trúc dữ liệu và giải thuật", 3, "Hoàng Mai", 20,
                        new Schedule(DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))),
                new Course("IT106", "Hệ điều hành", 3, "Đỗ Quân", 20,
                        new Schedule(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(15, 0))),
                new Course("IT107", "Công nghệ phần mềm", 3, "Bùi Lan", 20,
                        new Schedule(DayOfWeek.TUESDAY, LocalTime.of(13, 0), LocalTime.of(15, 0))),
                new Course("MA102", "Xác suất thống kê", 3, "Ngô Hải", 20,
                        new Schedule(DayOfWeek.THURSDAY, LocalTime.of(13, 0), LocalTime.of(15, 0))),
                new Course("PH101", "Vật lý đại cương", 2, "Đặng Sơn", 20,
                        new Schedule(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)))
        ));
        Student otherStudent = new Student("SV02", "Nguyễn Văn Long", "CNTT01", "Công nghệ thông tin", 18);
        service.register(otherStudent, "EN101");

        System.out.println("Đăng ký môn học — " + student.getName() + " (" + student.getId() + ")");
        try (Scanner input = new Scanner(System.in, "UTF-8")) {
            runMenu(input, student, service);
        }
    }

    private static void runMenu(Scanner input, Student student, RegistrationService service) {
        while (true) {
            System.out.print("\n1. Xem môn học   2. Tìm môn học   3. Đăng ký\n"
                    + "4. Hủy đăng ký   5. Xem môn đã đăng ký   0. Thoát\nChọn: ");
            if (!input.hasNextLine()) {
                return;
            }
            String choice = input.nextLine().trim();
            try {
                switch (choice) {
                    case "1":
                        printCourses(service.search(""), service);
                        break;
                    case "2":
                        System.out.print("Nhập mã hoặc tên môn: ");
                        if (!input.hasNextLine()) {
                            return;
                        }
                        printCourses(service.search(input.nextLine()), service);
                        break;
                    case "3":
                    case "4":
                        System.out.print("Nhập mã môn: ");
                        if (!input.hasNextLine()) {
                            return;
                        }
                        String courseId = input.nextLine();
                        if (choice.equals("3")) {
                            service.register(student, courseId);
                            System.out.println("Đăng ký thành công.");
                        } else {
                            service.drop(student, courseId);
                            System.out.println("Hủy đăng ký thành công.");
                        }
                        break;
                    case "5":
                        printCourses(service.registeredCourses(student), service);
                        System.out.printf("Tổng tín chỉ: %d/%d%n",
                                service.totalCredits(student), student.getMaxCredits());
                        break;
                    case "0":
                        return;
                    default:
                        System.out.println("Lựa chọn không hợp lệ.");
                }
            } catch (RegistrationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void printCourses(Collection<Course> courses, RegistrationService service) {
        if (courses.isEmpty()) {
            System.out.println("Không có môn học để hiển thị.");
            return;
        }
        for (Course course : courses) {
            System.out.printf("%s | %s | %d tín chỉ | %s | %s | Sĩ số %d/%d%n",
                    course.getId(), course.getName(), course.getCredits(), course.getLecturer(),
                    course.getSchedule(), service.enrolledCount(course), course.getCapacity());
        }
    }
}
