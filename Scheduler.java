// Scheduler.java
// Implements several scheduling algorithms and returns a Gantt chart and metrics.

import java.awt.Color;
import java.util.*;
import java.util.List;

public class Scheduler {

    public static class GanttEntry {
        public String pid;
        public int start;
        public int end;
        public Color color;
        public GanttEntry(String pid, int start, int end, Color color) {
            this.pid = pid; this.start = start; this.end = end; this.color = color;
        }
    }

    public static class Result {
        public List<GanttEntry> gantt = new ArrayList<>();
        public List<ProcessModel> processes = new ArrayList<>(); // final processes with metrics
    }

    // FCFS (non preemptive)
    public static Result fcfs(List<ProcessModel> input) {
        List<ProcessModel> procs = copyAndSortByArrival(input);
        Result res = new Result();
        int time = 0;
        for (ProcessModel p : procs) {
            if (time < p.arrival) time = p.arrival;
            p.startTime = time;
            p.completionTime = time + p.burst;
            p.turnaroundTime = p.completionTime - p.arrival;
            p.waitingTime = p.startTime - p.arrival;
            res.gantt.add(new GanttEntry(p.pid, p.startTime, p.completionTime, p.color));
            time = p.completionTime;
            res.processes.add(p);
        }
        return res;
    }

    // SJF Non-preemptive (Shortest Job First)
    public static Result sjfNonPreemptive(List<ProcessModel> input) {
        List<ProcessModel> procs = copyList(input);
        Result res = new Result();
        int n = procs.size();
        int completed = 0;
        int time = 0;
        boolean[] done = new boolean[n];

        while (completed < n) {
            // collect arrived and not done
            int idx = -1;
            int bestBurst = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                ProcessModel p = procs.get(i);
                if (!done[i] && p.arrival <= time) {
                    if (p.burst < bestBurst) {
                        bestBurst = p.burst;
                        idx = i;
                    } else if (p.burst == bestBurst) {
                        // tie-break: earlier arrival or PID
                        if (p.arrival < procs.get(idx).arrival) idx = i;
                    }
                }
            }
            if (idx == -1) {
                // no process arrived yet
                time++;
                continue;
            }
            ProcessModel p = procs.get(idx);
            p.startTime = time;
            p.completionTime = time + p.burst;
            p.turnaroundTime = p.completionTime - p.arrival;
            p.waitingTime = p.startTime - p.arrival;
            res.gantt.add(new GanttEntry(p.pid, p.startTime, p.completionTime, p.color));
            time = p.completionTime;
            done[idx] = true;
            completed++;
            res.processes.add(p);
        }
        return res;
    }

    // Priority Non-preemptive (lower number = higher priority)
    public static Result priorityNonPreemptive(List<ProcessModel> input) {
        List<ProcessModel> procs = copyList(input);
        Result res = new Result();
        int n = procs.size();
        int completed = 0;
        int time = 0;
        boolean[] done = new boolean[n];

        while (completed < n) {
            int idx = -1;
            int bestPriority = Integer.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                ProcessModel p = procs.get(i);
                if (!done[i] && p.arrival <= time) {
                    if (p.priority < bestPriority) {
                        bestPriority = p.priority;
                        idx = i;
                    } else if (p.priority == bestPriority) {
                        // tie-break: shorter burst
                        if (p.burst < procs.get(idx).burst) idx = i;
                    }
                }
            }
            if (idx == -1) {
                time++;
                continue;
            }
            ProcessModel p = procs.get(idx);
            p.startTime = time;
            p.completionTime = time + p.burst;
            p.turnaroundTime = p.completionTime - p.arrival;
            p.waitingTime = p.startTime - p.arrival;
            res.gantt.add(new GanttEntry(p.pid, p.startTime, p.completionTime, p.color));
            time = p.completionTime;
            done[idx] = true;
            completed++;
            res.processes.add(p);
        }
        return res;
    }

    // Round Robin
    public static Result roundRobin(List<ProcessModel> input, int quantum) {
        if (quantum <= 0) quantum = 1;
        List<ProcessModel> procs = copyList(input);
        Result res = new Result();
        int time = 0;
        // sort by arrival to seed the queue
        procs.sort(Comparator.comparingInt(p -> p.arrival));
        Queue<ProcessModel> q = new LinkedList<>();
        int index = 0;
        int n = procs.size();
        int completed = 0;

        while (completed < n) {
            // add arrived processes to queue
            while (index < n && procs.get(index).arrival <= time) {
                q.add(procs.get(index));
                index++;
            }
            if (q.isEmpty()) {
                if (index < n) {
                    // fast-forward to next arrival
                    time = procs.get(index).arrival;
                    continue;
                } else {
                    break;
                }
            }
            ProcessModel p = q.poll();
            if (p.startTime == -1) {
                p.startTime = time;
            }
            int exec = Math.min(quantum, p.remaining);
            int start = time;
            time += exec;
            p.remaining -= exec;
            int end = time;
            res.gantt.add(new GanttEntry(p.pid, start, end, p.color));

            // add newly arrived processes during execution
            while (index < n && procs.get(index).arrival <= time) {
                q.add(procs.get(index));
                index++;
            }

            if (p.remaining > 0) {
                q.add(p); // requeue
            } else {
                p.completionTime = time;
                p.turnaroundTime = p.completionTime - p.arrival;
                p.waitingTime = p.turnaroundTime - p.burst;
                completed++;
                res.processes.add(p);
            }
        }
        return res;
    }

    // Utility: deep copy list
    private static List<ProcessModel> copyList(List<ProcessModel> input) {
        List<ProcessModel> out = new ArrayList<>();
        for (ProcessModel p : input) out.add(p.copy());
        return out;
    }

    private static List<ProcessModel> copyAndSortByArrival(List<ProcessModel> input) {
        List<ProcessModel> out = copyList(input);
        out.sort(Comparator.comparingInt(p -> p.arrival));
        return out;
    }

    // convenience: compute averages for a result
    public static Map<String, Double> computeMetrics(Result res) {
        double totalWaiting = 0;
        double totalTurnaround = 0;
        int n = res.processes.size();
        for (ProcessModel p : res.processes) {
            totalWaiting += p.waitingTime;
            totalTurnaround += p.turnaroundTime;
        }
        Map<String, Double> m = new HashMap<>();
        m.put("avgWaiting", n == 0 ? 0.0 : totalWaiting / n);
        m.put("avgTurnaround", n == 0 ? 0.0 : totalTurnaround / n);
        m.put("totalTime", res.gantt.isEmpty() ? 0.0 : (double) res.gantt.get(res.gantt.size()-1).end);
        return m;
    }
}
