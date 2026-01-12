import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive Library Management System
 * Manages books, members, loans, reservations, and library operations
 */
public class LibraryManagementSystem {
    
    // Constants
    private static final int MAX_BOOKS_PER_MEMBER = 5;
    private static final int LOAN_DURATION_DAYS = 14;
    private static final double DAILY_FINE = 5.0;
    private static final double MAX_FINE = 1000.0;
    
    // Data structures
    private Map<String, Book> books;
    private Map<String, Member> members;
    private Map<String, Author> authors;
    private Map<String, Publisher> publishers;
    private Map<String, Category> categories;
    private List<Loan> loans;
    private List<Reservation> reservations;
    private List<Fine> fines;
    
    // Statistics
    private int totalBooks;
    private int totalMembers;
    private int totalLoans;
    private double totalFines;
    
    public LibraryManagementSystem() {
        this.books = new HashMap<>();
        this.members = new HashMap<>();
        this.authors = new HashMap<>();
        this.publishers = new HashMap<>();
        this.categories = new HashMap<>();
        this.loans = new ArrayList<>();
        this.reservations = new ArrayList<>();
        this.fines = new ArrayList<>();
        this.totalBooks = 0;
        this.totalMembers = 0;
        this.totalLoans = 0;
        this.totalFines = 0.0;
        initializeSystem();
    }
    
    private void initializeSystem() {
        // Create categories
        Category fiction = new Category("CAT001", "Fiction", "Fictional literature");
        Category science = new Category("CAT002", "Science", "Science and technology books");
        Category history = new Category("CAT003", "History", "Historical books");
        Category programming = new Category("CAT004", "Programming", "Computer programming books");
        
        categories.put("CAT001", fiction);
        categories.put("CAT002", science);
        categories.put("CAT003", history);
        categories.put("CAT004", programming);
        
        // Create publishers
        Publisher publisher1 = new Publisher("PUB001", "Tech Books Publishing", "tech@pub.com");
        Publisher publisher2 = new Publisher("PUB002", "Classic Literature Press", "classic@pub.com");
        Publisher publisher3 = new Publisher("PUB003", "Science Publications", "science@pub.com");
        
        publishers.put("PUB001", publisher1);
        publishers.put("PUB002", publisher2);
        publishers.put("PUB003", publisher3);
        
        // Create authors
        Author author1 = new Author("AUTH001", "John Smith", "Renowned programming expert");
        Author author2 = new Author("AUTH002", "Jane Doe", "Award-winning novelist");
        Author author3 = new Author("AUTH003", "Michael Chen", "Science writer and researcher");
        
        authors.put("AUTH001", author1);
        authors.put("AUTH002", author2);
        authors.put("AUTH003", author3);
        
        // Create books
        Book book1 = new Book("BOOK001", "Java Programming", "CAT004", "AUTH001", "PUB001",
                              "978-0123456789", 2020, 500, 10);
        Book book2 = new Book("BOOK002", "The Great Novel", "CAT001", "AUTH002", "PUB002",
                              "978-0987654321", 2019, 300, 5);
        Book book3 = new Book("BOOK003", "Introduction to Physics", "CAT002", "AUTH003", "PUB003",
                              "978-1122334455", 2021, 400, 8);
        Book book4 = new Book("BOOK004", "World History", "CAT003", "AUTH003", "PUB003",
                              "978-5566778899", 2018, 600, 12);
        Book book5 = new Book("BOOK005", "Advanced Algorithms", "CAT004", "AUTH001", "PUB001",
                              "978-9988776655", 2022, 450, 6);
        
        books.put("BOOK001", book1);
        books.put("BOOK002", book2);
        books.put("BOOK003", book3);
        books.put("BOOK004", book4);
        books.put("BOOK005", book5);
        totalBooks = books.size();
        
        // Create members
        Member member1 = new Member("MEM001", "Alice Johnson", "alice@email.com", "1234567890",
                                    LocalDate.of(2020, 1, 15));
        Member member2 = new Member("MEM002", "Bob Williams", "bob@email.com", "0987654321",
                                    LocalDate.of(2021, 3, 20));
        Member member3 = new Member("MEM003", "Carol Brown", "carol@email.com", "1122334455",
                                    LocalDate.of(2020, 6, 10));
        
        members.put("MEM001", member1);
        members.put("MEM002", member2);
        members.put("MEM003", member3);
        totalMembers = members.size();
    }
    
