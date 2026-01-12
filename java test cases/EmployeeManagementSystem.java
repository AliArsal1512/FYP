import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Comprehensive Employee Management System
 * Manages employees, departments, attendance, payroll, leaves, and performance
 */
public class EmployeeManagementSystem {
    
    // Constants
    private static final int MAX_LEAVE_DAYS = 30;
    private static final double WORKING_HOURS_PER_DAY = 8.0;
    private static final double OVERTIME_RATE = 1.5;
    private static final double BONUS_PERCENTAGE = 0.10;
    
    // Data structures
    private Map<String, Employee> employees;
    private Map<String, Department> departments;
    private Map<String, Position> positions;
    private List<Attendance> attendanceRecords;
    private List<LeaveRequest> leaveRequests;
    private List<Payroll> payrollRecords;
    private List<PerformanceReview> performanceReviews;
    private Map<String, Project> projects;
    private List<Task> tasks;
    
    // Statistics
    private int totalEmployees;
    private int totalDepartments;
    private double totalPayroll;
    private int activeProjects;
    
    public EmployeeManagementSystem() {
        this.employees = new HashMap<>();
        this.departments = new HashMap<>();
        this.positions = new HashMap<>();
        this.attendanceRecords = new ArrayList<>();
        this.leaveRequests = new ArrayList<>();
        this.payrollRecords = new ArrayList<>();
        this.performanceReviews = new ArrayList<>();
        this.projects = new HashMap<>();
        this.tasks = new ArrayList<>();
        this.totalEmployees = 0;
        this.totalDepartments = 0;
        this.totalPayroll = 0.0;
        this.activeProjects = 0;
        initializeSystem();
    }
    
    private void initializeSystem() {
        // Create departments
        Department it = new Department("DEPT001", "Information Technology", "IT Department", 50);
        Department hr = new Department("DEPT002", "Human Resources", "HR Department", 20);
        Department finance = new Department("DEPT003", "Finance", "Finance Department", 30);
        Department marketing = new Department("DEPT004", "Marketing", "Marketing Department", 25);
        Department operations = new Department("DEPT005", "Operations", "Operations Department", 40);
        
        departments.put("DEPT001", it);
        departments.put("DEPT002", hr);
        departments.put("DEPT003", finance);
        departments.put("DEPT004", marketing);
        departments.put("DEPT005", operations);
        totalDepartments = departments.size();
        
        // Create positions
        Position manager = new Position("POS001", "Manager", "Department Manager", 80000.0, 120000.0);
        Position seniorDev = new Position("POS002", "Senior Developer", "Senior Software Developer", 70000.0, 100000.0);
        Position developer = new Position("POS003", "Developer", "Software Developer", 50000.0, 80000.0);
        Position analyst = new Position("POS004", "Analyst", "Business Analyst", 55000.0, 75000.0);
        Position hrSpecialist = new Position("POS005", "HR Specialist", "Human Resources Specialist", 45000.0, 65000.0);
        
        positions.put("POS001", manager);
        positions.put("POS002", seniorDev);
        positions.put("POS003", developer);
        positions.put("POS004", analyst);
        positions.put("POS005", hrSpecialist);
        
        // Create employees
        Employee emp1 = new Employee("EMP001", "John Smith", "DEPT001", "POS001", 
                                     "john.smith@company.com", "1234567890", LocalDate.of(2020, 1, 15), 95000.0);
        Employee emp2 = new Employee("EMP002", "Jane Doe", "DEPT001", "POS002",
                                     "jane.doe@company.com", "0987654321", LocalDate.of(2021, 3, 1), 85000.0);
        Employee emp3 = new Employee("EMP003", "Bob Johnson", "DEPT002", "POS005",
                                     "bob.johnson@company.com", "1122334455", LocalDate.of(2020, 6, 10), 55000.0);
        Employee emp4 = new Employee("EMP004", "Alice Williams", "DEPT001", "POS003",
                                     "alice.williams@company.com", "5566778899", LocalDate.of(2022, 1, 20), 65000.0);
        Employee emp5 = new Employee("EMP005", "Charlie Brown", "DEPT003", "POS004",
                                     "charlie.brown@company.com", "9988776655", LocalDate.of(2021, 9, 5), 60000.0);
        
        employees.put("EMP001", emp1);
        employees.put("EMP002", emp2);
        employees.put("EMP003", emp3);
        employees.put("EMP004", emp4);
        employees.put("EMP005", emp5);
        totalEmployees = employees.size();
        
        // Create projects
        Project project1 = new Project("PROJ001", "Website Redesign", "DEPT001", LocalDate.of(2024, 1, 1),
                                       LocalDate.of(2024, 6, 30), "IN_PROGRESS", 100000.0);
        Project project2 = new Project("PROJ002", "Mobile App Development", "DEPT001", LocalDate.of(2024, 2, 1),
                                       LocalDate.of(2024, 8, 31), "IN_PROGRESS", 150000.0);
        projects.put("PROJ001", project1);
        projects.put("PROJ002", project2);
        activeProjects = projects.size();
    }
    
