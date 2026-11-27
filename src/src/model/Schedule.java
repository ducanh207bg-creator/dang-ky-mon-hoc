package model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class Schedule {
    private final DayOfWeek day;
    private final LocalTime start;
    private final LocalTime end;

    public Schedule(DayOfWeek day, LocalTime start, LocalTime end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
        this.day = day;
        this.start = start;
        this.end = end;
    }

    public boolean overlaps(Schedule other) {
        // Hai tiết liền nhau có thể dùng chung mốc kết thúc và bắt đầu.
        return day == other.day && start.isBefore(other.end) && other.start.isBefore(end);
    }

    @Override
    public String toString() {
        String weekday = day == DayOfWeek.SUNDAY ? "Chủ nhật" : "Thứ " + (day.getValue() + 1);
        return weekday + " " + start + "–" + end;
    }
}
