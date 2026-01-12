import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive Student Management System
 * Manages students, courses, enrollments, grades, and academic records
 */
public class StudentManagementSystem {
    
    // Constants
    private static final double MIN_GPA = 0.0;
    private static final double MAX_GPA = 4.0;
    private static final int MAX_ENROLLMENTS = 8;
    private static final double PASSING_GRADE = 60.0;
    
    // Data structures
    private Map<String, Student> students;
    private Map<String, Course> courses;
    private Map<String, Teacher> teachers;
    private Map<String, Department> departments;
    private List<Enrollment> enrollments;
    private List<Grade> grades;
    private Map<String, Semester> semesters;
    
    // Statistics
    private int totalStudents;
    private int totalCourses;
    private int activeEnrollments;
    
    public StudentManagementSystem() {
        this.students = new HashMap<>();
        this.courses = new HashMap<>();
        this.teachers = new HashMap<>();
        this.departments = new HashMap<>();
        this.enrollments = new ArrayList<>();
        this.grades = new ArrayList<>();
        this.semesters = new HashMap<>();
        this.totalStudents = 0;
        this.totalCourses = 0;
        this.activeEnrollments = 0;
        initializeSystem();
    }
    
    private void initializeSystem() {
        // Create departments
        Department csDept = new Department("DEPT001", "Computer Science", "CS");
        Department mathDept = new Department("DEPT002", "Mathematics", "MATH");
        Department engDept = new Department("DEPT003", "Engineering", "ENG");
        
        departments.put("DEPT001", csDept);
        departments.put("DEPT002", mathDept);
        departments.put("DEPT003", engDept);
        
        // Create teachers
        Teacher teacher1 = new Teacher("T001", "Dr. Smith", "DEPT001", "Associate Professor", 85000.0);
        Teacher teacher2 = new Teacher("T002", "Dr. Johnson", "DEPT002", "Professor", 95000.0);
        Teacher teacher3 = new Teacher("T003", "Dr. Williams", "DEPT001", "Assistant Professor", 70000.0);
        
        teachers.put("T001", teacher1);
        teachers.put("T002", teacher2);
        teachers.put("T003", teacher3);
        
        // Create courses
        Course course1 = new Course("CS101", "Introduction to Programming", "DEPT001", 3, "T001");
        Course course2 = new Course("CS201", "Data Structures", "DEPT001", 4, "T001");
        Course course3 = new Course("MATH101", "Calculus I", "DEPT002", 4, "T002");
        Course course4 = new Course("CS301", "Algorithms", "DEPT001", 4, "T003");
        Course course5 = new Course("ENG101", "Engineering Fundamentals", "DEPT003", 3, "T003");
        
        courses.put("CS101", course1);
        courses.put("CS201", course2);
        courses.put("MATH101", course3);
        courses.put("CS301", course4);
        courses.put("ENG101", course5);
        totalCourses = courses.size();
        
        // Create students
        Student student1 = new Student("S001", "John Doe", "DEPT001", LocalDate.of(2020, 9, 1));
        Student student2 = new Student("S002", "Jane Smith", "DEPT001", LocalDate.of(2021, 9, 1));
        Student student3 = new Student("S003", "Bob Johnson", "DEPT002", LocalDate.of(2020, 9, 1));
        
        students.put("S001", student1);
        students.put("S002", student2);
        students.put("S003", student3);
        totalStudents = students.size();
        
        // Create semesters
        Semester fall2023 = new Semester("FALL2023", "Fall 2023", LocalDate.of(2023, 9, 1), LocalDate.of(2023, 12, 15));
        Semester spring2024 = new Semester("SPRING2024", "Spring 2024", LocalDate.of(2024, 1, 15), LocalDate.of(2024, 5, 1));
        semesters.put("FALL2023", fall2023);
        semesters.put("SPRING2024", spring2024);
    }
    