    public boolean addEmployee(String employeeId, String name, String departmentId, String positionId,
                              String email, String phone, LocalDate hireDate, double salary) {
        if (employees.containsKey(employeeId)) {
            System.out.println("Employee ID already exists: " + employeeId);
            return false;
        }
        
        if (!departments.containsKey(departmentId)) {
            System.out.println("Department not found: " + departmentId);
            return false;
        }
        
        if (!positions.containsKey(positionId)) {
            System.out.println("Position not found: " + positionId);
            return false;
        }
        
        if (salary <= 0) {
            System.out.println("Salary must be positive");
            return false;
        }
        
        Employee employee = new Employee(employeeId, name, departmentId, positionId, email, phone, hireDate, salary);
        employees.put(employeeId, employee);
        totalEmployees++;
        
        Department department = departments.get(departmentId);
        department.incrementEmployeeCount();
        
        System.out.println("Employee added: " + employeeId);
        return true;
    }
    
    public boolean markAttendance(String employeeId, LocalDate date, LocalDateTime checkIn, LocalDateTime checkOut) {
        if (!employees.containsKey(employeeId)) {
            System.out.println("Employee not found: " + employeeId);
            return false;
        }
        
        if (checkIn.isAfter(checkOut)) {
            System.out.println("Check-in time must be before check-out time");
            return false;
        }
        
        // Check for existing attendance record
        for (Attendance attendance : attendanceRecords) {
            if (attendance.getEmployeeId().equals(employeeId) && 
                attendance.getDate().equals(date)) {
                System.out.println("Attendance already recorded for this date");
                return false;
            }
        }
        
        long hoursWorked = ChronoUnit.HOURS.between(checkIn, checkOut);
        long minutesWorked = ChronoUnit.MINUTES.between(checkIn, checkOut) % 60;
        double totalHours = hoursWorked + (minutesWorked / 60.0);
        
        double overtimeHours = 0.0;
        if (totalHours > WORKING_HOURS_PER_DAY) {
            overtimeHours = totalHours - WORKING_HOURS_PER_DAY;
        }
        
        String attendanceId = generateAttendanceId();
        Attendance attendance = new Attendance(attendanceId, employeeId, date, checkIn, checkOut,
                                              totalHours, overtimeHours, "PRESENT");
        attendanceRecords.add(attendance);
        
        System.out.println("Attendance marked: " + employeeId + " - " + totalHours + " hours");
        return true;
    }
    
