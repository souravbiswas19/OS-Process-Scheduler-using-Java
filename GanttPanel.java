// GanttPanel.java
// Simple Gantt chart Swing panel.

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GanttPanel extends JPanel {
    private List<Scheduler.GanttEntry> gantt;
    private static final int ROW_HEIGHT = 40;
    private static final int LEFT_PADDING = 40;
    private static final int PIXELS_PER_UNIT = 30; // scale; change if timeline too wide

    public GanttPanel() {
        setPreferredSize(new Dimension(800, 200));
    }

    public void setGantt(List<Scheduler.GanttEntry> gantt) {
        this.gantt = gantt;
        int width = LEFT_PADDING + (gantt == null || gantt.isEmpty() ? 400
                : (gantt.get(gantt.size()-1).end * PIXELS_PER_UNIT + 100));
        setPreferredSize(new Dimension(Math.max(width, 600), ROW_HEIGHT + 60));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        if (gantt == null || gantt.isEmpty()) {
            g.setColor(Color.DARK_GRAY);
            g.drawString("No Gantt chart to display. Run simulation.", 20, 40);
            return;
        }

        // draw time axis
        int yTop = 20;
        int yBar = yTop + 10;
        g.setColor(Color.BLACK);
        g.drawString("Time →", 5, yTop + ROW_HEIGHT / 2);

        // draw each entry
        for (Scheduler.GanttEntry e : gantt) {
            int x = LEFT_PADDING + e.start * PIXELS_PER_UNIT;
            int w = Math.max(1, (e.end - e.start) * PIXELS_PER_UNIT);
            g.setColor(e.color != null ? e.color : Color.LIGHT_GRAY);
            g.fillRect(x, yBar, w, ROW_HEIGHT - 10);
            g.setColor(Color.BLACK);
            g.drawRect(x, yBar, w, ROW_HEIGHT - 10);

            // write pid centered if space
            String label = e.pid + " (" + e.start + "-" + e.end + ")";
            FontMetrics fm = g.getFontMetrics();
            int strW = fm.stringWidth(label);
            if (strW + 6 < w) {
                g.drawString(label, x + (w - strW) / 2, yBar + (ROW_HEIGHT - 10) / 2 + 5);
            } else {
                // write truncated
                String s = e.pid + " " + e.start + "-" + e.end;
                g.drawString(s, x + 4, yBar + (ROW_HEIGHT - 10) / 2 + 5);
            }
        }

        // time ticks
        int maxTime = gantt.get(gantt.size()-1).end;
        for (int t = 0; t <= maxTime; t++) {
            int x = LEFT_PADDING + t * PIXELS_PER_UNIT;
            g.setColor(Color.GRAY);
            g.drawLine(x, yBar + ROW_HEIGHT - 5, x, yBar + ROW_HEIGHT + 5);
            g.setColor(Color.BLACK);
            g.drawString(Integer.toString(t), x - 4, yBar + ROW_HEIGHT + 20);
        }
    }
}
