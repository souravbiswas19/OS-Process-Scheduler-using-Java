// SchedulerGUIFancy.java
// A visually upgraded Swing GUI for the Process Scheduler System using FlatLaf.
//
// Drop this file in the same folder as ProcessModel.java, Scheduler.java, GanttPanel.java
// Compile with FlatLaf on the classpath:
//    javac -cp flatlaf-3.0.6.jar *.java
// Run with:
//    java -cp .;flatlaf-3.0.6.jar SchedulerGUIFancy   (Windows)
//    java -cp .:flatlaf-3.0.6.jar SchedulerGUIFancy   (macOS/Linux)

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Modernized scheduler GUI using FlatLaf for an attractive UI.
 * Uses rounded "card" panels, a toolbar, and improved spacing.
 */
public class SchedulerGUIFancy extends JFrame {
    private DefaultTableModel tableModel;
    private JTable processTable;
    private JTextField pidField, arrivalField, burstField, priorityField, quantumField;
    private JComboBox<String> algoCombo;
    private GanttPanel ganttPanel;
    private JTextArea metricsArea;
    private int autoPidCounter = 1;

    // soft palette
    private static final Color ACCENT = new Color(56, 142, 255);
    private static final Color CARD_BG = new Color(250, 250, 250);
    private static final Color BORDER = new Color(220, 225, 230);

