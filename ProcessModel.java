// ProcessModel.java
// Simple model representing a process for scheduling.

import java.awt.Color;

public class ProcessModel {
    public String pid;
    public int arrival;
    public int burst;
    public int remaining; // used for preemptive algorithms
    public int priority;
    public int startTime = -1;
    public int completionTime = -1;
    public int waitingTime = 0;
    public int turnaroundTime = 0;
    public Color color;

    public ProcessModel(String pid, int arrival, int burst, int priority, Color color) {
        this.pid = pid;
        this.arrival = arrival;
        this.burst = burst;
        this.remaining = burst;
        this.priority = priority;
        this.color = color;
    }

    public ProcessModel copy() {
        ProcessModel p = new ProcessModel(pid, arrival, burst, priority, color);
        p.remaining = remaining;
        p.startTime = startTime;
        p.completionTime = completionTime;
        p.waitingTime = waitingTime;
        p.turnaroundTime = turnaroundTime;
        return p;
    }

    @Override
    public String toString() {
        return String.format("%s (A=%d, B=%d, P=%d)", pid, arrival, burst, priority);
    }
}
