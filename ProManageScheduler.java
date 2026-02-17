import java.sql.*;
import java.util.*;

public class ProManageScheduler {

    // --- CONFIGURATION ---
    static final String DB_URL = "jdbc:postgresql://localhost:5432/promanage_db";
    static final String DB_USER = "postgres"; // Change to your username
    static final String DB_PASS = "password"; // Change to your password
    static final int MAX_DAYS = 5; // Monday to Friday

    // --- MODEL CLASS ---
    static class Project {
        int id;
        String title;
        int deadline;
        double revenue;

        public Project(int id, String title, int deadline, double revenue) {
            this.id = id;
            this.title = title;
            this.deadline = deadline;
            this.revenue = revenue;
        }

        @Override
        public String toString() {
            return String.format("ID: %d | %-15s | Deadline: Day %d | Revenue: $%.2f", 
                id, title, deadline, revenue);
        }
    }

    // --- MAIN EXECUTION ---
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- ProManage Solutions Scheduler ---");
            System.out.println("1. Add New Project");
            System.out.println("2. View All Projects");
            System.out.println("3. Generate Optimal Weekly Schedule");
            System.out.println("4. Exit");
            System.out.print("Enter choice: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    addProject(scanner);
                    break;
                case 2:
                    viewProjects();
                    break;
                case 3:
                    generateSchedule();
                    break;
                case 4:
                    System.out.println("Exiting system.");
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // --- DATABASE OPERATIONS ---
    
    // 1. Add Project
    private static void addProject(Scanner scanner) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            System.out.print("Enter Project Title: ");
            String title = scanner.nextLine();
            
            System.out.print("Enter Deadline (Days from Mon): ");
            int deadline = scanner.nextInt();
            
            System.out.print("Enter Expected Revenue: ");
            double revenue = scanner.nextDouble();

            String sql = "INSERT INTO projects (title, deadline, revenue) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, title);
            pstmt.setInt(2, deadline);
            pstmt.setDouble(3, revenue);
            
            int rows = pstmt.executeUpdate();
            if (rows > 0) System.out.println("Project added successfully!");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 2. View Projects
    private static List<Project> viewProjects() {
        List<Project> projects = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT * FROM projects";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- Project List ---");
            while (rs.next()) {
                Project p = new Project(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getInt("deadline"),
                    rs.getDouble("revenue")
                );
                projects.add(p);
                System.out.println(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return projects;
    }

    // --- THE ALGORITHM (Job Sequencing with Deadlines) ---
    private static void generateSchedule() {
        List<Project> projects = viewProjects();
        
        // Step A: Sort projects by Revenue in Descending Order (Greedy Approach)
        projects.sort((p1, p2) -> Double.compare(p2.revenue, p1.revenue));

        // Step B: Initialize Schedule (Array of 5 days, Index 0 = Mon, 4 = Fri)
        Project[] weekSchedule = new Project[MAX_DAYS];
        boolean[] slotFilled = new boolean[MAX_DAYS];
        double totalRevenue = 0;
        int projectsScheduled = 0;

        // Step C: Allocate Slots
        for (Project p : projects) {
            // Find the latest possible free slot before the deadline
            // (e.g., if deadline is 3, check indices 2, then 1, then 0)
            
            // We take Math.min(p.deadline, MAX_DAYS) because we can't schedule beyond Friday
            for (int j = Math.min(p.deadline, MAX_DAYS) - 1; j >= 0; j--) {
                if (!slotFilled[j]) {
                    weekSchedule[j] = p;
                    slotFilled[j] = true;
                    totalRevenue += p.revenue;
                    projectsScheduled++;
                    break; // Move to next project once scheduled
                }
            }
        }

        // --- OUTPUT RESULTS ---
        System.out.println("\n=== OPTIMAL WEEKLY SCHEDULE ===");
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        
        for (int i = 0; i < MAX_DAYS; i++) {
            System.out.print(days[i] + ": ");
            if (slotFilled[i]) {
                System.out.println(weekSchedule[i].title + " (Rev: $" + weekSchedule[i].revenue + ")");
            } else {
                System.out.println("[Free Slot]");
            }
        }
        
        System.out.println("--------------------------------");
        System.out.println("Total Projects Scheduled: " + projectsScheduled);
        System.out.println("Total Expected Revenue: $" + totalRevenue);
    }
}