    public boolean registerMember(String memberId, String name, String email, String phone, LocalDate registrationDate) {
        if (members.containsKey(memberId)) {
            System.out.println("Member ID already exists: " + memberId);
            return false;
        }
        
        Member member = new Member(memberId, name, email, phone, registrationDate);
        members.put(memberId, member);
        totalMembers++;
        System.out.println("Member registered: " + memberId);
        return true;
    }
    
    public boolean addBook(String bookId, String title, String categoryId, String authorId,
                          String publisherId, String isbn, int publicationYear, int pages, int copies) {
        if (books.containsKey(bookId)) {
            System.out.println("Book ID already exists: " + bookId);
            return false;
        }
        
        if (!categories.containsKey(categoryId)) {
            System.out.println("Category not found: " + categoryId);
            return false;
        }
        
        if (!authors.containsKey(authorId)) {
            System.out.println("Author not found: " + authorId);
            return false;
        }
        
        if (!publishers.containsKey(publisherId)) {
            System.out.println("Publisher not found: " + publisherId);
            return false;
        }
        
        if (copies <= 0) {
            System.out.println("Number of copies must be positive");
            return false;
        }
        
        Book book = new Book(bookId, title, categoryId, authorId, publisherId, isbn,
                            publicationYear, pages, copies);
        books.put(bookId, book);
        totalBooks++;
        System.out.println("Book added: " + title);
        return true;
    }
    
