package com.rasterglyph.ui;

import com.rasterglyph.core.*;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

/**
 * Main JavaFX Desktop Application for RASTER//GLYPH.
 *
 * Implements a Cyberpunk HUD aesthetic inspired by tactical telemetry displays:
 * - Real-time parameter processing
 * - Drag & Drop image importing
 * - Instant export suite (PNG, SVG, HTML, Clipboard)
 * - Telemetry metrics and CRT scanlines
 */
public class RasterGlyphApp extends Application {

    private BufferedImage sourceImage;
    private final ControlDeck controlDeck = new ControlDeck();
    private final AsciiViewport viewport = new AsciiViewport();
    private final PauseTransition pipelineDebounce = new PauseTransition(Duration.millis(20));

    // Telemetry Badges
    private final Label statusBadge = new Label("[ STATUS: ARMED ]");
    private final Label dimsBadge = new Label("[ DIMS: --x-- ]");
    private final Label latencyBadge = new Label("[ LATENCY: 0ms ]");
    private final Label fileBadge = new Label("[ SOURCE: SYNTH_DEMO ]");
    private final Label zoomBadge = new Label("[ ZOOM: 100% ]");

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("RASTER // GLYPH  ──  [ TACTICAL CONVERSION ENGINE v1.0 ]");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0A0B10; -fx-background: #0A0B10;");
        root.setTop(buildHeaderBar());

        // Left control deck wrapped in a minimal dark scroll pane
        ScrollPane controlScroll = new ScrollPane(controlDeck);
        controlScroll.setFitToWidth(true);
        controlScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        controlScroll.setStyle("-fx-background-color: #0A0B10; -fx-background: #0A0B10;");

        root.setLeft(controlScroll);
        root.setCenter(viewport);
        root.setBottom(buildFooterBar());

        Scene scene = new Scene(root, 1280, 820);
        scene.setFill(javafx.scene.paint.Color.web("#0A0B10"));

        // Load retro HUD CSS
        var cssUrl = getClass().getResource("/com/rasterglyph/ui/styles/retro-hud.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        setupDragAndDrop(scene);
        wireControlDeckActions();

        // Synthesize initial demo image so the app boots with vibrant visuals
        this.sourceImage = createDefaultBadgeImage(300, 200);
        updateDisplaySourceImage(this.sourceImage);
        processPipeline();

        stage.setScene(scene);
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.show();
    }

    private HBox buildHeaderBar() {
        Label logo = new Label("RASTER // GLYPH");
        logo.getStyleClass().add("logo-text");

        Label indicator = new Label("[ ■ ■ ■ ■ ]");
        indicator.setStyle("-fx-text-fill: #9D8CFF; -fx-font-size: 11px;");

        statusBadge.getStyleClass().add("status-badge-active");
        fileBadge.getStyleClass().add("status-badge");
        dimsBadge.getStyleClass().add("status-badge");
        latencyBadge.getStyleClass().add("status-badge");
        zoomBadge.getStyleClass().add("status-badge");
        zoomBadge.setStyle("-fx-cursor: hand;");
        zoomBadge.setOnMouseClicked(e -> viewport.resetZoom());

        HBox leftBox = new HBox(12, logo, indicator, fileBadge);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        HBox rightBox = new HBox(8, zoomBadge, dimsBadge, latencyBadge, statusBadge);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(leftBox, rightBox);
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        header.getStyleClass().add("header-bar");
        return header;
    }

    private HBox buildFooterBar() {
        Label telemetry = new Label("// SYSTEM: ONLINE  ──  MEMORY: NOMINAL  ──  COLOR: 24-BIT TRUE-COLOR  ──  ENGINE: JAVA 21");
        HBox footer = new HBox(telemetry);
        footer.getStyleClass().add("footer-bar");
        return footer;
    }

    private void wireControlDeckActions() {
        pipelineDebounce.setOnFinished(e -> processPipeline());
        controlDeck.setOnChangeListener(() -> pipelineDebounce.playFromStart());

        controlDeck.setOnImport(this::handleFileImport);
        controlDeck.setOnExportPng(this::handleExportPng);
        controlDeck.setOnExportSvg(this::handleExportSvg);
        controlDeck.setOnExportHtml(this::handleExportHtml);
        controlDeck.setOnCopy(this::handleCopyClipboard);

        controlDeck.getScanlinesCheck().selectedProperty().addListener((obs, o, n) -> {
            viewport.setShowScanlines(n);
        });

        viewport.setOnZoomChangeListener(zf -> {
            zoomBadge.setText(String.format("[ ZOOM: %d%% ]", Math.round(zf * 100)));
        });
    }

