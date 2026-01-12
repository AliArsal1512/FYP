import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Comprehensive E-Commerce System
 * Manages products, customers, orders, shopping cart, payments, and shipping
 */
public class ECommerceSystem {
    
    // Constants
    private static final double SHIPPING_FEE = 50.0;
    private static final double TAX_RATE = 0.12;
    private static final double FREE_SHIPPING_THRESHOLD = 1000.0;
    private static final int MAX_CART_ITEMS = 50;
    
    // Data structures
    private Map<String, Product> products;
    private Map<String, Customer> customers;
    private Map<String, Category> categories;
    private Map<String, Cart> shoppingCarts;
    private List<Order> orders;
    private List<Payment> payments;
    private Map<String, Address> addresses;
    private List<Review> reviews;
    
    // Statistics
    private int totalProducts;
    private int totalCustomers;
    private int totalOrders;
    private double totalRevenue;
    
    public ECommerceSystem() {
        this.products = new HashMap<>();
        this.customers = new HashMap<>();
        this.categories = new HashMap<>();
        this.shoppingCarts = new HashMap<>();
        this.orders = new ArrayList<>();
        this.payments = new ArrayList<>();
        this.addresses = new HashMap<>();
        this.reviews = new ArrayList<>();
        this.totalProducts = 0;
        this.totalCustomers = 0;
        this.totalOrders = 0;
        this.totalRevenue = 0.0;
        initializeSystem();
    }
    
    private void initializeSystem() {
        // Create categories
        Category electronics = new Category("CAT001", "Electronics", "Electronic devices");
        Category clothing = new Category("CAT002", "Clothing", "Apparel and accessories");
        Category books = new Category("CAT003", "Books", "Books and publications");
        Category home = new Category("CAT004", "Home & Garden", "Home improvement items");
        
        categories.put("CAT001", electronics);
        categories.put("CAT002", clothing);
        categories.put("CAT003", books);
        categories.put("CAT004", home);
        
        // Create products
        Product product1 = new Product("PROD001", "Smartphone", "CAT001", 29999.99, 100, 
                                       "Latest model smartphone", 4.5);
        Product product2 = new Product("PROD002", "Laptop", "CAT001", 59999.99, 50,
                                       "High-performance laptop", 4.8);
        Product product3 = new Product("PROD003", "T-Shirt", "CAT002", 999.99, 200,
                                       "Cotton t-shirt", 4.2);
        Product product4 = new Product("PROD004", "Programming Book", "CAT003", 1299.99, 150,
                                       "Learn Java programming", 4.7);
        Product product5 = new Product("PROD005", "Desk Lamp", "CAT004", 1999.99, 80,
                                       "LED desk lamp", 4.3);
        
        products.put("PROD001", product1);
        products.put("PROD002", product2);
        products.put("PROD003", product3);
        products.put("PROD004", product4);
        products.put("PROD005", product5);
        totalProducts = products.size();
        
        // Create customers
        Customer customer1 = new Customer("CUST001", "John Doe", "john@email.com", "1234567890");
        Customer customer2 = new Customer("CUST002", "Jane Smith", "jane@email.com", "0987654321");
        Customer customer3 = new Customer("CUST003", "Bob Johnson", "bob@email.com", "1122334455");
        
        customers.put("CUST001", customer1);
        customers.put("CUST002", customer2);
        customers.put("CUST003", customer3);
        totalCustomers = customers.size();
        
        // Create addresses
        Address address1 = new Address("ADD001", "CUST001", "123 Main St", "City A", "State X", "12345", "Home");
        Address address2 = new Address("ADD002", "CUST001", "456 Work Ave", "City A", "State X", "12345", "Office");
        addresses.put("ADD001", address1);
        addresses.put("ADD002", address2);
    }
    
