package model;

public class Course {
    private final String id;
    private final String name;
    private final int credits;
    private final String lecturer;
    private final int capacity;
    private final Schedule schedule;

    public Course(String id, String name, int credits, String lecturer, int capacity, Schedule schedule) {
        this.id = id;
        this.name = name;
        this.credits = credits;
        this.lecturer = lecturer;
        this.capacity = capacity;
        this.schedule = schedule;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getCredits() { return credits; }
    public String getLecturer() { return lecturer; }
    public int getCapacity() { return capacity; }
    public Schedule getSchedule() { return schedule; }
}