    public boolean addStudent(String studentId, String name, String departmentId, LocalDate enrollmentDate) {
        if (students.containsKey(studentId)) {
            System.out.println("Student ID already exists: " + studentId);
            return false;
        }
        
        if (!departments.containsKey(departmentId)) {
            System.out.println("Department not found: " + departmentId);
            return false;
        }
        
        Student student = new Student(studentId, name, departmentId, enrollmentDate);
        students.put(studentId, student);
        totalStudents++;
        System.out.println("Student added: " + studentId);
        return true;
    }
    
    public boolean addCourse(String courseId, String name, String departmentId, int credits, String teacherId) {
        if (courses.containsKey(courseId)) {
            System.out.println("Course ID already exists: " + courseId);
            return false;
        }
        
        if (!departments.containsKey(departmentId)) {
            System.out.println("Department not found: " + departmentId);
            return false;
        }
        
        if (!teachers.containsKey(teacherId)) {
            System.out.println("Teacher not found: " + teacherId);
            return false;
        }
        
        if (credits <= 0 || credits > 6) {
            System.out.println("Invalid number of credits (1-6)");
            return false;
        }
        
        Course course = new Course(courseId, name, departmentId, credits, teacherId);
        courses.put(courseId, course);
        totalCourses++;
        System.out.println("Course added: " + courseId);
        return true;
    }
    
    public Enrollment enrollStudent(String studentId, String courseId, String semesterId) {
        if (!students.containsKey(studentId)) {
            System.out.println("Student not found: " + studentId);
            return null;
        }
        
        if (!courses.containsKey(courseId)) {
            System.out.println("Course not found: " + courseId);
            return null;
        }
        
        if (!semesters.containsKey(semesterId)) {
            System.out.println("Semester not found: " + semesterId);
            return null;
        }
        
        Student student = students.get(studentId);
        Course course = courses.get(courseId);
        
        // Check if already enrolled
        for (Enrollment enrollment : enrollments) {
            if (enrollment.getStudentId().equals(studentId) && 
                enrollment.getCourseId().equals(courseId) && 
                enrollment.getSemesterId().equals(semesterId)) {
                System.out.println("Student already enrolled in this course for this semester");
                return null;
            }
        }
        
        // Check enrollment limit
        int currentEnrollments = countEnrollmentsForStudent(studentId, semesterId);
        if (currentEnrollments >= MAX_ENROLLMENTS) {
            System.out.println("Maximum enrollments reached for this semester");
            return null;
        }
        
        // Check prerequisites
        if (!checkPrerequisites(studentId, courseId)) {
            System.out.println("Prerequisites not met for course: " + courseId);
            return null;
        }
        
        String enrollmentId = generateEnrollmentId();
        Enrollment enrollment = new Enrollment(enrollmentId, studentId, courseId, semesterId, LocalDate.now(), "ACTIVE");
        enrollments.add(enrollment);
        activeEnrollments++;
        
        student.addEnrollment(enrollmentId);
        course.addEnrollment(enrollmentId);
        
        System.out.println("Student enrolled: " + studentId + " in " + courseId);
        return enrollment;
    }
    
