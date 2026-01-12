import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive Hospital Management System
 * Manages patients, doctors, appointments, medical records, and billing
 */
public class HospitalManagementSystem {
    
    // Constants
    private static final double CONSULTATION_FEE = 500.0;
    private static final double SURGERY_FEE_BASE = 50000.0;
    private static final int MAX_APPOINTMENTS_PER_DAY = 20;
    
    // Data structures
    private Map<String, Patient> patients;
    private Map<String, Doctor> doctors;
    private Map<String, Department> departments;
    private Map<String, Room> rooms;
    private List<Appointment> appointments;
    private List<MedicalRecord> medicalRecords;
    private List<Bill> bills;
    private Map<String, Medicine> medicines;
    
    // Statistics
    private int totalPatients;
    private int totalDoctors;
    private int totalAppointments;
    private double totalRevenue;
    
    public HospitalManagementSystem() {
        this.patients = new HashMap<>();
        this.doctors = new HashMap<>();
        this.departments = new HashMap<>();
        this.rooms = new HashMap<>();
        this.appointments = new ArrayList<>();
        this.medicalRecords = new ArrayList<>();
        this.bills = new ArrayList<>();
        this.medicines = new HashMap<>();
        this.totalPatients = 0;
        this.totalDoctors = 0;
        this.totalAppointments = 0;
        this.totalRevenue = 0.0;
        initializeSystem();
    }
    
    private void initializeSystem() {
        // Create departments
        Department cardiology = new Department("DEPT001", "Cardiology", "Cardiac care unit");
        Department neurology = new Department("DEPT002", "Neurology", "Brain and nerve disorders");
        Department pediatrics = new Department("DEPT003", "Pediatrics", "Children's healthcare");
        Department surgery = new Department("DEPT004", "Surgery", "Surgical procedures");
        
        departments.put("DEPT001", cardiology);
        departments.put("DEPT002", neurology);
        departments.put("DEPT003", pediatrics);
        departments.put("DEPT004", surgery);
        
        // Create doctors
        Doctor doctor1 = new Doctor("DOC001", "Dr. Sarah Johnson", "DEPT001", "Cardiologist", "MD", 150000.0);
        Doctor doctor2 = new Doctor("DOC002", "Dr. Michael Chen", "DEPT002", "Neurologist", "MD", 145000.0);
        Doctor doctor3 = new Doctor("DOC003", "Dr. Emily Brown", "DEPT003", "Pediatrician", "MD", 130000.0);
        Doctor doctor4 = new Doctor("DOC004", "Dr. James Wilson", "DEPT004", "Surgeon", "MD", 200000.0);
        
        doctors.put("DOC001", doctor1);
        doctors.put("DOC002", doctor2);
        doctors.put("DOC003", doctor3);
        doctors.put("DOC004", doctor4);
        totalDoctors = doctors.size();
        
        // Create rooms
        for (int i = 1; i <= 50; i++) {
            String roomId = "ROOM" + String.format("%03d", i);
            String roomType = (i <= 20) ? "GENERAL" : ((i <= 35) ? "PRIVATE" : "ICU");
            Room room = new Room(roomId, roomType, 1000.0 + (i * 100));
            rooms.put(roomId, room);
        }
        
        // Create medicines
        Medicine medicine1 = new Medicine("MED001", "Paracetamol", "Pain reliever", 50.0, 1000);
        Medicine medicine2 = new Medicine("MED002", "Amoxicillin", "Antibiotic", 150.0, 500);
        Medicine medicine3 = new Medicine("MED003", "Aspirin", "Blood thinner", 30.0, 800);
        Medicine medicine4 = new Medicine("MED004", "Insulin", "Diabetes medication", 500.0, 200);
        
        medicines.put("MED001", medicine1);
        medicines.put("MED002", medicine2);
        medicines.put("MED003", medicine3);
        medicines.put("MED004", medicine4);
        
        // Create sample patients
        Patient patient1 = new Patient("PAT001", "John Doe", 45, "M", "1234567890", "123 Main St");
        Patient patient2 = new Patient("PAT002", "Jane Smith", 32, "F", "0987654321", "456 Oak Ave");
        Patient patient3 = new Patient("PAT003", "Robert Lee", 28, "M", "1122334455", "789 Pine Rd");
        
        patients.put("PAT001", patient1);
        patients.put("PAT002", patient2);
        patients.put("PAT003", patient3);
        totalPatients = patients.size();
    }
    