    public SchedulerGUIFancy() {
        super("Process Scheduler — Modern UI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 760);
        initLookAndFeel();
        initUI();
        setLocationRelativeTo(null);
    }

    private void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            // tweak defaults (optional)
            UIManager.put("Component.focusWidth", 2);
            UIManager.put("Button.arc", 10);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.showHorizontalLines", false);
        } catch (Exception e) {
            System.err.println("Failed to apply FlatLaf: " + e.getMessage());
        }
    }

    private void initUI() {
        // Top toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(new EmptyBorder(6, 10, 6, 10));
        toolbar.setBackground(Color.WHITE);

        JLabel title = new JLabel("⧉ Process Scheduler");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(ACCENT);
        toolbar.add(title);
        toolbar.addSeparator(new Dimension(16, 0));

        JButton sampleBtn = createToolbarButton("⤴ Load Sample", "Load sample processes");
        JButton runBtn = createToolbarButton("▶ Run", "Run scheduling simulation");
        JButton clearBtn = createToolbarButton("✖ Clear", "Clear all processes");
        toolbar.add(sampleBtn);
        toolbar.add(runBtn);
        toolbar.add(clearBtn);

        // Main container (vertical)
        JPanel main = new JPanel();
        main.setLayout(new BorderLayout(12, 12));
        main.setBorder(new EmptyBorder(12, 12, 12, 12));
        main.setBackground(new Color(245, 247, 250));

        // Top area: controls card
        JPanel topCards = new JPanel(new GridBagLayout());
        topCards.setOpaque(false);
        GridBagConstraints tc = new GridBagConstraints();
        tc.insets = new Insets(8, 8, 8, 8);
        tc.fill = GridBagConstraints.BOTH;

        // Input Card
        RoundedPanel inputCard = new RoundedPanel(14, CARD_BG);
        inputCard.setLayout(new GridBagLayout());
        inputCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(12,12,12,12)));
        GridBagConstraints ic = new GridBagConstraints();
        ic.insets = new Insets(6,6,6,6);
        ic.anchor = GridBagConstraints.WEST;

        pidField = new JTextField(8);
        arrivalField = new JTextField(6);
        burstField = new JTextField(6);
        priorityField = new JTextField(6);
        quantumField = new JTextField(4);
        quantumField.setText("2");

        algoCombo = new JComboBox<>(new String[] {"FCFS", "SJF (Non-preemptive)", "Priority (Non-preemptive)", "Round Robin"});
        algoCombo.addActionListener(e -> quantumField.setEnabled("Round Robin".equals(algoCombo.getSelectedItem().toString())));

        ic.gridx = 0; ic.gridy = 0; inputCard.add(label("PID (opt)"), ic);
        ic.gridx = 1; inputCard.add(pidField, ic);
        ic.gridx = 2; inputCard.add(label("Arrival"), ic);
        ic.gridx = 3; inputCard.add(arrivalField, ic);
        ic.gridx = 4; inputCard.add(label("Burst"), ic);
        ic.gridx = 5; inputCard.add(burstField, ic);

        ic.gridy = 1; ic.gridx = 0; inputCard.add(label("Priority"), ic);
        ic.gridx = 1; inputCard.add(priorityField, ic);
        ic.gridx = 2; inputCard.add(label("Algorithm"), ic);
        ic.gridx = 3; inputCard.add(algoCombo, ic);
        ic.gridx = 4; inputCard.add(label("Quantum (RR)"), ic);
        ic.gridx = 5; inputCard.add(quantumField, ic);

        // actions
        JButton addBtn = new JButton("＋ Add");
        addBtn.putClientProperty("JButton.buttonType", "roundRect");
        addBtn.setBackground(ACCENT);
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        addBtn.setPreferredSize(new Dimension(110, 36));
        addBtn.addActionListener(e -> onAdd());

        JPanel addWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addWrap.setOpaque(false);
        addWrap.add(addBtn);

        ic.gridy = 2; ic.gridx = 0; ic.gridwidth = 6; inputCard.add(addWrap, ic);

        // Table Card
        RoundedPanel tableCard = new RoundedPanel(14, CARD_BG);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(10,10,10,10)));

        String[] cols = {"PID", "Arrival", "Burst", "Priority"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        processTable = new JTable(tableModel);
        processTable.setRowHeight(28);
        JScrollPane tableScroll = new JScrollPane(processTable);
        tableScroll.setBorder(null);
        tableCard.add(new JLabel("Processes"), BorderLayout.NORTH);
        tableCard.add(tableScroll, BorderLayout.CENTER);

        // Gantt + Metrics Card (center)
        RoundedPanel centerCard = new RoundedPanel(14, CARD_BG);
        centerCard.setLayout(new BorderLayout());
        centerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(10,10,10,10)));

        ganttPanel = new GanttPanel();
        JScrollPane ganttScroll = new JScrollPane(ganttPanel);
        ganttScroll.setBorder(null);
        ganttScroll.setPreferredSize(new Dimension(900, 260));

        metricsArea = new JTextArea(6, 60);
        metricsArea.setEditable(false);
        metricsArea.setFont(metricsArea.getFont().deriveFont(13f));
        JScrollPane metricsScroll = new JScrollPane(metricsArea);
        metricsScroll.setBorder(null);

        centerCard.add(ganttScroll, BorderLayout.CENTER);
        centerCard.add(metricsScroll, BorderLayout.SOUTH);

        // assemble top cards layout
        tc.gridx = 0; tc.gridy = 0; tc.weightx = 1; tc.weighty = 0; tc.gridwidth = 2;
        topCards.add(inputCard, tc);
        tc.gridy = 1; tc.gridwidth = 2; topCards.add(tableCard, tc);

        // put center card on main
        main.add(toolbar, BorderLayout.NORTH);
        main.add(topCards, BorderLayout.WEST);
        main.add(centerCard, BorderLayout.CENTER);

        add(main);

        // connect toolbar actions
        sampleBtn.addActionListener(e -> addSampleData());
        runBtn.addActionListener(e -> onRun());
        clearBtn.addActionListener(e -> onClear());
        // connect add button
        addBtn.addActionListener(e -> onAdd());

        // small polish
        addSampleData();
    }

    // small helpers
    private JButton createToolbarButton(String text, String hint) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setBackground(new Color(0,0,0,0));
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText(hint);
        b.setFont(b.getFont().deriveFont(13f));
        return b;
    }
    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(13f));
        return l;
    }

    private void addSampleData() {
        safeAddProcess("P1", 0, 5, 2);
        safeAddProcess("P2", 2, 3, 1);
        safeAddProcess("P3", 4, 1, 3);
    }

    // actions
    private void onAdd() {
        String pid = pidField.getText().trim();
        String sArr = arrivalField.getText().trim();
        String sBurst = burstField.getText().trim();
        String sPri = priorityField.getText().trim();

        if (sArr.isEmpty() || sBurst.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Arrival and Burst are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            int arrival = Integer.parseInt(sArr);
            int burst = Integer.parseInt(sBurst);
            int priority = sPri.isEmpty() ? 0 : Integer.parseInt(sPri);
            if (arrival < 0 || burst <= 0) throw new NumberFormatException();
            if (pid.isEmpty()) pid = "P" + (autoPidCounter++);
            safeAddProcess(pid, arrival, burst, priority);
            pidField.setText(""); arrivalField.setText(""); burstField.setText(""); priorityField.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid integer values (arrival >=0, burst >0).", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void safeAddProcess(String pid, int arrival, int burst, int priority) {
        tableModel.addRow(new Object[] {pid, arrival, burst, priority});
    }

    private void onClear() {
        tableModel.setRowCount(0);
        ganttPanel.setGantt(null);
        metricsArea.setText("");
        autoPidCounter = 1;
    }

    private void onRun() {
        List<ProcessModel> procs = collectProcesses();
        if (procs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No processes to schedule.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String algo = (String) algoCombo.getSelectedItem();
        Scheduler.Result res;
        try {
            if ("FCFS".equals(algo)) {
                res = Scheduler.fcfs(procs);
            } else if ("SJF (Non-preemptive)".equals(algo)) {
                res = Scheduler.sjfNonPreemptive(procs);
            } else if ("Priority (Non-preemptive)".equals(algo)) {
                res = Scheduler.priorityNonPreemptive(procs);
            } else {
                int q = Integer.parseInt(quantumField.getText().trim());
                res = Scheduler.roundRobin(procs, q);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error running scheduler: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            return;
        }
        // apply colors to gantt entries (nice accent hues)
        Color[] palette = new Color[] {
                new Color(135,206,250), new Color(144,238,144), new Color(255,182,193),
                new Color(255,228,181), new Color(221,160,221), new Color(240,230,140),
                new Color(176,196,222)
        };
        // assign color by pid mapping
        Map<String, Color> colorMap = new HashMap<>();
        int idx = 0;
        for (Scheduler.GanttEntry e : res.gantt) {
            colorMap.putIfAbsent(e.pid, palette[(idx++) % palette.length]);
        }
        // update gantt colors
        for (Scheduler.GanttEntry e : res.gantt) {
            e.color = colorMap.getOrDefault(e.pid, Color.LIGHT_GRAY);
        }

        ganttPanel.setGantt(res.gantt);
        displayMetrics(res);
    }

    private List<ProcessModel> collectProcesses() {
        List<ProcessModel> procs = new ArrayList<>();
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            String pid = (String) tableModel.getValueAt(r, 0);
            int arr = Integer.parseInt(tableModel.getValueAt(r, 1).toString());
            int burst = Integer.parseInt(tableModel.getValueAt(r, 2).toString());
            int pri = Integer.parseInt(tableModel.getValueAt(r, 3).toString());
            // color set later
            procs.add(new ProcessModel(pid, arr, burst, pri, null));
        }
        return procs;
    }

    private void displayMetrics(Scheduler.Result res) {
        StringBuilder sb = new StringBuilder();
        sb.append("Process Metrics:\n");
        sb.append(String.format("%-8s %-8s %-10s %-12s %-12s\n", "PID","Arrival","Burst","Waiting","Turnaround"));
        res.processes.sort(Comparator.comparing(p -> p.pid));
        for (ProcessModel p : res.processes) {
            sb.append(String.format("%-8s %-8d %-10d %-12d %-12d\n", p.pid, p.arrival, p.burst, p.waitingTime, p.turnaroundTime));
        }
        Map<String, Double> m = Scheduler.computeMetrics(res);
        sb.append("\nAverages:\n");
        sb.append(String.format("Average Waiting Time: %.2f\n", m.get("avgWaiting")));
        sb.append(String.format("Average Turnaround Time: %.2f\n", m.get("avgTurnaround")));
        sb.append(String.format("Total CPU Time (timeline end): %.0f\n", m.get("totalTime")));
        metricsArea.setText(sb.toString());
    }

    // small rounded panel helper
    static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color backgroundColor;
        public RoundedPanel(int arc, Color bg) {
            this.arc = arc;
            this.backgroundColor = bg;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(backgroundColor);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            super.paintComponent(g);
            g.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SchedulerGUIFancy gui = new SchedulerGUIFancy();
            gui.setVisible(true);
        });
    }
}