    public LeaveRequest requestLeave(String employeeId, LocalDate startDate, LocalDate endDate, String leaveType, String reason) {
        if (!employees.containsKey(employeeId)) {
            System.out.println("Employee not found: " + employeeId);
            return null;
        }
        
        if (endDate.isBefore(startDate)) {
            System.out.println("End date must be after start date");
            return null;
        }
        
        long daysRequested = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        
        Employee employee = employees.get(employeeId);
        if (employee.getRemainingLeaveDays() < daysRequested) {
            System.out.println("Insufficient leave days. Available: " + employee.getRemainingLeaveDays());
            return null;
        }
        
        String leaveRequestId = generateLeaveRequestId();
        LeaveRequest leaveRequest = new LeaveRequest(leaveRequestId, employeeId, startDate, endDate,
                                                     (int)daysRequested, leaveType, reason, LocalDate.now(), "PENDING");
        leaveRequests.add(leaveRequest);
        
        System.out.println("Leave request submitted: " + leaveRequestId);
        return leaveRequest;
    }
    
    public boolean approveLeave(String leaveRequestId, String approverId) {
        if (!employees.containsKey(approverId)) {
            System.out.println("Approver not found: " + approverId);
            return false;
        }
        
        LeaveRequest leaveRequest = findLeaveRequest(leaveRequestId);
        if (leaveRequest == null) {
            System.out.println("Leave request not found: " + leaveRequestId);
            return false;
        }
        
        if (!leaveRequest.getStatus().equals("PENDING")) {
            System.out.println("Leave request cannot be approved. Current status: " + leaveRequest.getStatus());
            return false;
        }
        
        leaveRequest.setStatus("APPROVED");
        leaveRequest.setApproverId(approverId);
        leaveRequest.setApprovalDate(LocalDate.now());
        
        Employee employee = employees.get(leaveRequest.getEmployeeId());
        employee.deductLeaveDays(leaveRequest.getDays());
        employee.incrementUsedLeaveDays(leaveRequest.getDays());
        
        System.out.println("Leave request approved: " + leaveRequestId);
        return true;
    }
    
    public Payroll generatePayroll(String employeeId, LocalDate payPeriodStart, LocalDate payPeriodEnd) {
        if (!employees.containsKey(employeeId)) {
            System.out.println("Employee not found: " + employeeId);
            return null;
        }
        
        Employee employee = employees.get(employeeId);
        
        // Calculate base salary
        double baseSalary = employee.getSalary();
        int workingDays = (int)ChronoUnit.DAYS.between(payPeriodStart, payPeriodEnd) + 1;
        double monthlyBaseSalary = baseSalary / 12.0;
        double periodBaseSalary = (monthlyBaseSalary / 30.0) * workingDays;
        
        // Calculate attendance hours
        double totalHours = 0.0;
        double totalOvertimeHours = 0.0;
        int daysPresent = 0;
        
        for (Attendance attendance : attendanceRecords) {
            if (attendance.getEmployeeId().equals(employeeId) &&
                !attendance.getDate().isBefore(payPeriodStart) &&
                !attendance.getDate().isAfter(payPeriodEnd) &&
                attendance.getStatus().equals("PRESENT")) {
                totalHours += attendance.getHoursWorked();
                totalOvertimeHours += attendance.getOvertimeHours();
                daysPresent++;
            }
        }
        
        // Calculate overtime pay
        double hourlyRate = baseSalary / (22 * WORKING_HOURS_PER_DAY * 12); // Assuming 22 working days per month
        double overtimePay = totalOvertimeHours * hourlyRate * OVERTIME_RATE;
        
        // Calculate bonuses
        double bonus = 0.0;
        if (daysPresent >= workingDays * 0.95) { // 95% attendance
            bonus = periodBaseSalary * BONUS_PERCENTAGE;
        }
        
        // Calculate deductions
        double tax = periodBaseSalary * 0.20; // 20% tax
        double insurance = periodBaseSalary * 0.05; // 5% insurance
        double totalDeductions = tax + insurance;
        
        // Calculate net salary
        double grossSalary = periodBaseSalary + overtimePay + bonus;
        double netSalary = grossSalary - totalDeductions;
        
        String payrollId = generatePayrollId();
        Payroll payroll = new Payroll(payrollId, employeeId, payPeriodStart, payPeriodEnd,
                                     periodBaseSalary, overtimePay, bonus, totalDeductions,
                                     grossSalary, netSalary, LocalDate.now(), "GENERATED");
        payrollRecords.add(payroll);
        totalPayroll += netSalary;
        
        System.out.println("Payroll generated: " + payrollId + " Net Salary: " + netSalary);
        return payroll;
    }
    