    public Loan borrowBook(String memberId, String bookId) {
        if (!members.containsKey(memberId)) {
            System.out.println("Member not found: " + memberId);
            return null;
        }
        
        if (!books.containsKey(bookId)) {
            System.out.println("Book not found: " + bookId);
            return null;
        }
        
        Member member = members.get(memberId);
        Book book = books.get(bookId);
        
        // Check member's loan limit
        int currentLoans = countActiveLoans(memberId);
        if (currentLoans >= MAX_BOOKS_PER_MEMBER) {
            System.out.println("Member has reached maximum loan limit");
            return null;
        }
        
        // Check if book is available
        if (book.getAvailableCopies() <= 0) {
            System.out.println("Book is not available");
            return null;
        }
        
        // Check for existing active loan
        if (hasActiveLoan(memberId, bookId)) {
            System.out.println("Member already has an active loan for this book");
            return null;
        }
        
        LocalDate loanDate = LocalDate.now();
        LocalDate dueDate = loanDate.plusDays(LOAN_DURATION_DAYS);
        
        String loanId = generateLoanId();
        Loan loan = new Loan(loanId, memberId, bookId, loanDate, dueDate, "ACTIVE");
        loans.add(loan);
        totalLoans++;
        
        book.reduceAvailableCopies();
        member.incrementLoanedBooks();
        
        System.out.println("Book loaned: " + book.getTitle() + " to " + member.getName() + 
                          ". Due date: " + dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        return loan;
    }
    
    public boolean returnBook(String loanId) {
        Loan loan = findLoan(loanId);
        if (loan == null) {
            System.out.println("Loan not found: " + loanId);
            return false;
        }
        
        if (!loan.getStatus().equals("ACTIVE")) {
            System.out.println("Loan is not active");
            return false;
        }
        
        Book book = books.get(loan.getBookId());
        Member member = members.get(loan.getMemberId());
        
        LocalDate returnDate = LocalDate.now();
        loan.setReturnDate(returnDate);
        loan.setStatus("RETURNED");
        
        book.increaseAvailableCopies();
        member.decrementLoanedBooks();
        
        // Calculate fine if overdue
        if (returnDate.isAfter(loan.getDueDate())) {
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
            double fineAmount = Math.min(daysOverdue * DAILY_FINE, MAX_FINE);
            
            String fineId = generateFineId();
            Fine fine = new Fine(fineId, loanId, loan.getMemberId(), fineAmount, 
                                LocalDate.now(), "PENDING");
            fines.add(fine);
            totalFines += fineAmount;
            
            System.out.println("Book returned. Fine for overdue: " + fineAmount);
        } else {
            System.out.println("Book returned on time");
        }
        
        return true;
    }
    
    public Reservation reserveBook(String memberId, String bookId) {
        if (!members.containsKey(memberId)) {
            System.out.println("Member not found: " + memberId);
            return null;
        }
        
        if (!books.containsKey(bookId)) {
            System.out.println("Book not found: " + bookId);
            return null;
        }
        
        Book book = books.get(bookId);
        
        if (book.getAvailableCopies() > 0) {
            System.out.println("Book is available. No reservation needed");
            return null;
        }
        
        // Check for existing reservation
        for (Reservation reservation : reservations) {
            if (reservation.getMemberId().equals(memberId) && 
                reservation.getBookId().equals(bookId) &&
                reservation.getStatus().equals("PENDING")) {
                System.out.println("Member already has a pending reservation for this book");
                return null;
            }
        }
        
        String reservationId = generateReservationId();
        Reservation reservation = new Reservation(reservationId, memberId, bookId, 
                                                  LocalDate.now(), "PENDING");
        reservations.add(reservation);
        
        System.out.println("Book reserved: " + book.getTitle());
        return reservation;
    }
    
    public boolean payFine(String fineId, double amount) {
        Fine fine = findFine(fineId);
        if (fine == null) {
            System.out.println("Fine not found: " + fineId);
            return false;
        }
        
        if (!fine.getStatus().equals("PENDING")) {
            System.out.println("Fine already paid");
            return false;
        }
        
        if (amount < fine.getAmount()) {
            System.out.println("Insufficient payment. Required: " + fine.getAmount());
            return false;
        }
        
        fine.setStatus("PAID");
        fine.setPaidDate(LocalDate.now());
        fine.setPaidAmount(amount);
        
        System.out.println("Fine paid: " + fineId);
        return true;
    }
    
    public void generateMemberReport(String memberId) {
        if (!members.containsKey(memberId)) {
            System.out.println("Member not found: " + memberId);
            return;
        }
        
        Member member = members.get(memberId);
        System.out.println("\n=== Member Report ===");
        System.out.println("Member ID: " + memberId);
        System.out.println("Name: " + member.getName());
        System.out.println("Email: " + member.getEmail());
        System.out.println("Registration Date: " + member.getRegistrationDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        System.out.println("Books Currently Loaned: " + member.getLoanedBooksCount());
        
        System.out.println("\nActive Loans:");
        System.out.println("Loan ID\t\tBook Title\t\tLoan Date\t\tDue Date");
        System.out.println("---------------------------------------------------------------");
        
        for (Loan loan : loans) {
            if (loan.getMemberId().equals(memberId) && loan.getStatus().equals("ACTIVE")) {
                Book book = books.get(loan.getBookId());
                System.out.println(
                    loan.getLoanId() + "\t" +
                    book.getTitle() + "\t\t" +
                    loan.getLoanDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\t" +
                    loan.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                );
            }
        }
        
        System.out.println("\nPending Fines:");
        System.out.println("Fine ID\t\tAmount\t\tDate");
        System.out.println("---------------------------------------------------");
        
        double totalPendingFines = 0.0;
        for (Fine fine : fines) {
            if (fine.getMemberId().equals(memberId) && fine.getStatus().equals("PENDING")) {
                System.out.println(
                    fine.getFineId() + "\t" +
                    fine.getAmount() + "\t\t" +
                    fine.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                );
                totalPendingFines += fine.getAmount();
            }
        }
        
        System.out.println("\nTotal Pending Fines: " + totalPendingFines);
        System.out.println("=== End of Report ===\n");
    }
    
    public void generateLibraryStatistics() {
        System.out.println("\n=== Library Statistics ===");
        System.out.println("Total Books: " + totalBooks);
        System.out.println("Total Members: " + totalMembers);
        System.out.println("Total Loans: " + totalLoans);
        System.out.println("Total Fines: " + totalFines);
        
        int activeLoans = 0;
        for (Loan loan : loans) {
            if (loan.getStatus().equals("ACTIVE")) {
                activeLoans++;
            }
        }
        System.out.println("Active Loans: " + activeLoans);
        
        int pendingReservations = 0;
        for (Reservation reservation : reservations) {
            if (reservation.getStatus().equals("PENDING")) {
                pendingReservations++;
            }
        }
        System.out.println("Pending Reservations: " + pendingReservations);
        
        double totalPendingFines = 0.0;
        for (Fine fine : fines) {
            if (fine.getStatus().equals("PENDING")) {
                totalPendingFines += fine.getAmount();
            }
        }
        System.out.println("Total Pending Fines: " + totalPendingFines);
        
        System.out.println("\nBooks by Category:");
        Map<String, Integer> booksByCategory = new HashMap<>();
        for (Book book : books.values()) {
            String categoryId = book.getCategoryId();
            booksByCategory.put(categoryId, booksByCategory.getOrDefault(categoryId, 0) + 1);
        }
        for (Map.Entry<String, Integer> entry : booksByCategory.entrySet()) {
            Category category = categories.get(entry.getKey());
            System.out.println("  " + category.getName() + ": " + entry.getValue() + " books");
        }
        
        System.out.println("=== End of Statistics ===\n");
    }
    
    private int countActiveLoans(String memberId) {
        int count = 0;
        for (Loan loan : loans) {
            if (loan.getMemberId().equals(memberId) && loan.getStatus().equals("ACTIVE")) {
                count++;
            }
        }
        return count;
    }
    
    private boolean hasActiveLoan(String memberId, String bookId) {
        for (Loan loan : loans) {
            if (loan.getMemberId().equals(memberId) && 
                loan.getBookId().equals(bookId) &&
                loan.getStatus().equals("ACTIVE")) {
                return true;
            }
        }
        return false;
    }
    
    private Loan findLoan(String loanId) {
        for (Loan loan : loans) {
            if (loan.getLoanId().equals(loanId)) {
                return loan;
            }
        }
        return null;
    }
    
    private Fine findFine(String fineId) {
        for (Fine fine : fines) {
            if (fine.getFineId().equals(fineId)) {
                return fine;
            }
        }
        return null;
    }
    
    private String generateLoanId() {
        return "LOAN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generateReservationId() {
        return "RES" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generateFineId() {
        return "FINE" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    // Nested classes
    class Book {
        private String bookId;
        private String title;
        private String categoryId;
        private String authorId;
        private String publisherId;
        private String isbn;
        private int publicationYear;
        private int pages;
        private int totalCopies;
        private int availableCopies;
        
        public Book(String bookId, String title, String categoryId, String authorId,
                   String publisherId, String isbn, int publicationYear, int pages, int copies) {
            this.bookId = bookId;
            this.title = title;
            this.categoryId = categoryId;
            this.authorId = authorId;
            this.publisherId = publisherId;
            this.isbn = isbn;
            this.publicationYear = publicationYear;
            this.pages = pages;
            this.totalCopies = copies;
            this.availableCopies = copies;
        }
        
        public void reduceAvailableCopies() {
            if (availableCopies > 0) {
                availableCopies--;
            }
        }
        
        public void increaseAvailableCopies() {
            if (availableCopies < totalCopies) {
                availableCopies++;
            }
        }
        
        public String getBookId() { return bookId; }
        public String getTitle() { return title; }
        public String getCategoryId() { return categoryId; }
        public String getAuthorId() { return authorId; }
        public String getPublisherId() { return publisherId; }
        public String getIsbn() { return isbn; }
        public int getPublicationYear() { return publicationYear; }
        public int getPages() { return pages; }
        public int getTotalCopies() { return totalCopies; }
        public int getAvailableCopies() { return availableCopies; }
    }
    
    class Member {
        private String memberId;
        private String name;
        private String email;
        private String phone;
        private LocalDate registrationDate;
        private int loanedBooksCount;
        
        public Member(String memberId, String name, String email, String phone, LocalDate registrationDate) {
            this.memberId = memberId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.registrationDate = registrationDate;
            this.loanedBooksCount = 0;
        }
        
        public void incrementLoanedBooks() {
            loanedBooksCount++;
        }
        
        public void decrementLoanedBooks() {
            if (loanedBooksCount > 0) {
                loanedBooksCount--;
            }
        }
        
        public String getMemberId() { return memberId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public LocalDate getRegistrationDate() { return registrationDate; }
        public int getLoanedBooksCount() { return loanedBooksCount; }
    }
    
    class Author {
        private String authorId;
        private String name;
        private String biography;
        
        public Author(String authorId, String name, String biography) {
            this.authorId = authorId;
            this.name = name;
            this.biography = biography;
        }
        
        public String getAuthorId() { return authorId; }
        public String getName() { return name; }
        public String getBiography() { return biography; }
    }
    
    class Publisher {
        private String publisherId;
        private String name;
        private String email;
        
        public Publisher(String publisherId, String name, String email) {
            this.publisherId = publisherId;
            this.name = name;
            this.email = email;
        }
        
        public String getPublisherId() { return publisherId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
    }
    
    class Category {
        private String categoryId;
        private String name;
        private String description;
        
        public Category(String categoryId, String name, String description) {
            this.categoryId = categoryId;
            this.name = name;
            this.description = description;
        }
        
        public String getCategoryId() { return categoryId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    class Loan {
        private String loanId;
        private String memberId;
        private String bookId;
        private LocalDate loanDate;
        private LocalDate dueDate;
        private LocalDate returnDate;
        private String status;
        
        public Loan(String loanId, String memberId, String bookId, LocalDate loanDate,
                   LocalDate dueDate, String status) {
            this.loanId = loanId;
            this.memberId = memberId;
            this.bookId = bookId;
            this.loanDate = loanDate;
            this.dueDate = dueDate;
            this.status = status;
            this.returnDate = null;
        }
        
        public String getLoanId() { return loanId; }
        public String getMemberId() { return memberId; }
        public String getBookId() { return bookId; }
        public LocalDate getLoanDate() { return loanDate; }
        public LocalDate getDueDate() { return dueDate; }
        public LocalDate getReturnDate() { return returnDate; }
        public String getStatus() { return status; }
        public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
        public void setStatus(String status) { this.status = status; }
    }
    
    class Reservation {
        private String reservationId;
        private String memberId;
        private String bookId;
        private LocalDate reservationDate;
        private String status;
        
        public Reservation(String reservationId, String memberId, String bookId,
                          LocalDate reservationDate, String status) {
            this.reservationId = reservationId;
            this.memberId = memberId;
            this.bookId = bookId;
            this.reservationDate = reservationDate;
            this.status = status;
        }
        
        public String getReservationId() { return reservationId; }
        public String getMemberId() { return memberId; }
        public String getBookId() { return bookId; }
        public LocalDate getReservationDate() { return reservationDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    class Fine {
        private String fineId;
        private String loanId;
        private String memberId;
        private double amount;
        private LocalDate date;
        private String status;
        private double paidAmount;
        private LocalDate paidDate;
        
        public Fine(String fineId, String loanId, String memberId, double amount,
                   LocalDate date, String status) {
            this.fineId = fineId;
            this.loanId = loanId;
            this.memberId = memberId;
            this.amount = amount;
            this.date = date;
            this.status = status;
        }
        
        public String getFineId() { return fineId; }
        public String getLoanId() { return loanId; }
        public String getMemberId() { return memberId; }
        public double getAmount() { return amount; }
        public LocalDate getDate() { return date; }
        public String getStatus() { return status; }
        public double getPaidAmount() { return paidAmount; }
        public LocalDate getPaidDate() { return paidDate; }
        public void setStatus(String status) { this.status = status; }
        public void setPaidAmount(double amount) { this.paidAmount = amount; }
        public void setPaidDate(LocalDate date) { this.paidDate = date; }
    }
    
    public static void main(String[] args) {
        LibraryManagementSystem library = new LibraryManagementSystem();
        
        // Test operations
        Loan loan = library.borrowBook("MEM001", "BOOK001");
        if (loan != null) {
            library.returnBook(loan.getLoanId());
        }
        
        library.reserveBook("MEM002", "BOOK002");
        library.generateMemberReport("MEM001");
        library.generateLibraryStatistics();
    }
}