    public boolean registerCustomer(String customerId, String name, String email, String phone) {
        if (customers.containsKey(customerId)) {
            System.out.println("Customer ID already exists: " + customerId);
            return false;
        }
        
        Customer customer = new Customer(customerId, name, email, phone);
        customers.put(customerId, customer);
        
        // Create shopping cart
        Cart cart = new Cart(customerId);
        shoppingCarts.put(customerId, cart);
        
        totalCustomers++;
        System.out.println("Customer registered: " + customerId);
        return true;
    }
    
    public boolean addProductToCart(String customerId, String productId, int quantity) {
        if (!customers.containsKey(customerId)) {
            System.out.println("Customer not found: " + customerId);
            return false;
        }
        
        if (!products.containsKey(productId)) {
            System.out.println("Product not found: " + productId);
            return false;
        }
        
        Product product = products.get(productId);
        if (product.getStock() < quantity) {
            System.out.println("Insufficient stock");
            return false;
        }
        
        Cart cart = shoppingCarts.get(customerId);
        if (cart == null) {
            cart = new Cart(customerId);
            shoppingCarts.put(customerId, cart);
        }
        
        if (cart.getItemCount() >= MAX_CART_ITEMS) {
            System.out.println("Cart limit reached");
            return false;
        }
        
        cart.addItem(productId, quantity, product.getPrice());
        System.out.println("Product added to cart: " + product.getName() + " x" + quantity);
        return true;
    }
    
    public boolean removeProductFromCart(String customerId, String productId) {
        if (!shoppingCarts.containsKey(customerId)) {
            System.out.println("Cart not found");
            return false;
        }
        
        Cart cart = shoppingCarts.get(customerId);
        boolean removed = cart.removeItem(productId);
        if (removed) {
            System.out.println("Product removed from cart: " + productId);
            return true;
        } else {
            System.out.println("Product not found in cart");
            return false;
        }
    }
    
    public Order checkout(String customerId, String shippingAddressId, String paymentMethod) {
        if (!customers.containsKey(customerId)) {
            System.out.println("Customer not found: " + customerId);
            return null;
        }
        
        Cart cart = shoppingCarts.get(customerId);
        if (cart == null || cart.getItemCount() == 0) {
            System.out.println("Cart is empty");
            return null;
        }
        
        if (!addresses.containsKey(shippingAddressId)) {
            System.out.println("Address not found: " + shippingAddressId);
            return null;
        }
        
        Address shippingAddress = addresses.get(shippingAddressId);
        if (!shippingAddress.getCustomerId().equals(customerId)) {
            System.out.println("Address does not belong to customer");
            return null;
        }
        
        // Validate stock
        for (CartItem item : cart.getItems()) {
            Product product = products.get(item.getProductId());
            if (product.getStock() < item.getQuantity()) {
                System.out.println("Insufficient stock for product: " + product.getName());
                return null;
            }
        }
        
        // Calculate totals
        double subtotal = cart.getTotal();
        double shippingFee = (subtotal >= FREE_SHIPPING_THRESHOLD) ? 0.0 : SHIPPING_FEE;
        double tax = subtotal * TAX_RATE;
        double total = subtotal + shippingFee + tax;
        
        // Create order
        String orderId = generateOrderId();
        Order order = new Order(orderId, customerId, new ArrayList<>(cart.getItems()),
                               subtotal, shippingFee, tax, total, LocalDate.now(),
                               "PENDING", shippingAddressId);
        
        orders.add(order);
        totalOrders++;
        
        // Process payment
        String paymentId = generatePaymentId();
        Payment payment = new Payment(paymentId, orderId, customerId, total, paymentMethod, 
                                     LocalDate.now(), "COMPLETED");
        payments.add(payment);
        
        // Update stock
        for (CartItem item : cart.getItems()) {
            Product product = products.get(item.getProductId());
            product.reduceStock(item.getQuantity());
        }
        
        // Clear cart
        cart.clear();
        
        order.setStatus("CONFIRMED");
        totalRevenue += total;
        
        System.out.println("Order placed: " + orderId + " Total: " + total);
        return order;
    }
    