    private void processPipeline() {
        if (sourceImage == null) return;

        long startTime = System.nanoTime();

        try {
            int targetWidth = controlDeck.getTargetWidth();

            // 1. Scale down to targetWidth FIRST (hardware-accelerated via Graphics2D)
            // Downscales millions of pixels to ~15,000 pixels in ~10ms once
            BufferedImage scaled = ImageResizer.resize(sourceImage, targetWidth, ImageResizer.DEFAULT_CHAR_ASPECT_RATIO);

            // 2. Fast Brightness & Contrast on the tiny scaled image
            int brightness = controlDeck.getBrightness();
            double contrast = controlDeck.getContrast();

            if (brightness != 0) {
                scaled = ImageProcessor.adjustBrightness(scaled, brightness);
            }
            if (contrast != 1.0) {
                scaled = ImageProcessor.adjustContrast(scaled, contrast);
            }

            // 3. Keep pre-dithered copy for clean Sobel edge detection
            BufferedImage preDither = scaled;

            // 4. Character Mapping Setup
            CharacterSet charSet = controlDeck.getSelectedCharacterSet();
            boolean inverted = controlDeck.isInvert();
            CharacterMapper mapper = new CharacterMapper(charSet, inverted);

            // 5. Floyd-Steinberg Dithering
            if (controlDeck.isDither()) {
                scaled = DitherFilter.ditherGrayscale(scaled, charSet.length());
            }

            // 6. Direct Conversion (zero redundant resizing)
            AsciiConverter converter = new AsciiConverter(
                    mapper,
                    ImageResizer.DEFAULT_CHAR_ASPECT_RATIO,
                    true,
                    false
            );
            AsciiResult result = converter.convertDirect(scaled);

            // 7. Sobel Edge Overlay (reuses preDither directly without second resize)
            if (controlDeck.isSobel()) {
                result = SobelEdgeDetector.overlayEdges(result, preDither, 45.0);
            }

            // 8. Palette Tinting
            String palette = controlDeck.getSelectedPalette();
            result = applySelectedPalette(result, charSet, palette);

            // 9. Update Viewport with optimal dynamic font size and Before/After source image
            viewport.setPageTheme(controlDeck.getPageTheme());
            viewport.setSplitViewEnabled(controlDeck.isSplitView());
            viewport.displayResult(result, controlDeck.getOptimalFontSize());

            // 10. Update Telemetry Metrics
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            dimsBadge.setText(String.format("[ DIMS: %dx%d // %d%% DETAIL ]",
                    result.getWidth(), result.getHeight(), controlDeck.getDetailPercentage()));
            latencyBadge.setText(String.format("[ LATENCY: %dms ]", elapsedMs));
            statusBadge.setText("[ STATUS: READY ]");

        } catch (Exception ex) {
            statusBadge.setText("[ STATUS: ERROR ]");
            ex.printStackTrace();
        }
    }

    private AsciiResult applySelectedPalette(AsciiResult base, CharacterSet charSet, String palette) {
        return switch (palette) {
            case "CYBERPUNK NEON" -> ColorGradient.CYBERPUNK.apply(base, charSet);
            case "PHOSPHOR GREEN" -> ColorGradient.PHOSPHOR_GREEN.apply(base, charSet);
            case "AMBER CRT" -> ColorGradient.AMBER_CRT.apply(base, charSet);
            case "THERMAL HEATMAP" -> ColorGradient.THERMAL.apply(base, charSet);
            case "MATRIX GREEN" -> ColorGradient.MATRIX.apply(base, charSet);
            case "MONOCHROME INK", "MONOCHROME CYAN" -> new AsciiResult(base.getGridCopy(), null);
            default -> base; // ORIGINAL RGB
        };
    }

