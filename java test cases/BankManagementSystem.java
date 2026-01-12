import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive Bank Management System
 * This system manages accounts, transactions, loans, and customer information
 */
public class BankManagementSystem {
    
    // Constants
    private static final double MIN_BALANCE = 100.0;
    private static final double MAX_TRANSACTION = 100000.0;
    private static final int MAX_ATTEMPTS = 3;
    
    // Data structures
    private Map<String, Account> accounts;
    private Map<String, Customer> customers;
    private List<Transaction> transactions;
    private Map<String, Loan> loans;
    private Map<String, Employee> employees;
    
    // Statistics
    private int totalTransactions;
    private double totalDeposits;
    private double totalWithdrawals;
    
    public BankManagementSystem() {
        this.accounts = new HashMap<>();
        this.customers = new HashMap<>();
        this.transactions = new ArrayList<>();
        this.loans = new HashMap<>();
        this.employees = new HashMap<>();
        this.totalTransactions = 0;
        this.totalDeposits = 0.0;
        this.totalWithdrawals = 0.0;
        initializeSystem();
    }
    
    private void initializeSystem() {
        // Create sample customers
        Customer customer1 = new Customer("C001", "John Doe", "john.doe@email.com", "1234567890", "123 Main St");
        Customer customer2 = new Customer("C002", "Jane Smith", "jane.smith@email.com", "0987654321", "456 Oak Ave");
        Customer customer3 = new Customer("C003", "Bob Johnson", "bob.j@email.com", "1122334455", "789 Pine Rd");
        
        customers.put("C001", customer1);
        customers.put("C002", customer2);
        customers.put("C003", customer3);
        
        // Create sample accounts
        Account account1 = new SavingsAccount("ACC001", "C001", 5000.0, 2.5);
        Account account2 = new CurrentAccount("ACC002", "C002", 10000.0, 1000.0);
        Account account3 = new FixedDepositAccount("ACC003", "C003", 50000.0, 5.0, 12);
        
        accounts.put("ACC001", account1);
        accounts.put("ACC002", account2);
        accounts.put("ACC003", account3);
        
        // Create sample employees
        Employee emp1 = new Employee("E001", "Alice Manager", "Manager", 75000.0);
        Employee emp2 = new Employee("E002", "Charlie Teller", "Teller", 40000.0);
        employees.put("E001", emp1);
        employees.put("E002", emp2);
    }
    
    public boolean createAccount(String accountId, String customerId, String accountType, double initialBalance) {
        if (accounts.containsKey(accountId)) {
            System.out.println("Account ID already exists: " + accountId);
            return false;
        }
        
        if (!customers.containsKey(customerId)) {
            System.out.println("Customer not found: " + customerId);
            return false;
        }
        
        if (initialBalance < MIN_BALANCE) {
            System.out.println("Initial balance must be at least " + MIN_BALANCE);
            return false;
        }
        
        Account newAccount = null;
        switch (accountType.toLowerCase()) {
            case "savings":
                newAccount = new SavingsAccount(accountId, customerId, initialBalance, 2.5);
                break;
            case "current":
                newAccount = new CurrentAccount(accountId, customerId, initialBalance, 1000.0);
                break;
            case "fixed":
                newAccount = new FixedDepositAccount(accountId, customerId, initialBalance, 5.0, 12);
                break;
            default:
                System.out.println("Invalid account type: " + accountType);
                return false;
        }
        
        accounts.put(accountId, newAccount);
        System.out.println("Account created successfully: " + accountId);
        return true;
    }
    
    public boolean deposit(String accountId, double amount, String employeeId) {
        if (!accounts.containsKey(accountId)) {
            System.out.println("Account not found: " + accountId);
            return false;
        }
        
        if (amount <= 0) {
            System.out.println("Deposit amount must be positive");
            return false;
        }
        
        if (amount > MAX_TRANSACTION) {
            System.out.println("Transaction amount exceeds maximum limit: " + MAX_TRANSACTION);
            return false;
        }
        
        Account account = accounts.get(accountId);
        boolean success = account.deposit(amount);
        
        if (success) {
            Transaction transaction = new Transaction(
                generateTransactionId(),
                accountId,
                "DEPOSIT",
                amount,
                LocalDateTime.now(),
                employeeId
            );
            transactions.add(transaction);
            totalTransactions++;
            totalDeposits += amount;
            System.out.println("Deposit successful. New balance: " + account.getBalance());
            return true;
        }
        
        return false;
    }
    
