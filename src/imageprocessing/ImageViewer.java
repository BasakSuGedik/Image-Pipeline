package imageprocessing;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;

/**
 * ImageViewer.java
 *
 * A simple Swing GUI that lets the user:
 *   1. Load any JPG/PNG image from disk
 *   2. Run the Adaptive Pipeline on it
 *   3. See the BEFORE / AFTER images side by side
 *   4. View benchmark stats: time, thread count, complexity, load balance
 *
 * Run this class directly in Eclipse:
 *   Right-click ImageViewer.java -> Run As -> Java Application
 */
public class ImageViewer extends JFrame {

    private static final long serialVersionUID = 1L;

    // UI components
    private JLabel originalLabel;
    private JLabel processedLabel;
    private JLabel statsLabel;
    private JLabel titleOriginal;
    private JLabel titleProcessed;
    private JButton loadButton;
    private JButton processButton;
    private JProgressBar progressBar;

    private BufferedImage originalImage;
    private BufferedImage processedImage;

    private static final int PREVIEW_W = 520;
    private static final int PREVIEW_H = 400;

    // Colors
    private static final Color BG        = new Color(18, 18, 28);
    private static final Color PANEL_BG  = new Color(28, 28, 42);
    private static final Color ACCENT    = new Color(70, 130, 220);
    private static final Color TEXT      = new Color(220, 220, 235);
    private static final Color TEXT_DIM  = new Color(140, 140, 160);
    private static final Color SUCCESS   = new Color(80, 200, 120);

