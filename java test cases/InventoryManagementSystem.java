import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive Inventory Management System
 * Manages products, suppliers, orders, and stock levels
 */
public class InventoryManagementSystem {
    
    // Constants
    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int MAX_QUANTITY_PER_ORDER = 10000;
    private static final double TAX_RATE = 0.10;
    
    // Data structures
    private Map<String, Product> products;
    private Map<String, Supplier> suppliers;
    private Map<String, Customer> customers;
    private List<Order> orders;
    private List<PurchaseOrder> purchaseOrders;
    private Map<String, Category> categories;
    private List<Transaction> transactions;
    
    // Statistics
    private double totalSales;
    private double totalPurchases;
    private int totalOrders;
    
    public InventoryManagementSystem() {
        this.products = new HashMap<>();
        this.suppliers = new HashMap<>();
        this.customers = new HashMap<>();
        this.orders = new ArrayList<>();
        this.purchaseOrders = new ArrayList<>();
        this.categories = new HashMap<>();
        this.transactions = new ArrayList<>();
        this.totalSales = 0.0;
        this.totalPurchases = 0.0;
        this.totalOrders = 0;
        initializeSystem();
    }
    
    private void initializeSystem() {
        // Create categories
        Category electronics = new Category("CAT001", "Electronics", "Electronic devices and components");
        Category clothing = new Category("CAT002", "Clothing", "Apparel and accessories");
        Category food = new Category("CAT003", "Food", "Food and beverages");
        Category furniture = new Category("CAT004", "Furniture", "Furniture and home decor");
        
        categories.put("CAT001", electronics);
        categories.put("CAT002", clothing);
        categories.put("CAT003", food);
        categories.put("CAT004", furniture);
        
        // Create suppliers
        Supplier supplier1 = new Supplier("SUP001", "Tech Corp", "tech@email.com", "1234567890", "Supplier Street 1");
        Supplier supplier2 = new Supplier("SUP002", "Fashion House", "fashion@email.com", "0987654321", "Fashion Avenue 2");
        Supplier supplier3 = new Supplier("SUP003", "Food Distributors", "food@email.com", "1122334455", "Food Lane 3");
        
        suppliers.put("SUP001", supplier1);
        suppliers.put("SUP002", supplier2);
        suppliers.put("SUP003", supplier3);
        
        // Create customers
        Customer customer1 = new Customer("CUST001", "Retail Store A", "storea@email.com", "1111111111");
        Customer customer2 = new Customer("CUST002", "Retail Store B", "storeb@email.com", "2222222222");
        Customer customer3 = new Customer("CUST003", "Online Shop C", "shopc@email.com", "3333333333");
        
        customers.put("CUST001", customer1);
        customers.put("CUST002", customer2);
        customers.put("CUST003", customer3);
        
        // Create products
        Product product1 = new Product("PROD001", "Laptop", "CAT001", 999.99, 50, 100.0, "SUP001");
        Product product2 = new Product("PROD002", "T-Shirt", "CAT002", 29.99, 200, 10.0, "SUP002");
        Product product3 = new Product("PROD003", "Coffee", "CAT003", 15.99, 150, 8.0, "SUP003");
        Product product4 = new Product("PROD004", "Desk Chair", "CAT004", 199.99, 30, 120.0, "SUP001");
        Product product5 = new Product("PROD005", "Smartphone", "CAT001", 699.99, 75, 500.0, "SUP001");
        
        products.put("PROD001", product1);
        products.put("PROD002", product2);
        products.put("PROD003", product3);
        products.put("PROD004", product4);
        products.put("PROD005", product5);
    }
    
    public boolean addProduct(String productId, String name, String categoryId, 
                             double sellingPrice, int initialStock, double costPrice, String supplierId) {
        if (products.containsKey(productId)) {
            System.out.println("Product ID already exists: " + productId);
            return false;
        }
        
        if (!categories.containsKey(categoryId)) {
            System.out.println("Category not found: " + categoryId);
            return false;
        }
        
        if (!suppliers.containsKey(supplierId)) {
            System.out.println("Supplier not found: " + supplierId);
            return false;
        }
        
        if (sellingPrice <= 0 || costPrice <= 0) {
            System.out.println("Prices must be positive");
            return false;
        }
        
        Product product = new Product(productId, name, categoryId, sellingPrice, initialStock, costPrice, supplierId);
        products.put(productId, product);
        System.out.println("Product added: " + productId);
        return true;
    }
    