    public boolean assignProject(String employeeId, String projectId, String role, double hoursAllocated) {
        if (!employees.containsKey(employeeId)) {
            System.out.println("Employee not found: " + employeeId);
            return false;
        }
        
        if (!projects.containsKey(projectId)) {
            System.out.println("Project not found: " + projectId);
            return false;
        }
        
        Project project = projects.get(projectId);
        project.assignEmployee(employeeId, role, hoursAllocated);
        
        Employee employee = employees.get(employeeId);
        employee.addProject(projectId);
        
        System.out.println("Employee assigned to project: " + employeeId + " -> " + projectId);
        return true;
    }
    
    public Task createTask(String projectId, String assignedTo, String title, String description,
                          LocalDate dueDate, String priority) {
        if (!projects.containsKey(projectId)) {
            System.out.println("Project not found: " + projectId);
            return null;
        }
        
        if (!employees.containsKey(assignedTo)) {
            System.out.println("Employee not found: " + assignedTo);
            return null;
        }
        
        String taskId = generateTaskId();
        Task task = new Task(taskId, projectId, assignedTo, title, description, dueDate,
                            priority, LocalDate.now(), "PENDING");
        tasks.add(task);
        
        System.out.println("Task created: " + taskId);
        return task;
    }
    
    public boolean completeTask(String taskId) {
        Task task = findTask(taskId);
        if (task == null) {
            System.out.println("Task not found: " + taskId);
            return false;
        }
        
        if (task.getStatus().equals("COMPLETED")) {
            System.out.println("Task already completed");
            return false;
        }
        
        task.setStatus("COMPLETED");
        task.setCompletionDate(LocalDate.now());
        
        System.out.println("Task completed: " + taskId);
        return true;
    }
    
    public PerformanceReview createPerformanceReview(String employeeId, LocalDate reviewDate,
                                                     double rating, String comments, String reviewedBy) {
        if (!employees.containsKey(employeeId)) {
            System.out.println("Employee not found: " + employeeId);
            return null;
        }
        
        if (rating < 0.0 || rating > 5.0) {
            System.out.println("Rating must be between 0 and 5");
            return null;
        }
        
        String reviewId = generateReviewId();
        PerformanceReview review = new PerformanceReview(reviewId, employeeId, reviewDate, rating,
                                                        comments, reviewedBy, LocalDate.now());
        performanceReviews.add(review);
        
        Employee employee = employees.get(employeeId);
        employee.updatePerformanceRating(rating);
        
        System.out.println("Performance review created: " + reviewId);
        return review;
    }
    
    public void generateEmployeeReport(String employeeId) {
        if (!employees.containsKey(employeeId)) {
            System.out.println("Employee not found: " + employeeId);
            return;
        }
        
        Employee employee = employees.get(employeeId);
        System.out.println("\n=== Employee Report ===");
        System.out.println("Employee ID: " + employeeId);
        System.out.println("Name: " + employee.getName());
        System.out.println("Department: " + departments.get(employee.getDepartmentId()).getName());
        System.out.println("Position: " + positions.get(employee.getPositionId()).getTitle());
        System.out.println("Salary: " + employee.getSalary());
        System.out.println("Performance Rating: " + employee.getPerformanceRating());
        System.out.println("Remaining Leave Days: " + employee.getRemainingLeaveDays());
        System.out.println("Used Leave Days: " + employee.getUsedLeaveDays());
        
        System.out.println("\nAttendance (Last 30 days):");
        System.out.println("Date\t\tCheck In\t\tCheck Out\t\tHours");
        System.out.println("---------------------------------------------------------------");
        
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        int daysPresent = 0;
        double totalHours = 0.0;
        
        for (Attendance attendance : attendanceRecords) {
            if (attendance.getEmployeeId().equals(employeeId) &&
                !attendance.getDate().isBefore(thirtyDaysAgo) &&
                attendance.getStatus().equals("PRESENT")) {
                System.out.println(
                    attendance.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\t" +
                    attendance.getCheckIn().format(DateTimeFormatter.ofPattern("HH:mm")) + "\t\t" +
                    attendance.getCheckOut().format(DateTimeFormatter.ofPattern("HH:mm")) + "\t\t" +
                    String.format("%.2f", attendance.getHoursWorked())
                );
                daysPresent++;
                totalHours += attendance.getHoursWorked();
            }
        }
        
        System.out.println("\nTotal Days Present: " + daysPresent);
        System.out.println("Total Hours Worked: " + String.format("%.2f", totalHours));
        
        System.out.println("\nActive Projects:");
        for (String projectId : employee.getProjects()) {
            Project project = projects.get(projectId);
            if (project.getStatus().equals("IN_PROGRESS")) {
                System.out.println("  " + project.getName() + " (" + projectId + ")");
            }
        }
        
        System.out.println("=== End of Report ===\n");
    }
    