    public ImageViewer() {
        super("CENG-479 — Adaptive Parallel Image Processing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(BG);
        buildUI();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        // ── TOP HEADER ────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(0, 0, 18, 0));

        JLabel title = new JLabel("Adaptive Parallel Image Processing Pipeline");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT);

        JLabel subtitle = new JLabel("CENG-479  |  Başak Su Gedik & Salih Kırlıoğlu  |  Gazi Üniversitesi");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_DIM);

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);

        // ── IMAGE PANELS ──────────────────────────────────────────────────────
        JPanel imagesPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        imagesPanel.setBackground(BG);

        // Original
        JPanel leftPanel = makeImagePanel();
        titleOriginal = makePanelTitle("ORIGINAL");
        originalLabel = makeImagePlaceholder("Load an image to begin");
        leftPanel.add(titleOriginal, BorderLayout.NORTH);
        leftPanel.add(originalLabel, BorderLayout.CENTER);

        // Processed
        JPanel rightPanel = makeImagePanel();
        titleProcessed = makePanelTitle("PROCESSED");
        processedLabel = makeImagePlaceholder("Output will appear here");
        rightPanel.add(titleProcessed, BorderLayout.NORTH);
        rightPanel.add(processedLabel, BorderLayout.CENTER);

        imagesPanel.add(leftPanel);
        imagesPanel.add(rightPanel);

        // ── STATS BAR ─────────────────────────────────────────────────────────
        JPanel statsPanel = new JPanel(new BorderLayout());
        statsPanel.setBackground(PANEL_BG);
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 50, 70), 1),
            new EmptyBorder(12, 16, 12, 16)
        ));

        statsLabel = new JLabel("Load an image and click Process to see results.");
        statsLabel.setFont(new Font("Segoe UI Mono", Font.PLAIN, 13));
        statsLabel.setForeground(TEXT_DIM);
        statsPanel.add(statsLabel, BorderLayout.CENTER);

        // ── PROGRESS BAR ──────────────────────────────────────────────────────
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setBackground(PANEL_BG);
        progressBar.setForeground(ACCENT);
        progressBar.setPreferredSize(new Dimension(0, 6));
        progressBar.setBorderPainted(false);

        // ── BUTTONS ───────────────────────────────────────────────────────────
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonPanel.setBackground(BG);
        buttonPanel.setBorder(new EmptyBorder(16, 0, 0, 0));

        loadButton = makeButton("📂  Load Image", ACCENT);
        processButton = makeButton("⚡  Process with Adaptive Pipeline", new Color(60, 160, 90));
        processButton.setEnabled(false);

        loadButton.addActionListener(this::onLoad);
        processButton.addActionListener(this::onProcess);

        buttonPanel.add(loadButton);
        buttonPanel.add(processButton);

        // ── ASSEMBLE ──────────────────────────────────────────────────────────
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setBackground(BG);
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        bottomPanel.add(statsPanel,  BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        root.add(header,      BorderLayout.NORTH);
        root.add(imagesPanel, BorderLayout.CENTER);
        root.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── EVENT HANDLERS ────────────────────────────────────────────────────────

    private void onLoad(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select an Image");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image files (JPG, PNG, BMP)", "jpg", "jpeg", "png", "bmp"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                originalImage = ImageIO.read(file);
                if (originalImage == null) {
                    showError("Could not read image file.");
                    return;
                }
                // Show preview
                originalLabel.setIcon(scaleToPreview(originalImage));
                originalLabel.setText("");
                titleOriginal.setText("ORIGINAL  (" + originalImage.getWidth() + " × " + originalImage.getHeight() + ")");

                // Reset processed panel
                processedLabel.setIcon(null);
                processedLabel.setText("Click Process to apply pipeline");
                titleProcessed.setText("PROCESSED");
                statsLabel.setText("Image loaded: " + file.getName() +
                    "  |  Size: " + originalImage.getWidth() + "×" + originalImage.getHeight() +
                    "  |  Ready to process.");
                statsLabel.setForeground(TEXT_DIM);

                processButton.setEnabled(true);
                processedImage = null;

            } catch (Exception ex) {
                showError("Error loading image: " + ex.getMessage());
            }
        }
    }

    private void onProcess(ActionEvent e) {
        if (originalImage == null) return;

        loadButton.setEnabled(false);
        processButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        statsLabel.setText("Processing... please wait.");
        statsLabel.setForeground(TEXT_DIM);

        // Run pipeline on background thread to keep UI responsive
        SwingWorker<AdaptivePipeline.PipelineResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AdaptivePipeline.PipelineResult doInBackground() throws Exception {
                int w = originalImage.getWidth();
                int h = originalImage.getHeight();
                processedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

                // Convert original to TYPE_INT_RGB if needed
                BufferedImage src = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = src.createGraphics();
                g.drawImage(originalImage, 0, 0, null);
                g.dispose();

                return AdaptivePipeline.run(src, processedImage);
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                loadButton.setEnabled(true);
                processButton.setEnabled(true);

                try {
                    AdaptivePipeline.PipelineResult result = get();

                    // Show processed image
                    processedLabel.setIcon(scaleToPreview(processedImage));
                    processedLabel.setText("");
                    titleProcessed.setText("PROCESSED  (" +
                        processedImage.getWidth() + " × " + processedImage.getHeight() + ")");

                    // Show stats
                    DecimalFormat df = new DecimalFormat("0.000");
                    String stats = String.format(
                        "⏱ Time: %d ms     🧵 Threads used: %d     " +
                        "📊 Complexity: %s     ⚖ Load balance ratio: %s",
                        result.totalMs,
                        result.threadCount,
                        df.format(result.avgComplexity),
                        df.format(result.loadBalanceRatio())
                    );
                    statsLabel.setText(stats);
                    statsLabel.setForeground(SUCCESS);

                } catch (Exception ex) {
                    statsLabel.setText("Error: " + ex.getMessage());
                    statsLabel.setForeground(Color.RED);
                }
            }
        };
        worker.execute();
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private JPanel makeImagePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(PANEL_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 50, 75), 1),
            new EmptyBorder(12, 12, 12, 12)
        ));
        p.setPreferredSize(new Dimension(PREVIEW_W + 24, PREVIEW_H + 60));
        return p;
    }

    private JLabel makePanelTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(ACCENT);
        l.setBorder(new EmptyBorder(0, 0, 6, 0));
        return l;
    }

    private JLabel makeImagePlaceholder(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(TEXT_DIM);
        l.setPreferredSize(new Dimension(PREVIEW_W, PREVIEW_H));
        l.setBackground(new Color(22, 22, 35));
        l.setOpaque(true);
        l.setBorder(BorderFactory.createLineBorder(new Color(45, 45, 65), 1));
        return l;
    }

    private JButton makeButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(260, 40));
        return b;
    }

    private ImageIcon scaleToPreview(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        double scale = Math.min((double) PREVIEW_W / w, (double) PREVIEW_H / h);
        int nw = (int) (w * scale);
        int nh = (int) (h * scale);
        Image scaled = img.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── MAIN ──────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new ImageViewer();
        });
    }
}