    public boolean updateStock(String productId, int quantity, String type) {
        if (!products.containsKey(productId)) {
            System.out.println("Product not found: " + productId);
            return false;
        }
        
        Product product = products.get(productId);
        
        if (type.equals("ADD")) {
            product.addStock(quantity);
            System.out.println("Stock added. New quantity: " + product.getStockQuantity());
            return true;
        } else if (type.equals("REMOVE")) {
            boolean success = product.removeStock(quantity);
            if (success) {
                System.out.println("Stock removed. New quantity: " + product.getStockQuantity());
                return true;
            } else {
                System.out.println("Insufficient stock");
                return false;
            }
        }
        
        return false;
    }
    
    public Order createOrder(String customerId, Map<String, Integer> items) {
        if (!customers.containsKey(customerId)) {
            System.out.println("Customer not found: " + customerId);
            return null;
        }
        
        if (items == null || items.isEmpty()) {
            System.out.println("Order must contain at least one item");
            return null;
        }
        
        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0.0;
        
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String productId = entry.getKey();
            int quantity = entry.getValue();
            
            if (!products.containsKey(productId)) {
                System.out.println("Product not found: " + productId);
                return null;
            }
            
            if (quantity <= 0 || quantity > MAX_QUANTITY_PER_ORDER) {
                System.out.println("Invalid quantity for product: " + productId);
                return null;
            }
            
            Product product = products.get(productId);
            
            if (product.getStockQuantity() < quantity) {
                System.out.println("Insufficient stock for product: " + productId);
                return null;
            }
            
            double itemTotal = product.getSellingPrice() * quantity;
            totalAmount += itemTotal;
            
            OrderItem orderItem = new OrderItem(productId, product.getName(), quantity, 
                                               product.getSellingPrice(), itemTotal);
            orderItems.add(orderItem);
        }
        
        double taxAmount = totalAmount * TAX_RATE;
        double finalAmount = totalAmount + taxAmount;
        
        String orderId = generateOrderId();
        Order order = new Order(orderId, customerId, orderItems, totalAmount, taxAmount, 
                               finalAmount, LocalDate.now(), "PENDING");
        