    public boolean registerPatient(String patientId, String name, int age, String gender, 
                                   String phone, String address) {
        if (patients.containsKey(patientId)) {
            System.out.println("Patient ID already exists: " + patientId);
            return false;
        }
        
        if (age < 0 || age > 150) {
            System.out.println("Invalid age");
            return false;
        }
        
        Patient patient = new Patient(patientId, name, age, gender, phone, address);
        patients.put(patientId, patient);
        totalPatients++;
        System.out.println("Patient registered: " + patientId);
        return true;
    }
    
    public Appointment bookAppointment(String patientId, String doctorId, LocalDateTime appointmentTime) {
        if (!patients.containsKey(patientId)) {
            System.out.println("Patient not found: " + patientId);
            return null;
        }
        
        if (!doctors.containsKey(doctorId)) {
            System.out.println("Doctor not found: " + doctorId);
            return null;
        }
        
        // Check doctor availability
        if (!isDoctorAvailable(doctorId, appointmentTime)) {
            System.out.println("Doctor not available at the requested time");
            return null;
        }
        
        // Check appointment limit for the day
        int appointmentsToday = countAppointmentsForDate(appointmentTime.toLocalDate());
        if (appointmentsToday >= MAX_APPOINTMENTS_PER_DAY) {
            System.out.println("Maximum appointments reached for this day");
            return null;
        }
        
        String appointmentId = generateAppointmentId();
        Appointment appointment = new Appointment(appointmentId, patientId, doctorId, 
                                                  appointmentTime, "SCHEDULED", CONSULTATION_FEE);
        appointments.add(appointment);
        totalAppointments++;
        
        System.out.println("Appointment booked: " + appointmentId + " at " + 
                          appointmentTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return appointment;
    }
    
    public boolean completeAppointment(String appointmentId, String diagnosis, String prescription) {
        Appointment appointment = findAppointment(appointmentId);
        if (appointment == null) {
            System.out.println("Appointment not found: " + appointmentId);
            return false;
        }
        
        if (!appointment.getStatus().equals("SCHEDULED")) {
            System.out.println("Appointment cannot be completed. Current status: " + appointment.getStatus());
            return false;
        }
        
        appointment.setStatus("COMPLETED");
        
        // Create medical record
        String recordId = generateRecordId();
        MedicalRecord record = new MedicalRecord(recordId, appointment.getPatientId(), 
                                                 appointment.getDoctorId(), LocalDate.now(),
                                                 diagnosis, prescription, appointment.getAppointmentTime());
        medicalRecords.add(record);
        
        // Create bill
        Bill bill = createBill(appointment.getPatientId(), appointmentId);
        if (bill != null) {
            bills.add(bill);
            totalRevenue += bill.getTotalAmount();
        }
        
        System.out.println("Appointment completed: " + appointmentId);
        return true;
    }
    
    public boolean admitPatient(String patientId, String roomId, String reason, int days) {
        if (!patients.containsKey(patientId)) {
            System.out.println("Patient not found: " + patientId);
            return false;
        }
        
        if (!rooms.containsKey(roomId)) {
            System.out.println("Room not found: " + roomId);
            return false;
        }
        
        Room room = rooms.get(roomId);
        if (!room.isAvailable()) {
            System.out.println("Room not available: " + roomId);
            return false;
        }
        
        Patient patient = patients.get(patientId);
        patient.setAdmitted(true);
        patient.setRoomId(roomId);
        room.setAvailable(false);
        room.setOccupiedBy(patientId);
        
        System.out.println("Patient admitted to room: " + roomId);
        
        // Create admission record
        String recordId = generateRecordId();
        MedicalRecord record = new MedicalRecord(recordId, patientId, null, LocalDate.now(),
                                                 "Admission: " + reason, "", null);
        medicalRecords.add(record);
        
        // Create bill for room charges
        double roomCharges = room.getDailyRate() * days;
        Bill bill = createAdmissionBill(patientId, roomCharges, days);
        if (bill != null) {
            bills.add(bill);
            totalRevenue += bill.getTotalAmount();
        }
        
        return true;
    }
    
    public boolean dischargePatient(String patientId) {
        if (!patients.containsKey(patientId)) {
            System.out.println("Patient not found: " + patientId);
            return false;
        }
        
        Patient patient = patients.get(patientId);
        if (!patient.isAdmitted()) {
            System.out.println("Patient is not admitted");
            return false;
        }
        
        String roomId = patient.getRoomId();
        Room room = rooms.get(roomId);
        room.setAvailable(true);
        room.setOccupiedBy(null);
        
        patient.setAdmitted(false);
        patient.setRoomId(null);
        
        System.out.println("Patient discharged from room: " + roomId);
        return true;
    }
    
    public boolean prescribeMedicine(String recordId, String medicineId, int quantity) {
        MedicalRecord record = findMedicalRecord(recordId);
        if (record == null) {
            System.out.println("Medical record not found: " + recordId);
            return false;
        }
        
        if (!medicines.containsKey(medicineId)) {
            System.out.println("Medicine not found: " + medicineId);
            return false;
        }
        
        Medicine medicine = medicines.get(medicineId);
        if (medicine.getStock() < quantity) {
            System.out.println("Insufficient medicine stock");
            return false;
        }
        
        medicine.reduceStock(quantity);
        record.addPrescription(medicineId, quantity);
        
        System.out.println("Medicine prescribed: " + medicine.getName() + " x" + quantity);
        return true;
    }
    
    public Bill createBill(String patientId, String appointmentId) {
        Patient patient = patients.get(patientId);
        Appointment appointment = findAppointment(appointmentId);
        
        if (appointment == null) {
            return null;
        }
        
        double consultationFee = CONSULTATION_FEE;
        double medicineCost = 0.0;
        double totalAmount = consultationFee + medicineCost;
        double tax = totalAmount * 0.10;
        double finalAmount = totalAmount + tax;
        
        String billId = generateBillId();
        Bill bill = new Bill(billId, patientId, appointmentId, consultationFee, medicineCost,
                           tax, finalAmount, LocalDate.now(), "PENDING");
        
        return bill;
    }
    
    public Bill createAdmissionBill(String patientId, double roomCharges, int days) {
        double totalAmount = roomCharges;
        double tax = totalAmount * 0.10;
        double finalAmount = totalAmount + tax;
        
        String billId = generateBillId();
        Bill bill = new Bill(billId, patientId, null, 0.0, 0.0,
                           tax, finalAmount, LocalDate.now(), "PENDING");
        bill.setRoomCharges(roomCharges);
        bill.setDays(days);
        return bill;
    }
    
    public boolean payBill(String billId, double amount) {
        Bill bill = findBill(billId);
        if (bill == null) {
            System.out.println("Bill not found: " + billId);
            return false;
        }
        
        if (!bill.getStatus().equals("PENDING")) {
            System.out.println("Bill already paid");
            return false;
        }
        
        if (amount < bill.getFinalAmount()) {
            System.out.println("Insufficient payment. Required: " + bill.getFinalAmount());
            return false;
        }
        
        bill.setStatus("PAID");
        bill.setPaidAmount(amount);
        bill.setPaymentDate(LocalDate.now());
        
        System.out.println("Bill paid: " + billId);
        return true;
    }
    
    public void generatePatientHistory(String patientId) {
        if (!patients.containsKey(patientId)) {
            System.out.println("Patient not found: " + patientId);
            return;
        }
        
        Patient patient = patients.get(patientId);
        System.out.println("\n=== Patient Medical History ===");
        System.out.println("Patient ID: " + patientId);
        System.out.println("Name: " + patient.getName());
        System.out.println("Age: " + patient.getAge());
        System.out.println("Gender: " + patient.getGender());
        System.out.println("\nMedical Records:");
        System.out.println("Date\t\tDoctor\t\tDiagnosis\t\tPrescription");
        System.out.println("---------------------------------------------------------------");
        
        for (MedicalRecord record : medicalRecords) {
            if (record.getPatientId().equals(patientId)) {
                String doctorName = record.getDoctorId() != null ? 
                    doctors.get(record.getDoctorId()).getName() : "N/A";
                System.out.println(
                    record.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\t" +
                    doctorName + "\t" +
                    record.getDiagnosis() + "\t\t" +
                    record.getPrescription()
                );
            }
        }
        
        System.out.println("\nBills:");
        System.out.println("Bill ID\t\tDate\t\tAmount\t\tStatus");
        System.out.println("---------------------------------------------------");
        for (Bill bill : bills) {
            if (bill.getPatientId().equals(patientId)) {
                System.out.println(
                    bill.getBillId() + "\t" +
                    bill.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\t" +
                    bill.getFinalAmount() + "\t\t" +
                    bill.getStatus()
                );
            }
        }
        
        System.out.println("=== End of History ===\n");
    }
    
    public void generateHospitalReport() {
        System.out.println("\n=== Hospital Report ===");
        System.out.println("Total Patients: " + totalPatients);
        System.out.println("Total Doctors: " + totalDoctors);
        System.out.println("Total Appointments: " + totalAppointments);
        System.out.println("Total Revenue: " + totalRevenue);
        
        int admittedPatients = 0;
        for (Patient patient : patients.values()) {
            if (patient.isAdmitted()) {
                admittedPatients++;
            }
        }
        System.out.println("Admitted Patients: " + admittedPatients);
        
        int availableRooms = 0;
        for (Room room : rooms.values()) {
            if (room.isAvailable()) {
                availableRooms++;
            }
        }
        System.out.println("Available Rooms: " + availableRooms);
        
        System.out.println("\nPatients by Department:");
        Map<String, Integer> patientsByDept = new HashMap<>();
        for (Appointment appointment : appointments) {
            Doctor doctor = doctors.get(appointment.getDoctorId());
            String deptId = doctor.getDepartmentId();
            patientsByDept.put(deptId, patientsByDept.getOrDefault(deptId, 0) + 1);
        }
        for (Map.Entry<String, Integer> entry : patientsByDept.entrySet()) {
            Department dept = departments.get(entry.getKey());
            System.out.println("  " + dept.getName() + ": " + entry.getValue() + " appointments");
        }
        
        System.out.println("=== End of Report ===\n");
    }
    
    private boolean isDoctorAvailable(String doctorId, LocalDateTime time) {
        // Simple availability check
        for (Appointment appointment : appointments) {
            if (appointment.getDoctorId().equals(doctorId) && 
                appointment.getAppointmentTime().equals(time) &&
                appointment.getStatus().equals("SCHEDULED")) {
                return false;
            }
        }
        return true;
    }
    
    private int countAppointmentsForDate(LocalDate date) {
        int count = 0;
        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentTime().toLocalDate().equals(date) &&
                appointment.getStatus().equals("SCHEDULED")) {
                count++;
            }
        }
        return count;
    }
    
    private Appointment findAppointment(String appointmentId) {
        for (Appointment appointment : appointments) {
            if (appointment.getAppointmentId().equals(appointmentId)) {
                return appointment;
            }
        }
        return null;
    }
    
    private MedicalRecord findMedicalRecord(String recordId) {
        for (MedicalRecord record : medicalRecords) {
            if (record.getRecordId().equals(recordId)) {
                return record;
            }
        }
        return null;
    }
    
    private Bill findBill(String billId) {
        for (Bill bill : bills) {
            if (bill.getBillId().equals(billId)) {
                return bill;
            }
        }
        return null;
    }
    
    private String generateAppointmentId() {
        return "APT" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generateRecordId() {
        return "REC" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generateBillId() {
        return "BILL" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    // Nested classes
    class Patient {
        private String patientId;
        private String name;
        private int age;
        private String gender;
        private String phone;
        private String address;
        private boolean isAdmitted;
        private String roomId;
        
        public Patient(String patientId, String name, int age, String gender, String phone, String address) {
            this.patientId = patientId;
            this.name = name;
            this.age = age;
            this.gender = gender;
            this.phone = phone;
            this.address = address;
            this.isAdmitted = false;
            this.roomId = null;
        }
        
        public String getPatientId() { return patientId; }
        public String getName() { return name; }
        public int getAge() { return age; }
        public String getGender() { return gender; }
        public String getPhone() { return phone; }
        public String getAddress() { return address; }
        public boolean isAdmitted() { return isAdmitted; }
        public String getRoomId() { return roomId; }
        public void setAdmitted(boolean admitted) { this.isAdmitted = admitted; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
    }
    
    class Doctor {
        private String doctorId;
        private String name;
        private String departmentId;
        private String specialization;
        private String qualification;
        private double salary;
        
        public Doctor(String doctorId, String name, String departmentId, String specialization,
                     String qualification, double salary) {
            this.doctorId = doctorId;
            this.name = name;
            this.departmentId = departmentId;
            this.specialization = specialization;
            this.qualification = qualification;
            this.salary = salary;
        }
        
        public String getDoctorId() { return doctorId; }
        public String getName() { return name; }
        public String getDepartmentId() { return departmentId; }
        public String getSpecialization() { return specialization; }
        public String getQualification() { return qualification; }
        public double getSalary() { return salary; }
    }
    
    class Department {
        private String departmentId;
        private String name;
        private String description;
        
        public Department(String departmentId, String name, String description) {
            this.departmentId = departmentId;
            this.name = name;
            this.description = description;
        }
        
        public String getDepartmentId() { return departmentId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    class Room {
        private String roomId;
        private String roomType;
        private double dailyRate;
        private boolean available;
        private String occupiedBy;
        
        public Room(String roomId, String roomType, double dailyRate) {
            this.roomId = roomId;
            this.roomType = roomType;
            this.dailyRate = dailyRate;
            this.available = true;
            this.occupiedBy = null;
        }
        
        public String getRoomId() { return roomId; }
        public String getRoomType() { return roomType; }
        public double getDailyRate() { return dailyRate; }
        public boolean isAvailable() { return available; }
        public String getOccupiedBy() { return occupiedBy; }
        public void setAvailable(boolean available) { this.available = available; }
        public void setOccupiedBy(String patientId) { this.occupiedBy = patientId; }
    }
    
    class Appointment {
        private String appointmentId;
        private String patientId;
        private String doctorId;
        private LocalDateTime appointmentTime;
        private String status;
        private double fee;
        
        public Appointment(String appointmentId, String patientId, String doctorId,
                          LocalDateTime appointmentTime, String status, double fee) {
            this.appointmentId = appointmentId;
            this.patientId = patientId;
            this.doctorId = doctorId;
            this.appointmentTime = appointmentTime;
            this.status = status;
            this.fee = fee;
        }
        
        public String getAppointmentId() { return appointmentId; }
        public String getPatientId() { return patientId; }
        public String getDoctorId() { return doctorId; }
        public LocalDateTime getAppointmentTime() { return appointmentTime; }
        public String getStatus() { return status; }
        public double getFee() { return fee; }
        public void setStatus(String status) { this.status = status; }
    }
    
    class MedicalRecord {
        private String recordId;
        private String patientId;
        private String doctorId;
        private LocalDate date;
        private String diagnosis;
        private String prescription;
        private LocalDateTime appointmentTime;
        private Map<String, Integer> prescriptions;
        
        public MedicalRecord(String recordId, String patientId, String doctorId, LocalDate date,
                            String diagnosis, String prescription, LocalDateTime appointmentTime) {
            this.recordId = recordId;
            this.patientId = patientId;
            this.doctorId = doctorId;
            this.date = date;
            this.diagnosis = diagnosis;
            this.prescription = prescription;
            this.appointmentTime = appointmentTime;
            this.prescriptions = new HashMap<>();
        }
        
        public void addPrescription(String medicineId, int quantity) {
            prescriptions.put(medicineId, prescriptions.getOrDefault(medicineId, 0) + quantity);
        }
        
        public String getRecordId() { return recordId; }
        public String getPatientId() { return patientId; }
        public String getDoctorId() { return doctorId; }
        public LocalDate getDate() { return date; }
        public String getDiagnosis() { return diagnosis; }
        public String getPrescription() { return prescription; }
        public LocalDateTime getAppointmentTime() { return appointmentTime; }
    }
    
    class Bill {
        private String billId;
        private String patientId;
        private String appointmentId;
        private double consultationFee;
        private double medicineCost;
        private double roomCharges;
        private int days;
        private double tax;
        private double totalAmount;
        private double finalAmount;
        private LocalDate date;
        private String status;
        private double paidAmount;
        private LocalDate paymentDate;
        
        public Bill(String billId, String patientId, String appointmentId, double consultationFee,
                   double medicineCost, double tax, double finalAmount, LocalDate date, String status) {
            this.billId = billId;
            this.patientId = patientId;
            this.appointmentId = appointmentId;
            this.consultationFee = consultationFee;
            this.medicineCost = medicineCost;
            this.tax = tax;
            this.totalAmount = consultationFee + medicineCost;
            this.finalAmount = finalAmount;
            this.date = date;
            this.status = status;
            this.roomCharges = 0.0;
            this.days = 0;
        }
        
        public String getBillId() { return billId; }
        public String getPatientId() { return patientId; }
        public String getAppointmentId() { return appointmentId; }
        public double getConsultationFee() { return consultationFee; }
        public double getMedicineCost() { return medicineCost; }
        public double getRoomCharges() { return roomCharges; }
        public int getDays() { return days; }
        public double getTax() { return tax; }
        public double getTotalAmount() { return totalAmount; }
        public double getFinalAmount() { return finalAmount; }
        public LocalDate getDate() { return date; }
        public String getStatus() { return status; }
        public double getPaidAmount() { return paidAmount; }
        public LocalDate getPaymentDate() { return paymentDate; }
        public void setStatus(String status) { this.status = status; }
        public void setPaidAmount(double amount) { this.paidAmount = amount; }
        public void setPaymentDate(LocalDate date) { this.paymentDate = date; }
        public void setRoomCharges(double charges) { 
            this.roomCharges = charges; 
            this.totalAmount = consultationFee + medicineCost + roomCharges;
            this.finalAmount = totalAmount + tax;
        }
        public void setDays(int days) { this.days = days; }
    }
    
    class Medicine {
        private String medicineId;
        private String name;
        private String description;
        private double price;
        private int stock;
        
        public Medicine(String medicineId, String name, String description, double price, int stock) {
            this.medicineId = medicineId;
            this.name = name;
            this.description = description;
            this.price = price;
            this.stock = stock;
        }
        
        public void reduceStock(int quantity) {
            if (quantity > 0 && stock >= quantity) {
                stock -= quantity;
            }
        }
        
        public String getMedicineId() { return medicineId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public double getPrice() { return price; }
        public int getStock() { return stock; }
    }
    
    public static void main(String[] args) {
        HospitalManagementSystem hms = new HospitalManagementSystem();
        
        // Test operations
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        Appointment appointment = hms.bookAppointment("PAT001", "DOC001", appointmentTime);
        if (appointment != null) {
            hms.completeAppointment(appointment.getAppointmentId(), "Hypertension", "Prescribed medication");
        }
        
        hms.admitPatient("PAT002", "ROOM001", "Surgery", 5);
        hms.generatePatientHistory("PAT001");
        hms.generateHospitalReport();
    }
}
