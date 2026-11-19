import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class OrderManagementSystem {

    // Lab system ke MySQL credentials ke hisab se USER aur PASS check kar lena
    private static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String USER = "root";
    private static final String PASS = "root123";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
            System.out.println("Connected to ecommerce_db successfully!");

            while (true) {
                System.out.println("\n========= E-COMMERCE ORDER MANAGEMENT SYSTEM =========");
                System.out.println("1. Insert Order Records (Part a)");
                System.out.println("2. Search Orders by Status (Qty > 2, Sorted by Unit Price DESC) (Part b)");
                System.out.println("3. Increase Order Quantity (Part c)");
                System.out.println("4. Search & Update Order Status (Part d)");
                System.out.println("5. Total Order Value & Delete Cancelled Order (Part e)");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");

                int choice = sc.nextInt();
                sc.nextLine(); // Clear newline buffer

                switch (choice) {
                    case 1:
                        insertOrders(con, sc);
                        break;
                    case 2:
                        retrieveAndSortOrders(con, sc);
                        break;
                    case 3:
                        increaseOrderQuantity(con, sc);
                        break;
                    case 4:
                        searchAndUpdateStatus(con, sc);
                        break;
                    case 5:
                        aggregateAndDeleteOrder(con, sc);
                        break;
                    case 6:
                        System.out.println("Exiting Order Management System...");
                        sc.close();
                        return;
                    default:
                        System.out.println("Invalid choice! Please try again.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database Connection Error: " + e.getMessage());
        }
    }

    // (a) Insert at least 3 order records using PreparedStatement & handle duplicate IDs
    private static void insertOrders(Connection con, Scanner sc) {
        System.out.print("How many orders do you want to insert? (Min 3 recommended): ");
        int count = sc.nextInt();
        sc.nextLine();

        String sql = "INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                System.out.println("\n--- Enter details for Order " + i + " ---");
                System.out.print("Order ID: ");
                int id = sc.nextInt();
                sc.nextLine();

                System.out.print("Customer Name: ");
                String customer = sc.nextLine();

                System.out.print("Product Name: ");
                String product = sc.nextLine();

                System.out.print("Quantity: ");
                int qty = sc.nextInt();

                System.out.print("Unit Price: ");
                double price = sc.nextDouble();
                sc.nextLine();

                System.out.print("Status (Pending / Shipped / Delivered / Cancelled): ");
                String status = sc.nextLine().trim();

                ps.setInt(1, id);
                ps.setString(2, customer);
                ps.setString(3, product);
                ps.setInt(4, qty);
                ps.setDouble(5, price);
                ps.setString(6, status);

                try {
                    int rows = ps.executeUpdate();
                    if (rows > 0) {
                        System.out.println("Order ID " + id + " inserted successfully!");
                    }
                } catch (SQLIntegrityConstraintViolationException dupEx) {
                    System.out.println("Error: Duplicate Order ID " + id + "! Record skipped.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Insert Error: " + e.getMessage());
        }
    }

    // (b) Accept status, display orders with status & quantity > 2, sorted by unit_price DESC
    private static void retrieveAndSortOrders(Connection con, Scanner sc) {
        System.out.print("Enter Order Status to filter (e.g., Pending, Shipped, Delivered): ");
        String status = sc.nextLine().trim();

        String sql = "SELECT * FROM Orders WHERE LOWER(status) = LOWER(?) AND quantity > 2 ORDER BY unit_price DESC";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();

            boolean found = false;
            System.out.println("\n--- Orders with Status '" + status + "' and Quantity > 2 (Sorted by Unit Price DESC) ---");
            while (rs.next()) {
                found = true;
                System.out.printf("Order ID: %d | Customer: %s | Product: %s | Qty: %d | Unit Price: %.2f | Status: %s%n",
                        rs.getInt("order_id"),
                        rs.getString("customer_name"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("unit_price"),
                        rs.getString("status"));
            }

            if (!found) {
                System.out.println("No orders found matching the given status with quantity > 2.");
            }
        } catch (SQLException e) {
            System.out.println("Retrieval Error: " + e.getMessage());
        }
    }

    // (c) Accept order ID & additional quantity, increase existing quantity
    private static void increaseOrderQuantity(Connection con, Scanner sc) {
        System.out.print("Enter Order ID: ");
        int id = sc.nextInt();

        System.out.print("Enter Additional Quantity to add: ");
        int addQty = sc.nextInt();
        sc.nextLine();

        String sql = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, addQty);
            ps.setInt(2, id);

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Order quantity updated successfully for Order ID " + id + "!");
            } else {
                System.out.println("Error: Order ID " + id + " does not exist!");
            }
        } catch (SQLException e) {
            System.out.println("Update Quantity Error: " + e.getMessage());
        }
    }

    // (d) Accept order ID & new status, validate status (Pending, Shipped, Delivered) & display updated details
    private static void searchAndUpdateStatus(Connection con, Scanner sc) {
        System.out.print("Enter Order ID: ");
        int id = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter New Status (Pending / Shipped / Delivered): ");
        String newStatus = sc.nextLine().trim();

        List<String> validStatuses = Arrays.asList("Pending", "Shipped", "Delivered");
        String normalizedStatus = null;
        for (String valid : validStatuses) {
            if (valid.equalsIgnoreCase(newStatus)) {
                normalizedStatus = valid;
                break;
            }
        }

        if (normalizedStatus == null) {
            System.out.println("Error: Invalid status! Allowed values are: Pending, Shipped, or Delivered.");
            return;
        }

        String checkSql = "SELECT * FROM Orders WHERE order_id = ?";
        String updateSql = "UPDATE Orders SET status = ? WHERE order_id = ?";

        try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
            checkPs.setInt(1, id);
            ResultSet rs = checkPs.executeQuery();

            if (!rs.next()) {
                System.out.println("Error: Order ID " + id + " does not exist!");
                return;
            }

            // Update status
            try (PreparedStatement updatePs = con.prepareStatement(updateSql)) {
                updatePs.setString(1, normalizedStatus);
                updatePs.setInt(2, id);
                updatePs.executeUpdate();
            }

            // Retrieve and display updated order details
            ResultSet updatedRs = checkPs.executeQuery();
            if (updatedRs.next()) {
                System.out.println("\n--- Updated Order Details ---");
                System.out.printf("Order ID: %d | Customer: %s | Product: %s | Qty: %d | Unit Price: %.2f | Updated Status: %s%n",
                        updatedRs.getInt("order_id"),
                        updatedRs.getString("customer_name"),
                        updatedRs.getString("product_name"),
                        updatedRs.getInt("quantity"),
                        updatedRs.getDouble("unit_price"),
                        updatedRs.getString("status"));
            }
        } catch (SQLException e) {
            System.out.println("Update Status Error: " + e.getMessage());
        }
    }

    // (e) Display total order value using SUM(quantity * unit_price) & delete only if status is Cancelled
    private static void aggregateAndDeleteOrder(Connection con, Scanner sc) {
        String sumSql = "SELECT SUM(quantity * unit_price) AS total_order_value FROM Orders";

        try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(sumSql)) {
            if (rs.next()) {
                double totalValue = rs.getDouble("total_order_value");
                System.out.printf("\nTotal Order Value: %.2f%n", totalValue);
            }
        } catch (SQLException e) {
            System.out.println("Aggregate Query Error: " + e.getMessage());
        }

        System.out.print("Enter Order ID to delete (only deletes if status is 'Cancelled'): ");
        int id = sc.nextInt();
        sc.nextLine();

        String checkSql = "SELECT status FROM Orders WHERE order_id = ?";
        String deleteSql = "DELETE FROM Orders WHERE order_id = ?";

        try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
            checkPs.setInt(1, id);
            ResultSet rs = checkPs.executeQuery();

            if (!rs.next()) {
                System.out.println("Error: Order ID " + id + " does not exist!");
                return;
            }

            String currentStatus = rs.getString("status");
            if (!"Cancelled".equalsIgnoreCase(currentStatus)) {
                System.out.println("Cannot delete: Order status is '" + currentStatus + "' (must be 'Cancelled' to delete).");
                return;
            }

            try (PreparedStatement deletePs = con.prepareStatement(deleteSql)) {
                deletePs.setInt(1, id);
                deletePs.executeUpdate();
                System.out.println("Cancelled Order ID " + id + " deleted successfully!");
            }
        } catch (SQLException e) {
            System.out.println("Delete Error: " + e.getMessage());
        }
    }
}