        orders.add(order);
        totalOrders++;
        System.out.println("Order created: " + orderId + " Total: " + finalAmount);
        return order;
    }
    
    public boolean processOrder(String orderId) {
        Order order = findOrder(orderId);
        if (order == null) {
            System.out.println("Order not found: " + orderId);
            return false;
        }
        
        if (!order.getStatus().equals("PENDING")) {
            System.out.println("Order cannot be processed. Current status: " + order.getStatus());
            return false;
        }
        
        // Check stock availability again
        for (OrderItem item : order.getItems()) {
            Product product = products.get(item.getProductId());
            if (product.getStockQuantity() < item.getQuantity()) {
                System.out.println("Insufficient stock for order processing");
                return false;
            }
        }
        
        // Deduct stock
        for (OrderItem item : order.getItems()) {
            Product product = products.get(item.getProductId());
            product.removeStock(item.getQuantity());
        }
        
        order.setStatus("COMPLETED");
        totalSales += order.getFinalAmount();
        
        Transaction transaction = new Transaction(
            generateTransactionId(),
            "SALE",
            order.getFinalAmount(),
            LocalDate.now(),
            orderId
        );
        transactions.add(transaction);
        
        System.out.println("Order processed: " + orderId);
        checkLowStock();
        return true;
    }
    
    public boolean cancelOrder(String orderId) {
        Order order = findOrder(orderId);
        if (order == null) {
            System.out.println("Order not found: " + orderId);
            return false;
        }
        
        if (order.getStatus().equals("COMPLETED")) {
            // Restore stock
            for (OrderItem item : order.getItems()) {
                Product product = products.get(item.getProductId());
                product.addStock(item.getQuantity());
            }
        }
        
        order.setStatus("CANCELLED");
        System.out.println("Order cancelled: " + orderId);
        return true;
    }
    
    public PurchaseOrder createPurchaseOrder(String supplierId, Map<String, Integer> items) {
        if (!suppliers.containsKey(supplierId)) {
            System.out.println("Supplier not found: " + supplierId);
            return null;
        }
        
        if (items == null || items.isEmpty()) {
            System.out.println("Purchase order must contain at least one item");
            return null;
        }
        
        List<PurchaseOrderItem> poItems = new ArrayList<>();
        double totalAmount = 0.0;
        
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String productId = entry.getKey();
            int quantity = entry.getValue();
            
            if (!products.containsKey(productId)) {
                System.out.println("Product not found: " + productId);
                return null;
            }
            
            if (quantity <= 0 || quantity > MAX_QUANTITY_PER_ORDER) {
                System.out.println("Invalid quantity for product: " + productId);
                return null;
            }
            
            Product product = products.get(productId);
            double itemTotal = product.getCostPrice() * quantity;
            totalAmount += itemTotal;
            
            PurchaseOrderItem poItem = new PurchaseOrderItem(productId, product.getName(), 
                                                            quantity, product.getCostPrice(), itemTotal);
            poItems.add(poItem);
        }
        
        String poId = generatePurchaseOrderId();
        PurchaseOrder purchaseOrder = new PurchaseOrder(poId, supplierId, poItems, totalAmount, 
                                                       LocalDate.now(), "PENDING");
        
        purchaseOrders.add(purchaseOrder);
        System.out.println("Purchase order created: " + poId + " Total: " + totalAmount);
        return purchaseOrder;
    }
    
    public boolean receivePurchaseOrder(String poId) {
        PurchaseOrder po = findPurchaseOrder(poId);
        if (po == null) {
            System.out.println("Purchase order not found: " + poId);
            return false;
        }
        
        if (!po.getStatus().equals("PENDING")) {
            System.out.println("Purchase order already processed");
            return false;
        }
        
        // Add stock
        for (PurchaseOrderItem item : po.getItems()) {
            Product product = products.get(item.getProductId());
            product.addStock(item.getQuantity());
        }
        
        po.setStatus("RECEIVED");
        totalPurchases += po.getTotalAmount();
        
        Transaction transaction = new Transaction(
            generateTransactionId(),
            "PURCHASE",
            po.getTotalAmount(),
            LocalDate.now(),
            poId
        );
        transactions.add(transaction);
        
        System.out.println("Purchase order received: " + poId);
        return true;
    }
    
    private void checkLowStock() {
        System.out.println("\n=== Low Stock Alert ===");
        boolean hasLowStock = false;
        for (Product product : products.values()) {
            if (product.getStockQuantity() <= LOW_STOCK_THRESHOLD) {
                System.out.println("LOW STOCK: " + product.getName() + " (ID: " + product.getProductId() + 
                                 ") - Quantity: " + product.getStockQuantity());
                hasLowStock = true;
            }
        }
        if (!hasLowStock) {
            System.out.println("All products are well stocked");
        }
        System.out.println("=======================\n");
    }
    
    public void generateInventoryReport() {
        System.out.println("\n=== Inventory Report ===");
        System.out.println("Total Products: " + products.size());
        
        double totalInventoryValue = 0.0;
        int totalStockQuantity = 0;
        
        for (Product product : products.values()) {
            double productValue = product.getCostPrice() * product.getStockQuantity();
            totalInventoryValue += productValue;
            totalStockQuantity += product.getStockQuantity();
        }
        
        System.out.println("Total Stock Quantity: " + totalStockQuantity);
        System.out.println("Total Inventory Value: " + totalInventoryValue);
        System.out.println("\nProducts by Category:");
        
        Map<String, List<Product>> productsByCategory = new HashMap<>();
        for (Product product : products.values()) {
            String categoryId = product.getCategoryId();
            productsByCategory.putIfAbsent(categoryId, new ArrayList<>());
            productsByCategory.get(categoryId).add(product);
        }
        
        for (Map.Entry<String, List<Product>> entry : productsByCategory.entrySet()) {
            Category category = categories.get(entry.getKey());
            System.out.println("  " + category.getName() + ": " + entry.getValue().size() + " products");
        }
        
        System.out.println("=== End of Report ===\n");
    }
    
    public void generateSalesReport(int days) {
        System.out.println("\n=== Sales Report (Last " + days + " days) ===");
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        
        int completedOrders = 0;
        double totalSalesAmount = 0.0;
        
        for (Order order : orders) {
            if (order.getStatus().equals("COMPLETED") && 
                !order.getOrderDate().isBefore(cutoffDate)) {
                completedOrders++;
                totalSalesAmount += order.getFinalAmount();
            }
        }
        
        System.out.println("Completed Orders: " + completedOrders);
        System.out.println("Total Sales: " + totalSalesAmount);
        System.out.println("Average Order Value: " + (completedOrders > 0 ? totalSalesAmount / completedOrders : 0));
        System.out.println("=== End of Report ===\n");
    }
    
    public List<Product> getLowStockProducts() {
        List<Product> lowStockProducts = new ArrayList<>();
        for (Product product : products.values()) {
            if (product.getStockQuantity() <= LOW_STOCK_THRESHOLD) {
                lowStockProducts.add(product);
            }
        }
        return lowStockProducts;
    }
    
    public Product getProduct(String productId) {
        return products.get(productId);
    }
    
    private Order findOrder(String orderId) {
        for (Order order : orders) {
            if (order.getOrderId().equals(orderId)) {
                return order;
            }
        }
        return null;
    }
    
    private PurchaseOrder findPurchaseOrder(String poId) {
        for (PurchaseOrder po : purchaseOrders) {
            if (po.getPoId().equals(poId)) {
                return po;
            }
        }
        return null;
    }
    
    private String generateOrderId() {
        return "ORD" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generatePurchaseOrderId() {
        return "PO" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    // Nested classes
    class Product {
        private String productId;
        private String name;
        private String categoryId;
        private double sellingPrice;
        private int stockQuantity;
        private double costPrice;
        private String supplierId;
        private LocalDate dateAdded;
        
        public Product(String productId, String name, String categoryId, double sellingPrice,
                      int stockQuantity, double costPrice, String supplierId) {
            this.productId = productId;
            this.name = name;
            this.categoryId = categoryId;
            this.sellingPrice = sellingPrice;
            this.stockQuantity = stockQuantity;
            this.costPrice = costPrice;
            this.supplierId = supplierId;
            this.dateAdded = LocalDate.now();
        }
        
        public void addStock(int quantity) {
            if (quantity > 0) {
                stockQuantity += quantity;
            }
        }
        
        public boolean removeStock(int quantity) {
            if (quantity > 0 && stockQuantity >= quantity) {
                stockQuantity -= quantity;
                return true;
            }
            return false;
        }
        
        public String getProductId() { return productId; }
        public String getName() { return name; }
        public String getCategoryId() { return categoryId; }
        public double getSellingPrice() { return sellingPrice; }
        public int getStockQuantity() { return stockQuantity; }
        public double getCostPrice() { return costPrice; }
        public String getSupplierId() { return supplierId; }
        public LocalDate getDateAdded() { return dateAdded; }
        
        public void updateSellingPrice(double newPrice) {
            if (newPrice > 0) {
                this.sellingPrice = newPrice;
            }
        }
        
        public void updateCostPrice(double newPrice) {
            if (newPrice > 0) {
                this.costPrice = newPrice;
            }
        }
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
    
    class Supplier {
        private String supplierId;
        private String name;
        private String email;
        private String phone;
        private String address;
        
        public Supplier(String supplierId, String name, String email, String phone, String address) {
            this.supplierId = supplierId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.address = address;
        }
        
        public String getSupplierId() { return supplierId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getAddress() { return address; }
    }
    
    class Customer {
        private String customerId;
        private String name;
        private String email;
        private String phone;
        
        public Customer(String customerId, String name, String email, String phone) {
            this.customerId = customerId;
            this.name = name;
            this.email = email;
            this.phone = phone;
        }
        
        public String getCustomerId() { return customerId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
    }
    
    class Order {
        private String orderId;
        private String customerId;
        private List<OrderItem> items;
        private double subtotal;
        private double taxAmount;
        private double finalAmount;
        private LocalDate orderDate;
        private String status;
        
        public Order(String orderId, String customerId, List<OrderItem> items, double subtotal,
                    double taxAmount, double finalAmount, LocalDate orderDate, String status) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.items = new ArrayList<>(items);
            this.subtotal = subtotal;
            this.taxAmount = taxAmount;
            this.finalAmount = finalAmount;
            this.orderDate = orderDate;
            this.status = status;
        }
        
        public String getOrderId() { return orderId; }
        public String getCustomerId() { return customerId; }
        public List<OrderItem> getItems() { return new ArrayList<>(items); }
        public double getSubtotal() { return subtotal; }
        public double getTaxAmount() { return taxAmount; }
        public double getFinalAmount() { return finalAmount; }
        public LocalDate getOrderDate() { return orderDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    class OrderItem {
        private String productId;
        private String productName;
        private int quantity;
        private double unitPrice;
        private double totalPrice;
        
        public OrderItem(String productId, String productName, int quantity, 
                        double unitPrice, double totalPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
        }
        
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public double getTotalPrice() { return totalPrice; }
    }
    
    class PurchaseOrder {
        private String poId;
        private String supplierId;
        private List<PurchaseOrderItem> items;
        private double totalAmount;
        private LocalDate orderDate;
        private String status;
        
        public PurchaseOrder(String poId, String supplierId, List<PurchaseOrderItem> items,
                           double totalAmount, LocalDate orderDate, String status) {
            this.poId = poId;
            this.supplierId = supplierId;
            this.items = new ArrayList<>(items);
            this.totalAmount = totalAmount;
            this.orderDate = orderDate;
            this.status = status;
        }
        
        public String getPoId() { return poId; }
        public String getSupplierId() { return supplierId; }
        public List<PurchaseOrderItem> getItems() { return new ArrayList<>(items); }
        public double getTotalAmount() { return totalAmount; }
        public LocalDate getOrderDate() { return orderDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    class PurchaseOrderItem {
        private String productId;
        private String productName;
        private int quantity;
        private double unitCost;
        private double totalCost;
        
        public PurchaseOrderItem(String productId, String productName, int quantity,
                                double unitCost, double totalCost) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitCost = unitCost;
            this.totalCost = totalCost;
        }
        
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getUnitCost() { return unitCost; }
        public double getTotalCost() { return totalCost; }
    }
    
    class Transaction {
        private String transactionId;
        private String type;
        private double amount;
        private LocalDate date;
        private String referenceId;
        
        public Transaction(String transactionId, String type, double amount, 
                          LocalDate date, String referenceId) {
            this.transactionId = transactionId;
            this.type = type;
            this.amount = amount;
            this.date = date;
            this.referenceId = referenceId;
        }
        
        public String getTransactionId() { return transactionId; }
        public String getType() { return type; }
        public double getAmount() { return amount; }
        public LocalDate getDate() { return date; }
        public String getReferenceId() { return referenceId; }
    }
    
    public static void main(String[] args) {
        InventoryManagementSystem ims = new InventoryManagementSystem();
        
        // Test operations
        Map<String, Integer> orderItems = new HashMap<>();
        orderItems.put("PROD001", 2);
        orderItems.put("PROD002", 5);
        Order order = ims.createOrder("CUST001", orderItems);
        if (order != null) {
            ims.processOrder(order.getOrderId());
        }
        
        Map<String, Integer> poItems = new HashMap<>();
        poItems.put("PROD001", 20);
        PurchaseOrder po = ims.createPurchaseOrder("SUP001", poItems);
        if (po != null) {
            ims.receivePurchaseOrder(po.getPoId());
        }
        
        ims.generateInventoryReport();
        ims.generateSalesReport(30);
    }
}
