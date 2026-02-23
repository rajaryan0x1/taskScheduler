import java.sql.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== ProManage System ===");
            System.out.println("1. Add Project");
            System.out.println("2. View All Projects");
            System.out.println("3. Generate Weekly Schedule (Smart)");
            System.out.println("4. Start New Week");
            System.out.println("5. Exit");

            System.out.print("Choose option: ");

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
                    startNewWeek();
                    break;
                case 5:
                    return;

                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    // ================= ADD PROJECT =================
    public static void addProject(Scanner scanner) {
        try (Connection conn = DatabaseConnection.getConnection()) {

            System.out.print("Enter Title: ");
            String title = scanner.nextLine();

            System.out.print("Enter Deadline: ");
            int deadline = scanner.nextInt();

            System.out.print("Enter Revenue: ");
            int revenue = scanner.nextInt();

            String sql = "INSERT INTO projects (title, deadline, revenue, status) VALUES (?, ?, ?, 'PENDING')";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, title);
            pstmt.setInt(2, deadline);
            pstmt.setInt(3, revenue);

            pstmt.executeUpdate();

            System.out.println("Project added!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= VIEW =================
    public static void viewProjects() {
        try (Connection conn = DatabaseConnection.getConnection()) {

            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM projects");

            System.out.println("\n--- Projects ---");

            while (rs.next()) {
                System.out.println(
                        rs.getInt("id") + " | " +
                                rs.getString("title") +
                                " | Deadline: " + rs.getInt("deadline") +
                                " | Revenue: " + rs.getInt("revenue") +
                                " | Status: " + rs.getString("status")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= SMART SCHEDULER =================
    public static void generateSchedule() {
        try (Connection conn = DatabaseConnection.getConnection()) {

            ResultSet rs = conn.createStatement()
                    .executeQuery("SELECT * FROM projects WHERE status = 'PENDING'");

            List<Project> projects = new ArrayList<>();

            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                int deadline = rs.getInt("deadline");
                int revenue = rs.getInt("revenue");

                double delayRisk = calculateDelayRisk(deadline, revenue);

                projects.add(new Project(id, title, deadline, revenue, delayRisk));
            }

            // 🔥 NEW SORTING (SMART)
            projects.sort((a, b) -> Double.compare(getScore(b), getScore(a)));

            Project[] week = new Project[5];

            System.out.println("\n--- Project Scores ---");

            for (Project p : projects) {
                System.out.println(
                        p.getTitle() +
                                " | Revenue: " + p.getRevenue() +
                                " | Deadline: " + p.getDeadline() +
                                " | DelayRisk: " + p.getDelayRisk() +
                                " | Score: " + getScore(p)
                );
            }

            // scheduling (same as before)
            for (Project project : projects) {
                for (int day = Math.min(5, project.getDeadline()) - 1; day >= 0; day--) {
                    if (week[day] == null) {
                        week[day] = project;
                        break;
                    }
                }
            }

            String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri"};
            int total = 0;

            System.out.println("\n--- Weekly Schedule ---");

            for (int i = 0; i < 5; i++) {
                if (week[i] != null) {
                    System.out.println(days[i] + ": " + week[i].getTitle());
                    total += week[i].getRevenue();



                } else {
                    System.out.println(days[i] + ": ---");
                }
            }

            System.out.println("\nTotal Revenue: " + total);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= DELAY RISK (PSEUDO ML) =================
    public static double calculateDelayRisk(int deadline, int revenue) {

        double revenueFactor = revenue / 10000.0;
        double deadlineFactor = deadline / 10.0;

        double risk = 0.5 * revenueFactor + 0.5 * deadlineFactor;

        return Math.min(risk, 1.0);
    }

    // ================= SCORE FUNCTION =================
    public static double getScore(Project p) {

        double urgency = 1.0 / (1 + p.getDeadline());  // softer penalty

        return (p.getRevenue() * 0.7)
                + (p.getRevenue() * p.getDelayRisk() * 0.3)
                + (urgency * 2000);
    }

    // ================= NEW WEEK =================
    public static void startNewWeek() {
        try (Connection conn = DatabaseConnection.getConnection()) {

            Statement stmt = conn.createStatement();

            stmt.executeUpdate("UPDATE projects SET deadline = deadline - 5 WHERE status = 'PENDING'");
            stmt.executeUpdate("UPDATE projects SET status = 'EXPIRED' WHERE deadline <= 0 AND status = 'PENDING'");

            System.out.println("New week started!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