    private void handleFileImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("IMPORT IMAGE FILE");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files (PNG, JPG, BMP)", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );

        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            loadFile(file);
        }
    }

    private void loadFile(File file) {
        try {
            this.sourceImage = ImageLoader.load(file);
            fileBadge.setText("[ SOURCE: " + file.getName().toUpperCase() + " ]");
            updateDisplaySourceImage(this.sourceImage);
            processPipeline();
        } catch (Exception e) {
            showError("Load Error", "Failed to load image file:\n" + e.getMessage());
        }
    }

    private void updateDisplaySourceImage(BufferedImage img) {
        if (img == null) return;
        // Cap display proxy to max 2048px to prevent VRAM spikes on massive camera RAWs
        BufferedImage display = img;
        if (img.getWidth() > 2048 || img.getHeight() > 2048) {
            display = ImageResizer.resize(img, Math.min(2048, img.getWidth()));
        }
        viewport.setSourceImage(display);
    }

    private void setupDragAndDrop(Scene scene) {
        scene.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        scene.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                if (ImageLoader.isSupportedExtension(ImageLoader.getFileExtension(file.getName()))) {
                    loadFile(file);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void handleExportPng() {
        AsciiResult current = viewport.getCurrentResult();
        if (current == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("EXPORT AS PNG IMAGE");
        chooser.setInitialFileName("ascii_art.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png"));

        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                String theme = controlDeck.getPageTheme();
                ImageExporter.ImageExportOptions options;
                if ("WHITE PAPER".equalsIgnoreCase(theme)) {
                    options = new ImageExporter.ImageExportOptions(14, Color.WHITE, new Color(18, 18, 24), 24, false);
                } else if ("VINTAGE PARCHMENT".equalsIgnoreCase(theme)) {
                    options = new ImageExporter.ImageExportOptions(14, new Color(244, 238, 219), new Color(40, 27, 16), 24, true);
                } else {
                    options = ImageExporter.ImageExportOptions.DEFAULT;
                }

                ImageExporter.exportToPng(current, file, options);
                statusBadge.setText("[ EXPORTED: PNG ]");
            } catch (Exception e) {
                showError("Export Error", "Failed to export PNG: " + e.getMessage());
            }
        }
    }

    private void handleExportSvg() {
        AsciiResult current = viewport.getCurrentResult();
        if (current == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("EXPORT AS SVG VECTOR");
        chooser.setInitialFileName("ascii_art.svg");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SVG Vector (*.svg)", "*.svg"));

        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                String theme = controlDeck.getPageTheme();
                SvgExporter.SvgOptions options;
                if ("WHITE PAPER".equalsIgnoreCase(theme)) {
                    options = new SvgExporter.SvgOptions(14, "#FFFFFF", "#101116", 24);
                } else if ("VINTAGE PARCHMENT".equalsIgnoreCase(theme)) {
                    options = new SvgExporter.SvgOptions(14, "#F4EEDB", "#281B10", 24);
                } else {
                    options = SvgExporter.SvgOptions.DEFAULT;
                }

                SvgExporter.exportToFile(current, file.toPath(), options);
                statusBadge.setText("[ EXPORTED: SVG ]");
            } catch (Exception e) {
                showError("Export Error", "Failed to export SVG: " + e.getMessage());
            }
        }
    }

    private void handleExportHtml() {
        AsciiResult current = viewport.getCurrentResult();
        if (current == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("EXPORT AS HTML DOCUMENT");
        chooser.setInitialFileName("ascii_art.html");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML File (*.html)", "*.html"));

        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                String theme = controlDeck.getPageTheme();
                HtmlExporter.HtmlOptions options;
                if ("WHITE PAPER".equalsIgnoreCase(theme)) {
                    options = new HtmlExporter.HtmlOptions("RASTER//GLYPH White Paper", "#FFFFFF", "#101116", 11, false);
                } else if ("VINTAGE PARCHMENT".equalsIgnoreCase(theme)) {
                    options = new HtmlExporter.HtmlOptions("RASTER//GLYPH Vintage Parchment", "#F4EEDB", "#281B10", 11, true);
                } else {
                    options = HtmlExporter.HtmlOptions.DEFAULT;
                }

                HtmlExporter.exportToFile(current, file.toPath(), options);
                statusBadge.setText("[ EXPORTED: HTML ]");
            } catch (Exception e) {
                showError("Export Error", "Failed to export HTML: " + e.getMessage());
            }
        }
    }

    private void handleCopyClipboard() {
        AsciiResult current = viewport.getCurrentResult();
        if (current == null) return;

        ClipboardContent content = new ClipboardContent();
        content.putString(current.toPlainText());
        Clipboard.getSystemClipboard().setContent(content);

        statusBadge.setText("[ COPIED TO CLIPBOARD ]");
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static BufferedImage createDefaultBadgeImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dark background
        g.setColor(new Color(15, 12, 22));
        g.fillRect(0, 0, width, height);

        // Synthwave radiant badge
        Point2D center = new Point2D.Float(width / 2.0f, height / 2.0f);
        float radius = Math.min(width, height) / 2.0f - 10;
        float[] dist = {0.0f, 0.45f, 0.85f, 1.0f};
        Color[] colors = {
                new Color(0, 255, 230),   // Electric Cyan
                new Color(255, 0, 128),   // Hot Pink
                new Color(255, 170, 0),   // Warm Amber
                new Color(20, 10, 35)     // Deep Violet
        };
        RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
        g.setPaint(p);
        g.fillOval((int) (center.getX() - radius), (int) (center.getY() - radius), (int) (radius * 2), (int) (radius * 2));

        g.dispose();
        return img;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
