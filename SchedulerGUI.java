// SchedulerGUI.java
// Improved Swing GUI for the Process Scheduler System with toolbar, menu, editable table,
// color chooser, zoom slider, export CSV and save PNG features.

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

public class SchedulerGUI extends JFrame {
    private DefaultTableModel tableModel;
    private JTable processTable;
    private JTextField pidField, arrivalField, burstField, priorityField, quantumField;
    private JComboBox<String> algoCombo;
    private GanttPanel ganttPanel;
    private JTextArea metricsArea;
    private JSlider zoomSlider;
    private JLabel statusLabel;
    private int autoPidCounter = 1;
    private final Color[] PALETTE = {
            new Color(135,206,250), new Color(144,238,144), new Color(255,182,193),
            new Color(255,228,181), new Color(221,160,221), new Color(240,230,140),
            new Color(176,196,222)
    };

    public SchedulerGUI() {
        super("Process Scheduler System — Improved UI");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 750);
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ex) {
            // ignore, use default
        }
        initUI();
        setLocationRelativeTo(null);
    }

    private void initUI() {
        createMenuBar();
        createToolBar();

        // Input panel (compact)
        JPanel input = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.anchor = GridBagConstraints.WEST;

        pidField = new JTextField(6);
        arrivalField = new JTextField(6);
        burstField = new JTextField(6);
        priorityField = new JTextField(6);
        quantumField = new JTextField(4);
        quantumField.setText("2");

        algoCombo = new JComboBox<>(new String[] {"FCFS", "SJF (Non-preemptive)", "Priority (Non-preemptive)", "Round Robin"});
        algoCombo.setToolTipText("Select scheduling algorithm");
        algoCombo.addActionListener(e -> quantumField.setEnabled("Round Robin".equals(algoCombo.getSelectedItem())));

        JButton addBtn = new JButton("Add");
        addBtn.setToolTipText("Add process (use PID optional)");
        JButton removeBtn = new JButton("Remove");
        JButton clearBtn = new JButton("Clear All");
        JButton runBtn = new JButton("Run Simulation");

        // layout
        int row = 0;
        c.gridx = 0; c.gridy = row; input.add(new JLabel("PID"), c);
        c.gridx = 1; input.add(pidField, c);
        c.gridx = 2; input.add(new JLabel("Arrival"), c);
        c.gridx = 3; input.add(arrivalField, c);
        c.gridx = 4; input.add(new JLabel("Burst"), c);
        c.gridx = 5; input.add(burstField, c);
        row++;
        c.gridy = row; c.gridx = 0; input.add(new JLabel("Priority"), c);
        c.gridx = 1; input.add(priorityField, c);
        c.gridx = 2; input.add(new JLabel("Algorithm"), c);
        c.gridx = 3; input.add(algoCombo, c);
        c.gridx = 4; input.add(new JLabel("Quantum"), c);
        c.gridx = 5; input.add(quantumField, c);
        row++;
        c.gridy = row; c.gridx = 0; input.add(addBtn, c);
        c.gridx = 1; input.add(removeBtn, c);
        c.gridx = 2; input.add(clearBtn, c);
        c.gridx = 3; input.add(runBtn, c);

        // table (editable)
        String[] cols = {"PID", "Arrival", "Burst", "Priority", "Color"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) {
                // allow editing arrival, burst, priority and color
                return col == 1 || col == 2 || col == 3 || col == 4;
            }
        };
        processTable = new JTable(tableModel);
        processTable.setRowHeight(24);
        setupTableRenderers();

        JScrollPane tableScroll = new JScrollPane(processTable);
        tableScroll.setPreferredSize(new Dimension(1000, 180));

        // lower: gantt panel + controls + metrics
        ganttPanel = new GanttPanel();
        JScrollPane ganttScroll = new JScrollPane(ganttPanel);
        ganttScroll.setPreferredSize(new Dimension(1000, 260));

        // zoom slider
        zoomSlider = new JSlider(6, 60, 30);
        zoomSlider.setMajorTickSpacing(12);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setToolTipText("Zoom (pixels per time unit)");
        zoomSlider.addChangeListener(e -> {
            ganttPanel.setScale(zoomSlider.getValue());
            status("Zoom: " + zoomSlider.getValue() + " px/unit");
        });

        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        zoomPanel.add(new JLabel("Zoom:"));
        zoomPanel.add(zoomSlider);

        metricsArea = new JTextArea(7, 80);
        metricsArea.setEditable(false);
        JScrollPane metricsScroll = new JScrollPane(metricsArea);

        // Assemble main content
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(input, BorderLayout.NORTH);
        topPanel.add(tableScroll, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(ganttScroll, BorderLayout.CENTER);
        centerPanel.add(zoomPanel, BorderLayout.SOUTH);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(metricsScroll, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(8,8));
        content.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        content.add(topPanel, BorderLayout.NORTH);
        content.add(centerPanel, BorderLayout.CENTER);
        content.add(bottomPanel, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);

        // status bar
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));
        add(statusLabel, BorderLayout.SOUTH);

        // button actions
        addBtn.addActionListener(e -> onAdd());
        removeBtn.addActionListener(e -> onRemove());
        clearBtn.addActionListener(e -> onClear());
        runBtn.addActionListener(e -> onRun());

        // double-click color chooser for color column
        processTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = processTable.rowAtPoint(e.getPoint());
                    int col = processTable.columnAtPoint(e.getPoint());
                    if (col == 4 && row >= 0) {
                        String hex = (String) tableModel.getValueAt(row, 4);
                        Color initial = hexToColor(hex);
                        Color chosen = JColorChooser.showDialog(SchedulerGUI.this, "Choose color for " + tableModel.getValueAt(row,0), initial);
                        if (chosen != null) {
                            tableModel.setValueAt(colorToHex(chosen), row, 4);
                        }
                    } else if (col >= 1 && col <= 3 && row >= 0) {
                        processTable.editCellAt(row, col);
                    }
                }
            }
        });

        // sample data
        addSampleData();
    }

    private void setupTableRenderers() {
        // Color renderer
        processTable.getColumnModel().getColumn(4).setCellRenderer(new ColorCellRenderer());
        processTable.getColumnModel().getColumn(4).setCellEditor(new ColorCellEditor());
        // center align numeric columns
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        processTable.getColumnModel().getColumn(1).setCellRenderer(center);
        processTable.getColumnModel().getColumn(2).setCellRenderer(center);
        processTable.getColumnModel().getColumn(3).setCellRenderer(center);
    }

    private void createMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem exportCsv = new JMenuItem("Export CSV...");
        JMenuItem savePng = new JMenuItem("Save Gantt as PNG...");
        JMenuItem exit = new JMenuItem("Exit");
        exportCsv.addActionListener(e -> onExportCsv());
        savePng.addActionListener(e -> onSavePng());
        exit.addActionListener(e -> System.exit(0));
        file.add(exportCsv);
        file.add(savePng);
        file.addSeparator();
        file.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Process Scheduler System\nImproved UI\n\nAlgorithms: FCFS, SJF, Priority, Round Robin\nAuthor: ChatGPT",
                "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        mb.add(file);
        mb.add(help);
        setJMenuBar(mb);
    }

    private void createToolBar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        JButton add = new JButton("Add");
        JButton run = new JButton("Run");
        JButton csv = new JButton("CSV");
        JButton png = new JButton("PNG");
        add.setToolTipText("Add process");
        run.setToolTipText("Run simulation");
        csv.setToolTipText("Export CSV");
        png.setToolTipText("Save Gantt PNG");

        add.addActionListener(e -> onAdd());
        run.addActionListener(e -> onRun());
        csv.addActionListener(e -> onExportCsv());
        png.addActionListener(e -> onSavePng());

        tb.add(add);
        tb.add(run);
        tb.addSeparator();
        tb.add(csv);
        tb.add(png);
        add(tb, BorderLayout.NORTH);
    }

    private void addSampleData() {
        safeAddProcess("P1", 0, 5, 2);
        safeAddProcess("P2", 2, 3, 1);
        safeAddProcess("P3", 4, 1, 3);
    }

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
            if (pid.isEmpty()) {
                pid = "P" + (autoPidCounter++);
            }
            safeAddProcess(pid, arrival, burst, priority);
            clearInputs();
            status("Added " + pid);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid integer values (arrival >=0, burst >0).", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void safeAddProcess(String pid, int arrival, int burst, int priority) {
        Color color = PALETTE[(tableModel.getRowCount()) % PALETTE.length];
        tableModel.addRow(new Object[] {pid, arrival, burst, priority, colorToHex(color)});
    }

    private void onRemove() {
        int[] sel = processTable.getSelectedRows();
        if (sel.length == 0) {
            JOptionPane.showMessageDialog(this, "Select one or more rows to remove.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // remove from last to first
        for (int i = sel.length - 1; i >= 0; i--) {
            tableModel.removeRow(processTable.convertRowIndexToModel(sel[i]));
        }
        status("Removed selected processes");
    }

    private void onClear() {
        tableModel.setRowCount(0);
        ganttPanel.setGantt(null);
        metricsArea.setText("");
        autoPidCounter = 1;
        status("Cleared all processes");
    }

    private void onRun() {
        List<ProcessModel> procs = new ArrayList<>();
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            try {
                String pid = tableModel.getValueAt(r, 0).toString();
                int arr = Integer.parseInt(tableModel.getValueAt(r, 1).toString());
                int burst = Integer.parseInt(tableModel.getValueAt(r, 2).toString());
                int pri = Integer.parseInt(tableModel.getValueAt(r, 3).toString());
                Color c = hexToColor(tableModel.getValueAt(r, 4).toString());
                procs.add(new ProcessModel(pid, arr, burst, pri, c));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid table data at row " + (r+1) + ". Check Arrival/Burst/Priority.", "Data Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
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
            } else { // Round Robin
                int quantum = Integer.parseInt(quantumField.getText().trim());
                res = Scheduler.roundRobin(procs, quantum);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error running scheduler: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            return;
        }

        ganttPanel.setGantt(res.gantt);
        displayMetrics(res);
        status("Simulation completed (" + algo + ")");
    }

    private void displayMetrics(Scheduler.Result res) {
        StringBuilder sb = new StringBuilder();
        sb.append("Process Metrics:\n");
        sb.append(String.format("%-8s %-8s %-10s %-12s %-12s\n", "PID","Arrival","Burst","Waiting","Turnaround"));
        // sort by PID for stable view
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

    private void onExportCsv() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No processes to export.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        int rc = fc.showSaveDialog(this);
        if (rc != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getParentFile(), f.getName() + ".csv");
        try (PrintWriter pw = new PrintWriter(f)) {
            pw.println("PID,Arrival,Burst,Priority,Color");
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                pw.printf("%s,%s,%s,%s,%s\n",
                        tableModel.getValueAt(r,0),
                        tableModel.getValueAt(r,1),
                        tableModel.getValueAt(r,2),
                        tableModel.getValueAt(r,3),
                        tableModel.getValueAt(r,4));
            }
            status("Exported CSV: " + f.getName());
            JOptionPane.showMessageDialog(this, "Exported to " + f.getAbsolutePath(), "Export CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSavePng() {
        if (ganttPanel == null) {
            JOptionPane.showMessageDialog(this, "No Gantt to save.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("PNG image", "png"));
        int rc = fc.showSaveDialog(this);
        if (rc != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".png")) f = new File(f.getParentFile(), f.getName() + ".png");
        // render panel to image
        BufferedImage img = new BufferedImage(ganttPanel.getWidth(), ganttPanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        ganttPanel.paint(g2);
        g2.dispose();
        try {
            ImageIO.write(img, "png", f);
            status("Saved Gantt PNG: " + f.getName());
            JOptionPane.showMessageDialog(this, "Saved image to " + f.getAbsolutePath(), "Saved PNG", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save PNG: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearInputs() {
        pidField.setText("");
        arrivalField.setText("");
        burstField.setText("");
        priorityField.setText("");
    }

    private void status(String s) {
        statusLabel.setText(s);
    }

    // Helpers: color hex <-> Color
    private String colorToHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
    private Color hexToColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (Exception e) {
            return Color.LIGHT_GRAY;
        }
    }

    // Color cell renderer
    private static class ColorCellRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            lbl.setOpaque(true);
            try {
                lbl.setBackground(Color.decode(value.toString()));
            } catch (Exception e) {
                lbl.setBackground(Color.LIGHT_GRAY);
            }
            lbl.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            return lbl;
        }
    }

    // Color cell editor (shows a small button to open chooser)
    private class ColorCellEditor extends AbstractCellEditor implements TableCellEditor {
        private JButton button = new JButton();
        private String currentHex = "#cccccc";

        public ColorCellEditor() {
            button.setBorderPainted(false);
            button.addActionListener(e -> {
                Color init = hexToColor(currentHex);
                Color chosen = JColorChooser.showDialog(SchedulerGUI.this, "Choose color", init);
                if (chosen != null) {
                    currentHex = colorToHex(chosen);
                }
                fireEditingStopped();
            });
        }

        public Object getCellEditorValue() {
            return currentHex;
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            currentHex = value == null ? "#cccccc" : value.toString();
            button.setBackground(hexToColor(currentHex));
            return button;
        }
    }

    // Main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SchedulerGUI gui = new SchedulerGUI();
            gui.setVisible(true);
        });
    }
}