    public boolean cancelOrder(String orderId) {
        Order order = findOrder(orderId);
        if (order == null) {
            System.out.println("Order not found: " + orderId);
            return false;
        }
        
        if (!order.getStatus().equals("PENDING") && !order.getStatus().equals("CONFIRMED")) {
            System.out.println("Order cannot be cancelled. Current status: " + order.getStatus());
            return false;
        }
        
        // Restore stock
        for (OrderItem item : order.getItems()) {
            Product product = products.get(item.getProductId());
            product.addStock(item.getQuantity());
        }
        
        order.setStatus("CANCELLED");
        
        // Refund payment
        Payment payment = findPaymentByOrder(orderId);
        if (payment != null) {
            payment.setStatus("REFUNDED");
        }
        
        System.out.println("Order cancelled: " + orderId);
        return true;
    }
    
    public boolean addReview(String customerId, String productId, int rating, String comment) {
        if (!customers.containsKey(customerId)) {
            System.out.println("Customer not found: " + customerId);
            return false;
        }
        
        if (!products.containsKey(productId)) {
            System.out.println("Product not found: " + productId);
            return false;
        }
        
        if (rating < 1 || rating > 5) {
            System.out.println("Rating must be between 1 and 5");
            return false;
        }
        
        // Check if customer has purchased the product
        boolean hasPurchased = false;
        for (Order order : orders) {
            if (order.getCustomerId().equals(customerId) && order.getStatus().equals("DELIVERED")) {
                for (OrderItem item : order.getItems()) {
                    if (item.getProductId().equals(productId)) {
                        hasPurchased = true;
                        break;
                    }
                }
                if (hasPurchased) break;
            }
        }
        
        if (!hasPurchased) {
            System.out.println("Customer must purchase product before reviewing");
            return false;
        }
        
        String reviewId = generateReviewId();
        Review review = new Review(reviewId, customerId, productId, rating, comment, LocalDate.now());
        reviews.add(review);
        
        // Update product rating
        Product product = products.get(productId);
        product.addReview(rating);
        
        System.out.println("Review added for product: " + productId);
        return true;
    }
    
    public void generateOrderHistory(String customerId) {
        if (!customers.containsKey(customerId)) {
            System.out.println("Customer not found: " + customerId);
            return;
        }
        
        Customer customer = customers.get(customerId);
        System.out.println("\n=== Order History ===");
        System.out.println("Customer ID: " + customerId);
        System.out.println("Name: " + customer.getName());
        System.out.println("\nOrders:");
        System.out.println("Order ID\tDate\t\tTotal\t\tStatus");
        System.out.println("---------------------------------------------------");
        
        for (Order order : orders) {
            if (order.getCustomerId().equals(customerId)) {
                System.out.println(
                    order.getOrderId() + "\t" +
                    order.getOrderDate().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\t" +
                    order.getTotal() + "\t\t" +
                    order.getStatus()
                );
            }
        }
        
        System.out.println("=== End of History ===\n");
    }
    
    public void generateSalesReport(int days) {
        System.out.println("\n=== Sales Report (Last " + days + " days) ===");
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        
        int completedOrders = 0;
        double totalSales = 0.0;
        int totalItemsSold = 0;
        
        for (Order order : orders) {
            if ((order.getStatus().equals("CONFIRMED") || order.getStatus().equals("DELIVERED")) &&
                !order.getOrderDate().isBefore(cutoffDate)) {
                completedOrders++;
                totalSales += order.getTotal();
                for (OrderItem item : order.getItems()) {
                    totalItemsSold += item.getQuantity();
                }
            }
        }
        
        System.out.println("Completed Orders: " + completedOrders);
        System.out.println("Total Sales: " + totalSales);
        System.out.println("Total Items Sold: " + totalItemsSold);
        System.out.println("Average Order Value: " + (completedOrders > 0 ? totalSales / completedOrders : 0));
        System.out.println("=== End of Report ===\n");
    }
    
    public List<Product> searchProducts(String keyword) {
        List<Product> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        
        for (Product product : products.values()) {
            if (product.getName().toLowerCase().contains(lowerKeyword) ||
                product.getDescription().toLowerCase().contains(lowerKeyword)) {
                results.add(product);
            }
        }
        
        return results;
    }
    
