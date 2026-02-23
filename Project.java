public class Project {

    private int id;
    private String title;
    private int deadline;
    private int revenue;
    private double delayRisk;

    public Project(int id, String title, int deadline, int revenue, double delayRisk) {
        this.id = id;
        this.title = title;
        this.deadline = deadline;
        this.revenue = revenue;
        this.delayRisk = delayRisk;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getDeadline() {
        return deadline;
    }

    public int getRevenue() {
        return revenue;
    }

    public double getDelayRisk() {
        return delayRisk;
    }
}

