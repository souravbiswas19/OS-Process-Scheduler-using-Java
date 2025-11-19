// GanttPanel.java
// Improved Gantt chart Swing panel: zoomable, rounded bars, better layout.

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class GanttPanel extends JPanel {
    private List<Scheduler.GanttEntry> gantt;
    private static final int ROW_HEIGHT = 40;
    private static final int LEFT_PADDING = 60;
    private int pixelsPerUnit = 30; // configurable zoom
    private final Font timeFont = new Font("SansSerif", Font.PLAIN, 11);
    private final Font labelFont = new Font("SansSerif", Font.BOLD, 12);

    public GanttPanel() {
        setPreferredSize(new Dimension(900, 220));
        setBackground(Color.white);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,220,220)),
                BorderFactory.createEmptyBorder(8,8,8,8)));
    }

    public void setGantt(List<Scheduler.GanttEntry> gantt) {
        this.gantt = gantt;
        updatePreferredSize();
        revalidate();
        repaint();
    }

    public void setScale(int pixelsPerUnit) {
        this.pixelsPerUnit = Math.max(4, pixelsPerUnit);
        updatePreferredSize();
        revalidate();
        repaint();
    }

    private void updatePreferredSize() {
        int width = LEFT_PADDING + (gantt == null || gantt.isEmpty() ? 400
                : (gantt.get(gantt.size()-1).end * pixelsPerUnit + 160));
        int height = ROW_HEIGHT + 120;
        setPreferredSize(new Dimension(Math.max(width, 700), height));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // background gradient subtle
        Paint old = g.getPaint();
        g.setPaint(new GradientPaint(0, 0, Color.WHITE, 0, getHeight(), new Color(250,250,250)));
        g.fillRect(0,0,getWidth(), getHeight());
        g.setPaint(old);

        if (gantt == null || gantt.isEmpty()) {
            g.setColor(new Color(120,120,120));
            g.setFont(labelFont);
            g.drawString("No Gantt chart — run simulation to see scheduling timeline.", 20, 50);
            g.dispose();
            return;
        }

        // title
        g.setFont(labelFont);
        g.setColor(new Color(60,60,60));
        g.drawString("Gantt Chart (timeline in time units)", LEFT_PADDING - 40, 20);

        int yTop = 40;
        int yBar = yTop + 10;

        // draw timeline ticks and grid
        int maxTime = gantt.get(gantt.size()-1).end;
        for (int t = 0; t <= maxTime; t++) {
            int x = LEFT_PADDING + t * pixelsPerUnit;
            // faint vertical line
            g.setColor(new Color(230,230,230));
            g.drawLine(x, yBar - 6, x, yBar + ROW_HEIGHT + 6);
            // time label
            g.setColor(new Color(80,80,80));
            g.setFont(timeFont);
            g.drawString(Integer.toString(t), x - 6, yBar + ROW_HEIGHT + 24);
        }

        // draw each gantt entry as rounded bar
        for (Scheduler.GanttEntry e : gantt) {
            int x = LEFT_PADDING + e.start * pixelsPerUnit;
            int w = Math.max(1, (e.end - e.start) * pixelsPerUnit);
            int h = ROW_HEIGHT - 10;
            int arc = Math.max(6, h / 3);
            Color barColor = e.color != null ? e.color : new Color(180, 180, 180);

            // darker border
            g.setColor(barColor);
            RoundRectangle2D bar = new RoundRectangle2D.Float(x, yBar, w, h, arc, arc);
            g.fill(bar);

            // subtle shading
            g.setPaint(new GradientPaint(x, yBar, barColor.brighter(), x + w, yBar + h, barColor.darker()));
            g.fill(bar);

            g.setColor(barColor.darker().darker());
            g.setStroke(new BasicStroke(1f));
            g.draw(bar);

            // label: pid and time (try center)
            String label = e.pid + " (" + e.start + "-" + e.end + ")";
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            int strW = fm.stringWidth(label);
            if (strW + 8 < w) {
                g.drawString(label, x + (w - strW) / 2, yBar + h/2 + fm.getAscent()/2 - 3);
            } else {
                // left align truncated
                String s = e.pid + " " + e.start + "-" + e.end;
                g.drawString(s, x + 6, yBar + h/2 + fm.getAscent()/2 - 3);
            }
        }

        // legend (unique PIDs)
        int lx = LEFT_PADDING;
        int ly = yBar + ROW_HEIGHT + 40;
        g.setFont(timeFont);
        g.setColor(new Color(70,70,70));
        g.drawString("Legend:", lx, ly);
        lx += 60;
        int drawn = 0;
        for (Scheduler.GanttEntry e : gantt) {
            if (drawn > 8) break; // don't overcrowd
            Color c = e.color != null ? e.color : Color.GRAY;
            g.setColor(c);
            g.fillRect(lx, ly - 12, 28, 12);
            g.setColor(Color.BLACK);
            g.drawRect(lx, ly - 12, 28, 12);
            g.drawString(e.pid, lx + 34, ly - 2);
            lx += 90;
            drawn++;
        }

        g.dispose();
    }
}