    public void generateDepartmentReport(String departmentId) {
        if (!departments.containsKey(departmentId)) {
            System.out.println("Department not found: " + departmentId);
            return;
        }
        
        Department department = departments.get(departmentId);
        System.out.println("\n=== Department Report ===");
        System.out.println("Department ID: " + departmentId);
        System.out.println("Name: " + department.getName());
        System.out.println("Employee Count: " + department.getEmployeeCount());
        
        System.out.println("\nEmployees:");
        System.out.println("Employee ID\tName\t\tPosition\t\tSalary");
        System.out.println("---------------------------------------------------------------");
        
        double totalSalary = 0.0;
        for (Employee employee : employees.values()) {
            if (employee.getDepartmentId().equals(departmentId)) {
                Position position = positions.get(employee.getPositionId());
                System.out.println(
                    employee.getEmployeeId() + "\t\t" +
                    employee.getName() + "\t\t" +
                    position.getTitle() + "\t\t" +
                    employee.getSalary()
                );
                totalSalary += employee.getSalary();
            }
        }
        
        System.out.println("\nTotal Department Salary: " + totalSalary);
        System.out.println("Average Salary: " + (department.getEmployeeCount() > 0 ? 
                          totalSalary / department.getEmployeeCount() : 0));
        System.out.println("=== End of Report ===\n");
    }
    