    public boolean withdraw(String accountId, double amount, String employeeId) {
        if (!accounts.containsKey(accountId)) {
            System.out.println("Account not found: " + accountId);
            return false;
        }
        
        if (amount <= 0) {
            System.out.println("Withdrawal amount must be positive");
            return false;
        }
        
        if (amount > MAX_TRANSACTION) {
            System.out.println("Transaction amount exceeds maximum limit: " + MAX_TRANSACTION);
            return false;
        }
        
        Account account = accounts.get(accountId);
        boolean success = account.withdraw(amount);
        
        if (success) {
            Transaction transaction = new Transaction(
                generateTransactionId(),
                accountId,
                "WITHDRAWAL",
                amount,
                LocalDateTime.now(),
                employeeId
            );
            transactions.add(transaction);
            totalTransactions++;
            totalWithdrawals += amount;
            System.out.println("Withdrawal successful. New balance: " + account.getBalance());
            return true;
        } else {
            System.out.println("Withdrawal failed. Insufficient balance or limit exceeded.");
            return false;
        }
    }
    
    public boolean transfer(String fromAccountId, String toAccountId, double amount, String employeeId) {
        if (!accounts.containsKey(fromAccountId) || !accounts.containsKey(toAccountId)) {
            System.out.println("One or both accounts not found");
            return false;
        }
        
        if (fromAccountId.equals(toAccountId)) {
            System.out.println("Cannot transfer to the same account");
            return false;
        }
        
        if (amount <= 0) {
            System.out.println("Transfer amount must be positive");
            return false;
        }
        
        Account fromAccount = accounts.get(fromAccountId);
        Account toAccount = accounts.get(toAccountId);
        
        // Check if withdrawal is possible
        if (!fromAccount.canWithdraw(amount)) {
            System.out.println("Insufficient balance for transfer");
            return false;
        }
        
        // Perform transfer
        boolean withdrawSuccess = fromAccount.withdraw(amount);
        if (withdrawSuccess) {
            boolean depositSuccess = toAccount.deposit(amount);
            if (depositSuccess) {
                Transaction transaction = new Transaction(
                    generateTransactionId(),
                    fromAccountId,
                    "TRANSFER",
                    amount,
                    LocalDateTime.now(),
                    employeeId,
                    toAccountId
                );
                transactions.add(transaction);
                totalTransactions++;
                System.out.println("Transfer successful from " + fromAccountId + " to " + toAccountId);
                return true;
            } else {
                // Rollback withdrawal if deposit fails
                fromAccount.deposit(amount);
                System.out.println("Transfer failed. Deposit to destination account failed.");
                return false;
            }
        }
        
        return false;
    }
    
    public Loan applyForLoan(String customerId, String loanType, double amount, int tenure) {
        if (!customers.containsKey(customerId)) {
            System.out.println("Customer not found: " + customerId);
            return null;
        }
        
        if (amount <= 0) {
            System.out.println("Loan amount must be positive");
            return null;
        }
        
        if (tenure <= 0 || tenure > 60) {
            System.out.println("Tenure must be between 1 and 60 months");
            return null;
        }
        
        double interestRate = calculateInterestRate(loanType, amount);
        Loan loan = new Loan(
            generateLoanId(),
            customerId,
            loanType,
            amount,
            interestRate,
            tenure,
            LocalDateTime.now()
        );
        
        loans.put(loan.getLoanId(), loan);
        System.out.println("Loan application submitted: " + loan.getLoanId());
        return loan;
    }
    
    private double calculateInterestRate(String loanType, double amount) {
        double baseRate = 0.0;
        switch (loanType.toLowerCase()) {
            case "home":
                baseRate = 8.5;
                break;
            case "car":
                baseRate = 10.0;
                break;
            case "personal":
                baseRate = 12.0;
                break;
            case "education":
                baseRate = 7.5;
                break;
            default:
                baseRate = 15.0;
        }
        
        // Adjust rate based on amount
        if (amount > 1000000) {
            baseRate -= 0.5;
        } else if (amount > 500000) {
            baseRate -= 0.25;
        }
        
        return baseRate;
    }
    
    public boolean processLoanPayment(String loanId, double amount) {
        if (!loans.containsKey(loanId)) {
            System.out.println("Loan not found: " + loanId);
            return false;
        }
        
        Loan loan = loans.get(loanId);
        boolean success = loan.makePayment(amount);
        
        if (success) {
            System.out.println("Payment processed. Remaining balance: " + loan.getRemainingBalance());
            if (loan.isFullyPaid()) {
                System.out.println("Loan fully paid!");
            }
            return true;
        } else {
            System.out.println("Payment failed. Invalid amount or loan already paid.");
            return false;
        }
    }
    
