package model;

public class Student extends User {
    private final String className;
    private final String major;
    private final int maxCredits;

    public Student(String id, String name, String className, String major, int maxCredits) {
        super(id, name);
        this.className = className;
        this.major = major;
        this.maxCredits = maxCredits;
    }

    public String getClassName() { return className; }
    public String getMajor() { return major; }
    public int getMaxCredits() { return maxCredits; }
}