    public void generatePayrollReport(LocalDate startDate, LocalDate endDate) {
        System.out.println("\n=== Payroll Report ===");
        System.out.println("Period: " + startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + " to " +
                          endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        System.out.println("\nPayroll Records:");
        System.out.println("Employee ID\tGross Salary\tDeductions\tNet Salary");
        System.out.println("---------------------------------------------------------------");
        
        double totalGross = 0.0;
        double totalDeductions = 0.0;
        double totalNet = 0.0;
        
        for (Payroll payroll : payrollRecords) {
            if (!payroll.getPayPeriodStart().isBefore(startDate) &&
                !payroll.getPayPeriodEnd().isAfter(endDate)) {
                System.out.println(
                    payroll.getEmployeeId() + "\t\t" +
                    String.format("%.2f", payroll.getGrossSalary()) + "\t\t" +
                    String.format("%.2f", payroll.getTotalDeductions()) + "\t\t" +
                    String.format("%.2f", payroll.getNetSalary())
                );
                totalGross += payroll.getGrossSalary();
                totalDeductions += payroll.getTotalDeductions();
                totalNet += payroll.getNetSalary();
            }
        }
        
        System.out.println("\nTotals:");
        System.out.println("Total Gross Salary: " + String.format("%.2f", totalGross));
        System.out.println("Total Deductions: " + String.format("%.2f", totalDeductions));
        System.out.println("Total Net Salary: " + String.format("%.2f", totalNet));
        System.out.println("=== End of Report ===\n");
    }
    
    private LeaveRequest findLeaveRequest(String leaveRequestId) {
        for (LeaveRequest leaveRequest : leaveRequests) {
            if (leaveRequest.getLeaveRequestId().equals(leaveRequestId)) {
                return leaveRequest;
            }
        }
        return null;
    }
    
    private Task findTask(String taskId) {
        for (Task task : tasks) {
            if (task.getTaskId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }
    
    private String generateAttendanceId() {
        return "ATT" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generateLeaveRequestId() {
        return "LEAVE" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generatePayrollId() {
        return "PAY" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generateTaskId() {
        return "TASK" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generateReviewId() {
        return "REV" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    // Nested classes
    class Employee {
        private String employeeId;
        private String name;
        private String departmentId;
        private String positionId;
        private String email;
        private String phone;
        private LocalDate hireDate;
        private double salary;
        private int remainingLeaveDays;
        private int usedLeaveDays;
        private double performanceRating;
        private List<String> projects;
        
        public Employee(String employeeId, String name, String departmentId, String positionId,
                       String email, String phone, LocalDate hireDate, double salary) {
            this.employeeId = employeeId;
            this.name = name;
            this.departmentId = departmentId;
            this.positionId = positionId;
            this.email = email;
            this.phone = phone;
            this.hireDate = hireDate;
            this.salary = salary;
            this.remainingLeaveDays = MAX_LEAVE_DAYS;
            this.usedLeaveDays = 0;
            this.performanceRating = 0.0;
            this.projects = new ArrayList<>();
        }
        
        public void deductLeaveDays(int days) {
            if (days > 0 && remainingLeaveDays >= days) {
                remainingLeaveDays -= days;
            }
        }
        
        public void incrementUsedLeaveDays(int days) {
            usedLeaveDays += days;
        }
        
        public void updatePerformanceRating(double rating) {
            // Average with existing rating
            if (performanceRating == 0.0) {
                performanceRating = rating;
            } else {
                performanceRating = (performanceRating + rating) / 2.0;
            }
        }
        
        public void addProject(String projectId) {
            if (!projects.contains(projectId)) {
                projects.add(projectId);
            }
        }
        
        public String getEmployeeId() { return employeeId; }
        public String getName() { return name; }
        public String getDepartmentId() { return departmentId; }
        public String getPositionId() { return positionId; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public LocalDate getHireDate() { return hireDate; }
        public double getSalary() { return salary; }
        public int getRemainingLeaveDays() { return remainingLeaveDays; }
        public int getUsedLeaveDays() { return usedLeaveDays; }
        public double getPerformanceRating() { return performanceRating; }
        public List<String> getProjects() { return new ArrayList<>(projects); }
    }
    
    class Department {
        private String departmentId;
        private String name;
        private String description;
        private int maxCapacity;
        private int employeeCount;
        
        public Department(String departmentId, String name, String description, int maxCapacity) {
            this.departmentId = departmentId;
            this.name = name;
            this.description = description;
            this.maxCapacity = maxCapacity;
            this.employeeCount = 0;
        }
        
        public void incrementEmployeeCount() {
            if (employeeCount < maxCapacity) {
                employeeCount++;
            }
        }
        
        public String getDepartmentId() { return departmentId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getMaxCapacity() { return maxCapacity; }
        public int getEmployeeCount() { return employeeCount; }
    }
    
    class Position {
        private String positionId;
        private String title;
        private String description;
        private double minSalary;
        private double maxSalary;
        
        public Position(String positionId, String title, String description, double minSalary, double maxSalary) {
            this.positionId = positionId;
            this.title = title;
            this.description = description;
            this.minSalary = minSalary;
            this.maxSalary = maxSalary;
        }
        
        public String getPositionId() { return positionId; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public double getMinSalary() { return minSalary; }
        public double getMaxSalary() { return maxSalary; }
    }
    
    class Attendance {
        private String attendanceId;
        private String employeeId;
        private LocalDate date;
        private LocalDateTime checkIn;
        private LocalDateTime checkOut;
        private double hoursWorked;
        private double overtimeHours;
        private String status;
        
        public Attendance(String attendanceId, String employeeId, LocalDate date,
                         LocalDateTime checkIn, LocalDateTime checkOut, double hoursWorked,
                         double overtimeHours, String status) {
            this.attendanceId = attendanceId;
            this.employeeId = employeeId;
            this.date = date;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.hoursWorked = hoursWorked;
            this.overtimeHours = overtimeHours;
            this.status = status;
        }
        
        public String getAttendanceId() { return attendanceId; }
        public String getEmployeeId() { return employeeId; }
        public LocalDate getDate() { return date; }
        public LocalDateTime getCheckIn() { return checkIn; }
        public LocalDateTime getCheckOut() { return checkOut; }
        public double getHoursWorked() { return hoursWorked; }
        public double getOvertimeHours() { return overtimeHours; }
        public String getStatus() { return status; }
    }
    
    class LeaveRequest {
        private String leaveRequestId;
        private String employeeId;
        private LocalDate startDate;
        private LocalDate endDate;
        private int days;
        private String leaveType;
        private String reason;
        private LocalDate requestDate;
        private String status;
        private String approverId;
        private LocalDate approvalDate;
        
        public LeaveRequest(String leaveRequestId, String employeeId, LocalDate startDate,
                           LocalDate endDate, int days, String leaveType, String reason,
                           LocalDate requestDate, String status) {
            this.leaveRequestId = leaveRequestId;
            this.employeeId = employeeId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.days = days;
            this.leaveType = leaveType;
            this.reason = reason;
            this.requestDate = requestDate;
            this.status = status;
        }
        
        public String getLeaveRequestId() { return leaveRequestId; }
        public String getEmployeeId() { return employeeId; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public int getDays() { return days; }
        public String getLeaveType() { return leaveType; }
        public String getReason() { return reason; }
        public LocalDate getRequestDate() { return requestDate; }
        public String getStatus() { return status; }
        public String getApproverId() { return approverId; }
        public LocalDate getApprovalDate() { return approvalDate; }
        public void setStatus(String status) { this.status = status; }
        public void setApproverId(String approverId) { this.approverId = approverId; }
        public void setApprovalDate(LocalDate date) { this.approvalDate = date; }
    }
    
    class Payroll {
        private String payrollId;
        private String employeeId;
        private LocalDate payPeriodStart;
        private LocalDate payPeriodEnd;
        private double baseSalary;
        private double overtimePay;
        private double bonus;
        private double totalDeductions;
        private double grossSalary;
        private double netSalary;
        private LocalDate generatedDate;
        private String status;
        
        public Payroll(String payrollId, String employeeId, LocalDate payPeriodStart, LocalDate payPeriodEnd,
                      double baseSalary, double overtimePay, double bonus, double totalDeductions,
                      double grossSalary, double netSalary, LocalDate generatedDate, String status) {
            this.payrollId = payrollId;
            this.employeeId = employeeId;
            this.payPeriodStart = payPeriodStart;
            this.payPeriodEnd = payPeriodEnd;
            this.baseSalary = baseSalary;
            this.overtimePay = overtimePay;
            this.bonus = bonus;
            this.totalDeductions = totalDeductions;
            this.grossSalary = grossSalary;
            this.netSalary = netSalary;
            this.generatedDate = generatedDate;
            this.status = status;
        }
        
        public String getPayrollId() { return payrollId; }
        public String getEmployeeId() { return employeeId; }
        public LocalDate getPayPeriodStart() { return payPeriodStart; }
        public LocalDate getPayPeriodEnd() { return payPeriodEnd; }
        public double getBaseSalary() { return baseSalary; }
        public double getOvertimePay() { return overtimePay; }
        public double getBonus() { return bonus; }
        public double getTotalDeductions() { return totalDeductions; }
        public double getGrossSalary() { return grossSalary; }
        public double getNetSalary() { return netSalary; }
        public LocalDate getGeneratedDate() { return generatedDate; }
        public String getStatus() { return status; }
    }
    
    class Project {
        private String projectId;
        private String name;
        private String departmentId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private double budget;
        private Map<String, ProjectAssignment> assignments;
        
        public Project(String projectId, String name, String departmentId, LocalDate startDate,
                      LocalDate endDate, String status, double budget) {
            this.projectId = projectId;
            this.name = name;
            this.departmentId = departmentId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.budget = budget;
            this.assignments = new HashMap<>();
        }
        
        public void assignEmployee(String employeeId, String role, double hoursAllocated) {
            assignments.put(employeeId, new ProjectAssignment(employeeId, role, hoursAllocated));
        }
        
        public String getProjectId() { return projectId; }
        public String getName() { return name; }
        public String getDepartmentId() { return departmentId; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public String getStatus() { return status; }
        public double getBudget() { return budget; }
        
        class ProjectAssignment {
            private String employeeId;
            private String role;
            private double hoursAllocated;
            
            public ProjectAssignment(String employeeId, String role, double hoursAllocated) {
                this.employeeId = employeeId;
                this.role = role;
                this.hoursAllocated = hoursAllocated;
            }
        }
    }
    
    class Task {
        private String taskId;
        private String projectId;
        private String assignedTo;
        private String title;
        private String description;
        private LocalDate dueDate;
        private String priority;
        private LocalDate createdDate;
        private LocalDate completionDate;
        private String status;
        
        public Task(String taskId, String projectId, String assignedTo, String title,
                   String description, LocalDate dueDate, String priority, LocalDate createdDate, String status) {
            this.taskId = taskId;
            this.projectId = projectId;
            this.assignedTo = assignedTo;
            this.title = title;
            this.description = description;
            this.dueDate = dueDate;
            this.priority = priority;
            this.createdDate = createdDate;
            this.status = status;
            this.completionDate = null;
        }
        
        public String getTaskId() { return taskId; }
        public String getProjectId() { return projectId; }
        public String getAssignedTo() { return assignedTo; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public LocalDate getDueDate() { return dueDate; }
        public String getPriority() { return priority; }
        public LocalDate getCreatedDate() { return createdDate; }
        public LocalDate getCompletionDate() { return completionDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public void setCompletionDate(LocalDate date) { this.completionDate = date; }
    }
    
    class PerformanceReview {
        private String reviewId;
        private String employeeId;
        private LocalDate reviewDate;
        private double rating;
        private String comments;
        private String reviewedBy;
        private LocalDate createdDate;
        
        public PerformanceReview(String reviewId, String employeeId, LocalDate reviewDate,
                                double rating, String comments, String reviewedBy, LocalDate createdDate) {
            this.reviewId = reviewId;
            this.employeeId = employeeId;
            this.reviewDate = reviewDate;
            this.rating = rating;
            this.comments = comments;
            this.reviewedBy = reviewedBy;
            this.createdDate = createdDate;
        }
        
        public String getReviewId() { return reviewId; }
        public String getEmployeeId() { return employeeId; }
        public LocalDate getReviewDate() { return reviewDate; }
        public double getRating() { return rating; }
        public String getComments() { return comments; }
        public String getReviewedBy() { return reviewedBy; }
        public LocalDate getCreatedDate() { return createdDate; }
    }
    
    public static void main(String[] args) {
        EmployeeManagementSystem ems = new EmployeeManagementSystem();
        
        // Test operations
        LocalDateTime checkIn = LocalDateTime.now().withHour(9).withMinute(0);
        LocalDateTime checkOut = LocalDateTime.now().withHour(17).withMinute(30);
        ems.markAttendance("EMP001", LocalDate.now(), checkIn, checkOut);
        
        LeaveRequest leave = ems.requestLeave("EMP002", LocalDate.now().plusDays(5), 
                                             LocalDate.now().plusDays(7), "ANNUAL", "Vacation");
        if (leave != null) {
            ems.approveLeave(leave.getLeaveRequestId(), "EMP001");
        }
        
        ems.generatePayroll("EMP001", LocalDate.now().minusDays(30), LocalDate.now());
        ems.generateEmployeeReport("EMP001");
        ems.generateDepartmentReport("DEPT001");
    }
}