    public void generateAccountStatement(String accountId, int days) {
        if (!accounts.containsKey(accountId)) {
            System.out.println("Account not found: " + accountId);
            return;
        }
        
        Account account = accounts.get(accountId);
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        
        System.out.println("\n=== Account Statement ===");
        System.out.println("Account ID: " + accountId);
        System.out.println("Customer ID: " + account.getCustomerId());
        System.out.println("Current Balance: " + account.getBalance());
        System.out.println("Statement Period: Last " + days + " days");
        System.out.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("\nTransactions:");
        System.out.println("Date\t\tType\t\tAmount\t\tEmployee");
        System.out.println("---------------------------------------------------");
        
        int transactionCount = 0;
        for (Transaction transaction : transactions) {
            if (transaction.getAccountId().equals(accountId) && 
                transaction.getTimestamp().isAfter(cutoffDate)) {
                System.out.println(
                    transaction.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\t" +
                    transaction.getType() + "\t" +
                    transaction.getAmount() + "\t\t" +
                    transaction.getEmployeeId()
                );
                transactionCount++;
            }
        }
        
        System.out.println("\nTotal Transactions: " + transactionCount);
        System.out.println("=== End of Statement ===\n");
    }
    
    public void calculateInterest() {
        System.out.println("\n=== Calculating Interest ===");
        for (Account account : accounts.values()) {
            if (account instanceof SavingsAccount) {
                SavingsAccount savingsAccount = (SavingsAccount) account;
                double interest = savingsAccount.calculateInterest();
                savingsAccount.deposit(interest);
                System.out.println("Interest calculated for " + account.getAccountId() + ": " + interest);
            } else if (account instanceof FixedDepositAccount) {
                FixedDepositAccount fdAccount = (FixedDepositAccount) account;
                double interest = fdAccount.calculateInterest();
                fdAccount.deposit(interest);
                System.out.println("Interest calculated for " + account.getAccountId() + ": " + interest);
            }
        }
        System.out.println("=== Interest Calculation Complete ===\n");
    }
    