    private boolean checkPrerequisites(String studentId, String courseId) {
        // Simple prerequisite checking
        Map<String, List<String>> prerequisites = new HashMap<>();
        prerequisites.put("CS201", Arrays.asList("CS101"));
        prerequisites.put("CS301", Arrays.asList("CS201"));
        
        if (!prerequisites.containsKey(courseId)) {
            return true; // No prerequisites
        }
        
        List<String> requiredCourses = prerequisites.get(courseId);
        for (String requiredCourse : requiredCourses) {
            if (!hasCompletedCourse(studentId, requiredCourse)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean hasCompletedCourse(String studentId, String courseId) {
        for (Grade grade : grades) {
            if (grade.getStudentId().equals(studentId) && 
                grade.getCourseId().equals(courseId) && 
                grade.getFinalGrade() >= PASSING_GRADE) {
                return true;
            }
        }
        return false;
    }
    
    public boolean addGrade(String studentId, String courseId, String semesterId, 
                           double midtermGrade, double finalGrade, double assignmentGrade) {
        if (!students.containsKey(studentId)) {
            System.out.println("Student not found: " + studentId);
            return false;
        }
        
        if (!courses.containsKey(courseId)) {
            System.out.println("Course not found: " + courseId);
            return false;
        }
        
        // Check if enrolled
        Enrollment enrollment = findEnrollment(studentId, courseId, semesterId);
        if (enrollment == null) {
            System.out.println("Student not enrolled in this course");
            return false;
        }
        
        if (midtermGrade < 0 || midtermGrade > 100 || 
            finalGrade < 0 || finalGrade > 100 || 
            assignmentGrade < 0 || assignmentGrade > 100) {
            System.out.println("Grades must be between 0 and 100");
            return false;
        }
        
        double finalGradeValue = calculateFinalGrade(midtermGrade, finalGrade, assignmentGrade);
        String letterGrade = calculateLetterGrade(finalGradeValue);
        double gradePoints = convertToGradePoints(letterGrade);
        
        String gradeId = generateGradeId();
        Grade grade = new Grade(gradeId, studentId, courseId, semesterId, 
                                midtermGrade, finalGrade, assignmentGrade, 
                                finalGradeValue, letterGrade, gradePoints, LocalDate.now());
        
        grades.add(grade);
        
        // Update student GPA
        Student student = students.get(studentId);
        student.updateGPA();
        
        // Check if course passed
        if (finalGradeValue >= PASSING_GRADE) {
            enrollment.setStatus("COMPLETED");
        } else {
            enrollment.setStatus("FAILED");
        }
        
        System.out.println("Grade added. Final Grade: " + finalGradeValue + " (" + letterGrade + ")");
        return true;
    }
    
    private double calculateFinalGrade(double midterm, double finalExam, double assignment) {
        // Weighted calculation: 30% midterm, 40% final, 30% assignment
        return (midterm * 0.30) + (finalExam * 0.40) + (assignment * 0.30);
    }
    
    private String calculateLetterGrade(double grade) {
        if (grade >= 90) return "A";
        if (grade >= 80) return "B";
        if (grade >= 70) return "C";
        if (grade >= 60) return "D";
        return "F";
    }
    
    private double convertToGradePoints(String letterGrade) {
        switch (letterGrade) {
            case "A": return 4.0;
            case "B": return 3.0;
            case "C": return 2.0;
            case "D": return 1.0;
            default: return 0.0;
        }
    }
    
    public void generateStudentTranscript(String studentId) {
        if (!students.containsKey(studentId)) {
            System.out.println("Student not found: " + studentId);
            return;
        }
        
        Student student = students.get(studentId);
        System.out.println("\n=== Student Transcript ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Name: " + student.getName());
        System.out.println("Department: " + departments.get(student.getDepartmentId()).getName());
        System.out.println("GPA: " + String.format("%.2f", student.getGpa()));
        System.out.println("Enrollment Date: " + student.getEnrollmentDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        System.out.println("\nCourses:");
        System.out.println("Course ID\tName\t\t\tSemester\tGrade\tCredits\tPoints");
        System.out.println("---------------------------------------------------------------");
        
        double totalPoints = 0.0;
        int totalCredits = 0;
        
        for (Grade grade : grades) {
            if (grade.getStudentId().equals(studentId)) {
                Course course = courses.get(grade.getCourseId());
                Semester semester = semesters.get(grade.getSemesterId());
                double points = grade.getGradePoints() * course.getCredits();
                totalPoints += points;
                totalCredits += course.getCredits();
                
                System.out.println(
                    grade.getCourseId() + "\t\t" +
                    course.getName() + "\t\t" +
                    semester.getName() + "\t" +
                    grade.getLetterGrade() + "\t" +
                    course.getCredits() + "\t" +
                    String.format("%.2f", points)
                );
            }
        }
        
        if (totalCredits > 0) {
            double calculatedGPA = totalPoints / totalCredits;
            System.out.println("\nTotal Credits: " + totalCredits);
            System.out.println("Total Points: " + String.format("%.2f", totalPoints));
            System.out.println("Calculated GPA: " + String.format("%.2f", calculatedGPA));
        }
        
        System.out.println("=== End of Transcript ===\n");
    }
    
    public void generateCourseReport(String courseId) {
        if (!courses.containsKey(courseId)) {
            System.out.println("Course not found: " + courseId);
            return;
        }
        
        Course course = courses.get(courseId);
        System.out.println("\n=== Course Report ===");
        System.out.println("Course ID: " + courseId);
        System.out.println("Course Name: " + course.getName());
        System.out.println("Department: " + departments.get(course.getDepartmentId()).getName());
        System.out.println("Credits: " + course.getCredits());
        System.out.println("Teacher: " + teachers.get(course.getTeacherId()).getName());
        System.out.println("\nEnrollments: " + course.getEnrollmentCount());
        System.out.println("\nStudent Grades:");
        System.out.println("Student ID\tName\t\tFinal Grade\tLetter Grade");
        System.out.println("---------------------------------------------------");
        
        int passCount = 0;
        int failCount = 0;
        double totalGrade = 0.0;
        int gradeCount = 0;
        
        for (Grade grade : grades) {
            if (grade.getCourseId().equals(courseId)) {
                Student student = students.get(grade.getStudentId());
                System.out.println(
                    grade.getStudentId() + "\t\t" +
                    student.getName() + "\t\t" +
                    String.format("%.2f", grade.getFinalGrade()) + "\t\t" +
                    grade.getLetterGrade()
                );
                
                totalGrade += grade.getFinalGrade();
                gradeCount++;
                if (grade.getFinalGrade() >= PASSING_GRADE) {
                    passCount++;
                } else {
                    failCount++;
                }
            }
        }
        
        if (gradeCount > 0) {
            System.out.println("\nAverage Grade: " + String.format("%.2f", totalGrade / gradeCount));
            System.out.println("Passed: " + passCount);
            System.out.println("Failed: " + failCount);
            System.out.println("Pass Rate: " + String.format("%.2f", (passCount * 100.0 / gradeCount)) + "%");
        }
        
        System.out.println("=== End of Report ===\n");
    }
    
    public List<Student> getStudentsByDepartment(String departmentId) {
        List<Student> departmentStudents = new ArrayList<>();
        for (Student student : students.values()) {
            if (student.getDepartmentId().equals(departmentId)) {
                departmentStudents.add(student);
            }
        }
        return departmentStudents;
    }
    
    public List<Course> getCoursesByDepartment(String departmentId) {
        List<Course> departmentCourses = new ArrayList<>();
        for (Course course : courses.values()) {
            if (course.getDepartmentId().equals(departmentId)) {
                departmentCourses.add(course);
            }
        }
        return departmentCourses;
    }
    
    public void generateAcademicStatistics() {
        System.out.println("\n=== Academic Statistics ===");
        System.out.println("Total Students: " + totalStudents);
        System.out.println("Total Courses: " + totalCourses);
        System.out.println("Active Enrollments: " + activeEnrollments);
        System.out.println("Total Grades Recorded: " + grades.size());
        
        // Calculate average GPA
        double totalGPA = 0.0;
        int studentsWithGPA = 0;
        for (Student student : students.values()) {
            if (student.getGpa() > 0) {
                totalGPA += student.getGpa();
                studentsWithGPA++;
            }
        }
        
        if (studentsWithGPA > 0) {
            System.out.println("Average GPA: " + String.format("%.2f", totalGPA / studentsWithGPA));
        }
        
        System.out.println("\nStudents by Department:");
        for (Department department : departments.values()) {
            int count = getStudentsByDepartment(department.getDepartmentId()).size();
            System.out.println("  " + department.getName() + ": " + count + " students");
        }
        
        System.out.println("\nCourses by Department:");
        for (Department department : departments.values()) {
            int count = getCoursesByDepartment(department.getDepartmentId()).size();
            System.out.println("  " + department.getName() + ": " + count + " courses");
        }
        
        System.out.println("=== End of Statistics ===\n");
    }
    
    private int countEnrollmentsForStudent(String studentId, String semesterId) {
        int count = 0;
        for (Enrollment enrollment : enrollments) {
            if (enrollment.getStudentId().equals(studentId) && 
                enrollment.getSemesterId().equals(semesterId) &&
                enrollment.getStatus().equals("ACTIVE")) {
                count++;
            }
        }
        return count;
    }
    
    private Enrollment findEnrollment(String studentId, String courseId, String semesterId) {
        for (Enrollment enrollment : enrollments) {
            if (enrollment.getStudentId().equals(studentId) && 
                enrollment.getCourseId().equals(courseId) && 
                enrollment.getSemesterId().equals(semesterId)) {
                return enrollment;
            }
        }
        return null;
    }
    
    private String generateEnrollmentId() {
        return "ENR" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generateGradeId() {
        return "GRD" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    // Nested classes
    class Student {
        private String studentId;
        private String name;
        private String departmentId;
        private LocalDate enrollmentDate;
        private double gpa;
        private List<String> enrollmentIds;
        
        public Student(String studentId, String name, String departmentId, LocalDate enrollmentDate) {
            this.studentId = studentId;
            this.name = name;
            this.departmentId = departmentId;
            this.enrollmentDate = enrollmentDate;
            this.gpa = 0.0;
            this.enrollmentIds = new ArrayList<>();
        }
        
        public void addEnrollment(String enrollmentId) {
            enrollmentIds.add(enrollmentId);
        }
        
        public void updateGPA() {
            // GPA calculation would require access to grades
            // This is a simplified version
            double totalPoints = 0.0;
            int totalCredits = 0;
            
            for (Grade grade : grades) {
                if (grade.getStudentId().equals(this.studentId)) {
                    Course course = courses.get(grade.getCourseId());
                    totalPoints += grade.getGradePoints() * course.getCredits();
                    totalCredits += course.getCredits();
                }
            }
            
            if (totalCredits > 0) {
                this.gpa = totalPoints / totalCredits;
            }
        }
        
        public String getStudentId() { return studentId; }
        public String getName() { return name; }
        public String getDepartmentId() { return departmentId; }
        public LocalDate getEnrollmentDate() { return enrollmentDate; }
        public double getGpa() { return gpa; }
    }
    
    class Course {
        private String courseId;
        private String name;
        private String departmentId;
        private int credits;
        private String teacherId;
        private List<String> enrollmentIds;
        
        public Course(String courseId, String name, String departmentId, int credits, String teacherId) {
            this.courseId = courseId;
            this.name = name;
            this.departmentId = departmentId;
            this.credits = credits;
            this.teacherId = teacherId;
            this.enrollmentIds = new ArrayList<>();
        }
        
        public void addEnrollment(String enrollmentId) {
            enrollmentIds.add(enrollmentId);
        }
        
        public String getCourseId() { return courseId; }
        public String getName() { return name; }
        public String getDepartmentId() { return departmentId; }
        public int getCredits() { return credits; }
        public String getTeacherId() { return teacherId; }
        public int getEnrollmentCount() { return enrollmentIds.size(); }
    }
    
    class Teacher {
        private String teacherId;
        private String name;
        private String departmentId;
        private String rank;
        private double salary;
        
        public Teacher(String teacherId, String name, String departmentId, String rank, double salary) {
            this.teacherId = teacherId;
            this.name = name;
            this.departmentId = departmentId;
            this.rank = rank;
            this.salary = salary;
        }
        
        public String getTeacherId() { return teacherId; }
        public String getName() { return name; }
        public String getDepartmentId() { return departmentId; }
        public String getRank() { return rank; }
        public double getSalary() { return salary; }
    }
    
    class Department {
        private String departmentId;
        private String name;
        private String code;
        
        public Department(String departmentId, String name, String code) {
            this.departmentId = departmentId;
            this.name = name;
            this.code = code;
        }
        
        public String getDepartmentId() { return departmentId; }
        public String getName() { return name; }
        public String getCode() { return code; }
    }
    
    class Enrollment {
        private String enrollmentId;
        private String studentId;
        private String courseId;
        private String semesterId;
        private LocalDate enrollmentDate;
        private String status;
        
        public Enrollment(String enrollmentId, String studentId, String courseId, 
                         String semesterId, LocalDate enrollmentDate, String status) {
            this.enrollmentId = enrollmentId;
            this.studentId = studentId;
            this.courseId = courseId;
            this.semesterId = semesterId;
            this.enrollmentDate = enrollmentDate;
            this.status = status;
        }
        
        public String getEnrollmentId() { return enrollmentId; }
        public String getStudentId() { return studentId; }
        public String getCourseId() { return courseId; }
        public String getSemesterId() { return semesterId; }
        public LocalDate getEnrollmentDate() { return enrollmentDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    class Grade {
        private String gradeId;
        private String studentId;
        private String courseId;
        private String semesterId;
        private double midtermGrade;
        private double finalGrade;
        private double assignmentGrade;
        private double finalGradeValue;
        private String letterGrade;
        private double gradePoints;
        private LocalDate gradeDate;
        
        public Grade(String gradeId, String studentId, String courseId, String semesterId,
                    double midtermGrade, double finalGrade, double assignmentGrade,
                    double finalGradeValue, String letterGrade, double gradePoints, LocalDate gradeDate) {
            this.gradeId = gradeId;
            this.studentId = studentId;
            this.courseId = courseId;
            this.semesterId = semesterId;
            this.midtermGrade = midtermGrade;
            this.finalGrade = finalGrade;
            this.assignmentGrade = assignmentGrade;
            this.finalGradeValue = finalGradeValue;
            this.letterGrade = letterGrade;
            this.gradePoints = gradePoints;
            this.gradeDate = gradeDate;
        }
        
        public String getGradeId() { return gradeId; }
        public String getStudentId() { return studentId; }
        public String getCourseId() { return courseId; }
        public String getSemesterId() { return semesterId; }
        public double getMidtermGrade() { return midtermGrade; }
        public double getFinalGrade() { return finalGrade; }
        public double getAssignmentGrade() { return assignmentGrade; }
        public double getFinalGradeValue() { return finalGradeValue; }
        public String getLetterGrade() { return letterGrade; }
        public double getGradePoints() { return gradePoints; }
        public LocalDate getGradeDate() { return gradeDate; }
    }
    
    class Semester {
        private String semesterId;
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        
        public Semester(String semesterId, String name, LocalDate startDate, LocalDate endDate) {
            this.semesterId = semesterId;
            this.name = name;
            this.startDate = startDate;
            this.endDate = endDate;
        }
        
        public String getSemesterId() { return semesterId; }
        public String getName() { return name; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
    }
    
    public static void main(String[] args) {
        StudentManagementSystem sms = new StudentManagementSystem();
        
        // Test operations
        sms.enrollStudent("S001", "CS101", "FALL2023");
        sms.enrollStudent("S001", "MATH101", "FALL2023");
        sms.addGrade("S001", "CS101", "FALL2023", 85.0, 90.0, 88.0);
        sms.addGrade("S001", "MATH101", "FALL2023", 75.0, 80.0, 78.0);
        sms.generateStudentTranscript("S001");
        sms.generateCourseReport("CS101");
        sms.generateAcademicStatistics();
    }
}