    public List<Product> getProductsByCategory(String categoryId) {
        List<Product> categoryProducts = new ArrayList<>();
        for (Product product : products.values()) {
            if (product.getCategoryId().equals(categoryId)) {
                categoryProducts.add(product);
            }
        }
        return categoryProducts;
    }
    
    private Order findOrder(String orderId) {
        for (Order order : orders) {
            if (order.getOrderId().equals(orderId)) {
                return order;
            }
        }
        return null;
    }
    
    private Payment findPaymentByOrder(String orderId) {
        for (Payment payment : payments) {
            if (payment.getOrderId().equals(orderId)) {
                return payment;
            }
        }
        return null;
    }
    
    private String generateOrderId() {
        return "ORD" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generatePaymentId() {
        return "PAY" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    private String generateReviewId() {
        return "REV" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
    
    // Nested classes
    class Product {
        private String productId;
        private String name;
        private String categoryId;
        private double price;
        private int stock;
        private String description;
        private double rating;
        private int reviewCount;
        
        public Product(String productId, String name, String categoryId, double price,
                      int stock, String description, double initialRating) {
            this.productId = productId;
            this.name = name;
            this.categoryId = categoryId;
            this.price = price;
            this.stock = stock;
            this.description = description;
            this.rating = initialRating;
            this.reviewCount = 0;
        }
        
        public void reduceStock(int quantity) {
            if (quantity > 0 && stock >= quantity) {
                stock -= quantity;
            }
        }
        
        public void addStock(int quantity) {
            if (quantity > 0) {
                stock += quantity;
            }
        }
        
        public void addReview(int rating) {
            reviewCount++;
            this.rating = ((this.rating * (reviewCount - 1)) + rating) / reviewCount;
        }
        
        public String getProductId() { return productId; }
        public String getName() { return name; }
        public String getCategoryId() { return categoryId; }
        public double getPrice() { return price; }
        public int getStock() { return stock; }
        public String getDescription() { return description; }
        public double getRating() { return rating; }
        public int getReviewCount() { return reviewCount; }
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
    
    class Customer {
        private String customerId;
        private String name;
        private String email;
        private String phone;
        private LocalDate registrationDate;
        
        public Customer(String customerId, String name, String email, String phone) {
            this.customerId = customerId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.registrationDate = LocalDate.now();
        }
        
        public String getCustomerId() { return customerId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public LocalDate getRegistrationDate() { return registrationDate; }
    }
    
    class Address {
        private String addressId;
        private String customerId;
        private String street;
        private String city;
        private String state;
        private String zipCode;
        private String addressType;
        
        public Address(String addressId, String customerId, String street, String city,
                      String state, String zipCode, String addressType) {
            this.addressId = addressId;
            this.customerId = customerId;
            this.street = street;
            this.city = city;
            this.state = state;
            this.zipCode = zipCode;
            this.addressType = addressType;
        }
        
        public String getAddressId() { return addressId; }
        public String getCustomerId() { return customerId; }
        public String getStreet() { return street; }
        public String getCity() { return city; }
        public String getState() { return state; }
        public String getZipCode() { return zipCode; }
        public String getAddressType() { return addressType; }
    }
    
    class Cart {
        private String customerId;
        private Map<String, CartItem> items;
        
        public Cart(String customerId) {
            this.customerId = customerId;
            this.items = new HashMap<>();
        }
        
        public void addItem(String productId, int quantity, double price) {
            if (items.containsKey(productId)) {
                CartItem item = items.get(productId);
                item.setQuantity(item.getQuantity() + quantity);
            } else {
                items.put(productId, new CartItem(productId, quantity, price));
            }
        }
        
        public boolean removeItem(String productId) {
            return items.remove(productId) != null;
        }
        
        public void clear() {
            items.clear();
        }
        
        public double getTotal() {
            double total = 0.0;
            for (CartItem item : items.values()) {
                total += item.getTotal();
            }
            return total;
        }
        
        public int getItemCount() {
            return items.size();
        }
        
        public List<CartItem> getItems() {
            return new ArrayList<>(items.values());
        }
    }
    
    class CartItem {
        private String productId;
        private int quantity;
        private double unitPrice;
        
        public CartItem(String productId, int quantity, double unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
        
        public String getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public double getUnitPrice() { return unitPrice; }
        public double getTotal() { return quantity * unitPrice; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
    
    class Order {
        private String orderId;
        private String customerId;
        private List<OrderItem> items;
        private double subtotal;
        private double shippingFee;
        private double tax;
        private double total;
        private LocalDate orderDate;
        private String status;
        private String shippingAddressId;
        
        public Order(String orderId, String customerId, List<OrderItem> items, double subtotal,
                    double shippingFee, double tax, double total, LocalDate orderDate,
                    String status, String shippingAddressId) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.items = new ArrayList<>(items);
            this.subtotal = subtotal;
            this.shippingFee = shippingFee;
            this.tax = tax;
            this.total = total;
            this.orderDate = orderDate;
            this.status = status;
            this.shippingAddressId = shippingAddressId;
        }
        
        public String getOrderId() { return orderId; }
        public String getCustomerId() { return customerId; }
        public List<OrderItem> getItems() { return new ArrayList<>(items); }
        public double getSubtotal() { return subtotal; }
        public double getShippingFee() { return shippingFee; }
        public double getTax() { return tax; }
        public double getTotal() { return total; }
        public LocalDate getOrderDate() { return orderDate; }
        public String getStatus() { return status; }
        public String getShippingAddressId() { return shippingAddressId; }
        public void setStatus(String status) { this.status = status; }
    }
    
    class OrderItem {
        private String productId;
        private String productName;
        private int quantity;
        private double unitPrice;
        private double totalPrice;
        
        public OrderItem(String productId, String productName, int quantity, double unitPrice, double totalPrice) {
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
    
    class Payment {
        private String paymentId;
        private String orderId;
        private String customerId;
        private double amount;
        private String paymentMethod;
        private LocalDate paymentDate;
        private String status;
        
        public Payment(String paymentId, String orderId, String customerId, double amount,
                      String paymentMethod, LocalDate paymentDate, String status) {
            this.paymentId = paymentId;
            this.orderId = orderId;
            this.customerId = customerId;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.paymentDate = paymentDate;
            this.status = status;
        }
        
        public String getPaymentId() { return paymentId; }
        public String getOrderId() { return orderId; }
        public String getCustomerId() { return customerId; }
        public double getAmount() { return amount; }
        public String getPaymentMethod() { return paymentMethod; }
        public LocalDate getPaymentDate() { return paymentDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    class Review {
        private String reviewId;
        private String customerId;
        private String productId;
        private int rating;
        private String comment;
        private LocalDate reviewDate;
        
        public Review(String reviewId, String customerId, String productId, int rating,
                     String comment, LocalDate reviewDate) {
            this.reviewId = reviewId;
            this.customerId = customerId;
            this.productId = productId;
            this.rating = rating;
            this.comment = comment;
            this.reviewDate = reviewDate;
        }
        
        public String getReviewId() { return reviewId; }
        public String getCustomerId() { return customerId; }
        public String getProductId() { return productId; }
        public int getRating() { return rating; }
        public String getComment() { return comment; }
        public LocalDate getReviewDate() { return reviewDate; }
    }
    
    public static void main(String[] args) {
        ECommerceSystem ecommerce = new ECommerceSystem();
        
        // Test operations
        ecommerce.addProductToCart("CUST001", "PROD001", 1);
        ecommerce.addProductToCart("CUST001", "PROD002", 1);
        Order order = ecommerce.checkout("CUST001", "ADD001", "Credit Card");
        if (order != null) {
            ecommerce.generateOrderHistory("CUST001");
        }
        ecommerce.generateSalesReport(30);
    }
}