    public void generateReports() {
        System.out.println("\n=== Bank Reports ===");
        System.out.println("Total Accounts: " + accounts.size());
        System.out.println("Total Customers: " + customers.size());
        System.out.println("Total Transactions: " + totalTransactions);
        System.out.println("Total Deposits: " + totalDeposits);
        System.out.println("Total Withdrawals: " + totalWithdrawals);
        System.out.println("Total Loans: " + loans.size());
        System.out.println("Total Employees: " + employees.size());
        
        double totalBalance = 0.0;
        for (Account account : accounts.values()) {
            totalBalance += account.getBalance();
        }
        System.out.println("Total Bank Balance: " + totalBalance);
        
        double totalLoanAmount = 0.0;
        for (Loan loan : loans.values()) {
            totalLoanAmount += loan.getRemainingBalance();
        }
        System.out.println("Total Outstanding Loans: " + totalLoanAmount);
        System.out.println("=== End of Reports ===\n");
    }
    
    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }
    
    public Customer getCustomer(String customerId) {
        return customers.get(customerId);
    }
    
    public List<Transaction> getTransactions(String accountId) {
        List<Transaction> accountTransactions = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction.getAccountId().equals(accountId)) {
                accountTransactions.add(transaction);
            }
        }
        return accountTransactions;
    }
    
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generateLoanId() {
        return "LOAN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    // Nested classes
    abstract class Account {
        protected String accountId;
        protected String customerId;
        protected double balance;
        protected LocalDateTime createdAt;
        protected List<String> transactionHistory;
        
        public Account(String accountId, String customerId, double balance) {
            this.accountId = accountId;
            this.customerId = customerId;
            this.balance = balance;
            this.createdAt = LocalDateTime.now();
            this.transactionHistory = new ArrayList<>();
        }
        
        public boolean deposit(double amount) {
            if (amount > 0) {
                balance += amount;
                transactionHistory.add("DEPOSIT: " + amount + " at " + LocalDateTime.now());
                return true;
            }
            return false;
        }
        
        public abstract boolean withdraw(double amount);
        
        public boolean canWithdraw(double amount) {
            return withdraw(amount, true);
        }
        
        protected abstract boolean withdraw(double amount, boolean checkOnly);
        
        public double getBalance() {
            return balance;
        }
        
        public String getAccountId() {
            return accountId;
        }
        
        public String getCustomerId() {
            return customerId;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public List<String> getTransactionHistory() {
            return new ArrayList<>(transactionHistory);
        }
        
        protected void addToHistory(String transaction) {
            transactionHistory.add(transaction);
        }
    }
    
    class SavingsAccount extends Account {
        private double interestRate;
        private double minBalance;
        
        public SavingsAccount(String accountId, String customerId, double balance, double interestRate) {
            super(accountId, customerId, balance);
            this.interestRate = interestRate;
            this.minBalance = MIN_BALANCE;
        }
        
        @Override
        public boolean withdraw(double amount) {
            return withdraw(amount, false);
        }
        
        @Override
        protected boolean withdraw(double amount, boolean checkOnly) {
            if (balance - amount >= minBalance) {
                if (!checkOnly) {
                    balance -= amount;
                    addToHistory("WITHDRAWAL: " + amount + " at " + LocalDateTime.now());
                }
                return true;
            }
            return false;
        }
        
        public double calculateInterest() {
            return balance * (interestRate / 100) / 12; // Monthly interest
        }
        
        public double getInterestRate() {
            return interestRate;
        }
    }
    
    class CurrentAccount extends Account {
        private double overdraftLimit;
        
        public CurrentAccount(String accountId, String customerId, double balance, double overdraftLimit) {
            super(accountId, customerId, balance);
            this.overdraftLimit = overdraftLimit;
        }
        
        @Override
        public boolean withdraw(double amount) {
            return withdraw(amount, false);
        }
        
        @Override
        protected boolean withdraw(double amount, boolean checkOnly) {
            if (balance - amount >= -overdraftLimit) {
                if (!checkOnly) {
                    balance -= amount;
                    addToHistory("WITHDRAWAL: " + amount + " at " + LocalDateTime.now());
                }
                return true;
            }
            return false;
        }
        
        public double getOverdraftLimit() {
            return overdraftLimit;
        }
    }
    
    class FixedDepositAccount extends Account {
        private double interestRate;
        private int tenureMonths;
        private LocalDateTime maturityDate;
        private boolean isMatured;
        
        public FixedDepositAccount(String accountId, String customerId, double balance, 
                                   double interestRate, int tenureMonths) {
            super(accountId, customerId, balance);
            this.interestRate = interestRate;
            this.tenureMonths = tenureMonths;
            this.maturityDate = createdAt.plusMonths(tenureMonths);
            this.isMatured = false;
        }
        
        @Override
        public boolean withdraw(double amount) {
            if (isMatured || LocalDateTime.now().isAfter(maturityDate)) {
                isMatured = true;
                return withdraw(amount, false);
            }
            System.out.println("Fixed deposit has not matured yet. Maturity date: " + maturityDate);
            return false;
        }
        
        @Override
        protected boolean withdraw(double amount, boolean checkOnly) {
            if (isMatured && balance >= amount) {
                if (!checkOnly) {
                    balance -= amount;
                    addToHistory("WITHDRAWAL: " + amount + " at " + LocalDateTime.now());
                }
                return true;
            }
            return false;
        }
        
        public double calculateInterest() {
            if (!isMatured && LocalDateTime.now().isAfter(maturityDate)) {
                isMatured = true;
            }
            return balance * (interestRate / 100) * (tenureMonths / 12.0);
        }
        
        public LocalDateTime getMaturityDate() {
            return maturityDate;
        }
        
        public boolean isMatured() {
            return isMatured || LocalDateTime.now().isAfter(maturityDate);
        }
    }
    
    class Customer {
        private String customerId;
        private String name;
        private String email;
        private String phone;
        private String address;
        private LocalDateTime registrationDate;
        
        public Customer(String customerId, String name, String email, String phone, String address) {
            this.customerId = customerId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.address = address;
            this.registrationDate = LocalDateTime.now();
        }
        
        public String getCustomerId() { return customerId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getAddress() { return address; }
        public LocalDateTime getRegistrationDate() { return registrationDate; }
        
        public void updateEmail(String newEmail) {
            this.email = newEmail;
        }
        
        public void updatePhone(String newPhone) {
            this.phone = newPhone;
        }
        
        public void updateAddress(String newAddress) {
            this.address = newAddress;
        }
    }
    
    class Transaction {
        private String transactionId;
        private String accountId;
        private String type;
        private double amount;
        private LocalDateTime timestamp;
        private String employeeId;
        private String toAccountId;
        
        public Transaction(String transactionId, String accountId, String type, 
                          double amount, LocalDateTime timestamp, String employeeId) {
            this.transactionId = transactionId;
            this.accountId = accountId;
            this.type = type;
            this.amount = amount;
            this.timestamp = timestamp;
            this.employeeId = employeeId;
        }
        
        public Transaction(String transactionId, String accountId, String type, 
                          double amount, LocalDateTime timestamp, String employeeId, String toAccountId) {
            this(transactionId, accountId, type, amount, timestamp, employeeId);
            this.toAccountId = toAccountId;
        }
        
        public String getTransactionId() { return transactionId; }
        public String getAccountId() { return accountId; }
        public String getType() { return type; }
        public double getAmount() { return amount; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getEmployeeId() { return employeeId; }
        public String getToAccountId() { return toAccountId; }
    }
    
    class Loan {
        private String loanId;
        private String customerId;
        private String loanType;
        private double principalAmount;
        private double interestRate;
        private int tenureMonths;
        private LocalDateTime startDate;
        private double remainingBalance;
        private List<Payment> payments;
        private boolean isApproved;
        
        public Loan(String loanId, String customerId, String loanType, double principalAmount,
                   double interestRate, int tenureMonths, LocalDateTime startDate) {
            this.loanId = loanId;
            this.customerId = customerId;
            this.loanType = loanType;
            this.principalAmount = principalAmount;
            this.interestRate = interestRate;
            this.tenureMonths = tenureMonths;
            this.startDate = startDate;
            this.remainingBalance = principalAmount * (1 + (interestRate / 100) * (tenureMonths / 12.0));
            this.payments = new ArrayList<>();
            this.isApproved = false;
            approveLoan();
        }
        
        private void approveLoan() {
            // Simple approval logic
            if (principalAmount <= 5000000) {
                isApproved = true;
            }
        }
        
        public boolean makePayment(double amount) {
            if (!isApproved) {
                return false;
            }
            
            if (remainingBalance <= 0) {
                return false;
            }
            
            if (amount > remainingBalance) {
                amount = remainingBalance;
            }
            
            remainingBalance -= amount;
            Payment payment = new Payment(amount, LocalDateTime.now());
            payments.add(payment);
            return true;
        }
        
        public double getRemainingBalance() {
            return remainingBalance;
        }
        
        public boolean isFullyPaid() {
            return remainingBalance <= 0.01; // Allow small floating point difference
        }
        
        public String getLoanId() { return loanId; }
        public String getCustomerId() { return customerId; }
        public String getLoanType() { return loanType; }
        public double getPrincipalAmount() { return principalAmount; }
        public double getInterestRate() { return interestRate; }
        public int getTenureMonths() { return tenureMonths; }
        public List<Payment> getPayments() { return new ArrayList<>(payments); }
        public boolean isApproved() { return isApproved; }
        
        class Payment {
            private double amount;
            private LocalDateTime paymentDate;
            
            public Payment(double amount, LocalDateTime paymentDate) {
                this.amount = amount;
                this.paymentDate = paymentDate;
            }
            
            public double getAmount() { return amount; }
            public LocalDateTime getPaymentDate() { return paymentDate; }
        }
    }
    
    class Employee {
        private String employeeId;
        private String name;
        private String position;
        private double salary;
        private LocalDateTime hireDate;
        
        public Employee(String employeeId, String name, String position, double salary) {
            this.employeeId = employeeId;
            this.name = name;
            this.position = position;
            this.salary = salary;
            this.hireDate = LocalDateTime.now();
        }
        
        public String getEmployeeId() { return employeeId; }
        public String getName() { return name; }
        public String getPosition() { return position; }
        public double getSalary() { return salary; }
        public LocalDateTime getHireDate() { return hireDate; }
        
        public void promote(String newPosition, double newSalary) {
            this.position = newPosition;
            this.salary = newSalary;
        }
    }
    
    // Main method for testing
    public static void main(String[] args) {
        BankManagementSystem bank = new BankManagementSystem();
        
        // Test operations
        bank.deposit("ACC001", 1000.0, "E001");
        bank.withdraw("ACC002", 500.0, "E002");
        bank.transfer("ACC001", "ACC002", 200.0, "E001");
        bank.generateAccountStatement("ACC001", 30);
        bank.calculateInterest();
        bank.generateReports();
        
        Loan loan = bank.applyForLoan("C001", "home", 500000.0, 20);
        if (loan != null) {
            bank.processLoanPayment(loan.getLoanId(), 10000.0);
        }
    }
}
