import java.sql.*;
import java.util.*;

public class JobScheduler {


    static final String DB_URL = "jdbc:postgresql://localhost:5432/db_db";
    static final String DB_USER = "postgres"; 
    static final String DB_PASS = "password"; 
    // static final int MAX_DAYS = 100; 


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
            scanner.nextLine(); 
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

   private static void generateSchedule() {
        List<Project> projects = viewProjects();

        if (projects.isEmpty()) {
            System.out.println("No projects found!");
            return;
        }
        
        // --- NEW STEP: Find the maximum deadline dynamically ---
        int maxDays = 0;
        for (Project p : projects) {
            if (p.deadline > maxDays) {
                maxDays = p.deadline;
            }
        }
        
        // 1. Sort by Highest Revenue (Greedy)
        projects.sort((p1, p2) -> Double.compare(p2.revenue, p1.revenue));

        // Use the dynamic maxDays instead of a static constant
        Project[] schedule = new Project[maxDays];
        boolean[] slotFilled = new boolean[maxDays];
        double totalRevenue = 0;
        int scheduledCount = 0;

        // 2. Schedule Logic
        for (Project p : projects) {
            // Check backwards from deadline (capped at our dynamic maxDays)
            int limit = Math.min(p.deadline, maxDays) - 1;

            for (int j = limit; j >= 0; j--) {
                if (!slotFilled[j]) {
                    schedule[j] = p;
                    slotFilled[j] = true;
                    totalRevenue += p.revenue;
                    scheduledCount++;
                    break; 
                }
            }
        }

        // 3. Dynamic Display
        System.out.println("\n=== OPTIMAL SCHEDULE (Max Deadline: " + maxDays + " days) ===");
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri"};
        
        for (int i = 0; i < maxDays; i++) {
            // Only print weeks if we actually have data to show or empty slots up to max
            if (i % 5 == 0) {
                int weekNum = (i / 5) + 1;
                System.out.println("\n--- WEEK " + weekNum + " ---");
            }

            String dayName = dayNames[i % 5];
            System.out.printf("[%s] ", dayName);

            if (slotFilled[i]) {
                System.out.printf("%-20s (Rev: $%.2f)\n", schedule[i].title, schedule[i].revenue);
            } else {
                System.out.println("... Free Slot ...");
            }
        }
        
        System.out.println("\n--------------------------------");
        System.out.println("Total Projects: " + scheduledCount);
        System.out.println("Total Revenue:  $" + totalRevenue);
    }
