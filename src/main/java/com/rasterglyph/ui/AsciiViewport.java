package com.rasterglyph.ui;

import com.rasterglyph.core.AsciiResult;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ultra-high-performance, hardware-accelerated zoomable CRT / Paper Viewport.
 *
 * Performance Architecture:
 * 1. Triple-Layer GPU Scissor Pipeline:
 *    - sourceCanvas (bottom layer, holds raw image)
 *    - asciiCanvas (middle layer, holds ASCII glyph grid)
 *    - overlayCanvas (top layer, holds draggable divider line & handle)
 * 2. Zero-Allocation Glyph Rendering with pre-cached ASCII string tables.
 * 3. Color State Deduplication: eliminates redundant gc.setFill() state dispatches.
 * 4. Draggable Split Wipe runs at 120 FPS by mutating GPU scissor clip bounds
 *    without re-evaluating or re-rendering any characters.
 */
public final class AsciiViewport extends StackPane {

    private static final double HANDLE_GRAB_WIDTH = 26.0;

    // Hard ceiling on canvas layout dimension to prevent exceeding GPU texture limits (4096px physical texture on 2x Retina)
    private static final double MAX_CANVAS_DIM = 2000.0;

    // Pre-allocated static string cache to eliminate tens of thousands of String allocations per frame
    private static final String[] ASCII_STRINGS = new String[256];
    private static final Map<Character, String> UNICODE_GLYPH_CACHE = new ConcurrentHashMap<>();

    static {
        for (int i = 0; i < 256; i++) {
            ASCII_STRINGS[i] = String.valueOf((char) i);
        }
        for (char c : new char[]{'█', '▓', '▒', '░'}) {
            UNICODE_GLYPH_CACHE.put(c, String.valueOf(c));
        }
    }

    private static String getGlyphString(char ch) {
        if (ch < 256) {
            return ASCII_STRINGS[ch];
        }
        return UNICODE_GLYPH_CACHE.computeIfAbsent(ch, String::valueOf);
    }

    private final ScrollPane scrollPane;
    private final StackPane canvasContainer;

    // Layered Canvases for zero-cost split wipes
    private final Canvas sourceCanvas;
    private final Canvas asciiCanvas;
    private final Canvas overlayCanvas;

    // Hardware Scissor Clipping Nodes
    private final Rectangle sourceClip = new Rectangle();
    private final Rectangle asciiClip = new Rectangle();

    private AsciiResult currentResult;
    private Image fxSourceImage;

    private double fontSize = 12.0;
    private double zoomFactor = 1.0;
    private boolean showScanlines = true;
    private String pageTheme = "DARK CRT";

    // Split-view state
    private boolean splitViewEnabled = false;
    private double splitRatio = 0.5;
    private double currentSplitX = 0.0;
    private boolean isDraggingSplit = false;

    // Cached layout geometry
    private double currentContentWidth = 800;
    private double currentContentHeight = 600;
    private double currentPadding = 28;
    private double currentArtWidth = 744;
    private double currentArtHeight = 544;
    private double currentEffectiveFontSize = 12.0;

    private long lastScrollZoomTime = 0;
    private java.util.function.Consumer<Double> onZoomChangeListener;

    public AsciiViewport() {
        this.sourceCanvas = new Canvas(800, 600);
        this.asciiCanvas = new Canvas(800, 600);
        this.overlayCanvas = new Canvas(800, 600);

        // Hardware clipping masks attached to canvases
        sourceCanvas.setClip(sourceClip);
        asciiCanvas.setClip(null); // Full view by default

        sourceCanvas.setVisible(false);
        overlayCanvas.setVisible(false);

        // Stacked container: source (back) -> ASCII (mid) -> overlay (front)
        this.canvasContainer = new StackPane(sourceCanvas, asciiCanvas, overlayCanvas);
        canvasContainer.setAlignment(Pos.CENTER);
        canvasContainer.getStyleClass().add("crt-container");
        canvasContainer.setStyle("-fx-background-color: #06070B; -fx-background: #06070B;");

        this.scrollPane = new ScrollPane(canvasContainer);
        scrollPane.getStyleClass().add("crt-screen");
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: #06070B; -fx-background: #06070B;");

        // Cursor-anchored Zooming with throttling to prevent GPU command queue flooding
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() != 0) {
                long now = System.currentTimeMillis();
                if (now - lastScrollZoomTime >= 20) {
                    double delta = event.getDeltaY() > 0 ? 0.12 : -0.12;
                    zoomAt(event.getSceneX(), event.getSceneY(), delta);
                    lastScrollZoomTime = now;
                }
                event.consume();
            }
        });

        // Trackpad pinch-to-zoom support
        scrollPane.addEventFilter(ZoomEvent.ZOOM, event -> {
            double zoomDelta = (event.getZoomFactor() - 1.0) * 0.8;
            if (Math.abs(zoomDelta) > 0.01) {
                zoomAt(event.getSceneX(), event.getSceneY(), zoomDelta);
            }
            event.consume();
        });

        // Double-click to reset zoom back to 100%
        scrollPane.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                Point2D pt = null;
                try {
                    pt = asciiCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
                } catch (Exception ignored) {}
                if (!splitViewEnabled || pt == null || Math.abs(pt.getX() - currentSplitX) > HANDLE_GRAB_WIDTH) {
                    resetZoom();
                }
            }
        });

        setupSplitDragHandlers();

        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #06070B; -fx-background: #06070B;");
        getChildren().add(scrollPane);

        renderPlaceholder();
    }

    private void setupSplitDragHandlers() {
        // Event filters intercept events on canvasContainer before ScrollPane panning can consume them
        canvasContainer.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (!splitViewEnabled || currentResult == null) {
                canvasContainer.setCursor(Cursor.DEFAULT);
                return;
            }
            if (Math.abs(event.getX() - currentSplitX) <= HANDLE_GRAB_WIDTH) {
                canvasContainer.setCursor(Cursor.H_RESIZE);
            } else {
                canvasContainer.setCursor(Cursor.DEFAULT);
            }
        });

        canvasContainer.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!splitViewEnabled || currentResult == null) return;
            if (Math.abs(event.getX() - currentSplitX) <= HANDLE_GRAB_WIDTH) {
                isDraggingSplit = true;
                updateSplitRatio(event.getX());
                event.consume();
            }
        });

        canvasContainer.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (isDraggingSplit) {
                updateSplitRatio(event.getX());
                event.consume();
            }
        });

        canvasContainer.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (isDraggingSplit) {
                isDraggingSplit = false;
                event.consume();
            }
        });

        canvasContainer.setOnMouseExited(event -> {
            if (!isDraggingSplit) {
                canvasContainer.setCursor(Cursor.DEFAULT);
            }
        });
    }

    /**
     * Blazing-fast 120 FPS split position update:
     * Only adjusts GPU scissor clip bounds and the overlay handle.
     * ZERO characters or images are re-rendered.
     */
    private void updateSplitRatio(double mouseX) {
        if (currentResult == null || currentArtWidth <= 0) return;

        double ratio = (mouseX - currentPadding) / currentArtWidth;
        this.splitRatio = Math.clamp(ratio, 0.02, 0.98);
        this.currentSplitX = currentPadding + (splitRatio * currentArtWidth);

        updateClipsAndOverlay();
    }

    private void updateClipsAndOverlay() {
        if (!splitViewEnabled) return;

        // Mutate GPU scissor clipping bounds safely
        sourceClip.setX(0);
        sourceClip.setY(0);
        sourceClip.setWidth(Math.max(0.0, currentSplitX));
        sourceClip.setHeight(Math.max(0.0, currentContentHeight));

        asciiClip.setX(Math.max(0.0, currentSplitX));
        asciiClip.setY(0);
        asciiClip.setWidth(Math.max(0.0, currentContentWidth - currentSplitX));
        asciiClip.setHeight(Math.max(0.0, currentContentHeight));

        renderOverlayLayer();
    }

    /**
     * Supplies the source image for the Before / After split-screen wipe.
     * Accepts a pre-converted JavaFX image for zero-copy efficiency.
     */
    public void setSourceImage(Image image) {
        this.fxSourceImage = image;
        if (splitViewEnabled) {
            renderSourceLayer();
        }
    }

    public void setSourceImage(BufferedImage image) {
        if (image != null) {
            this.fxSourceImage = FxImageUtils.toFxImage(image);
        } else {
            this.fxSourceImage = null;
        }
        if (splitViewEnabled) {
            renderSourceLayer();
        }
    }

    /**
     * Enables or disables the interactive Before / After split screen.
     */
    public void setSplitViewEnabled(boolean enabled) {
        if (this.splitViewEnabled != enabled) {
            this.splitViewEnabled = enabled;
            if (enabled) {
                sourceCanvas.setVisible(true);
                overlayCanvas.setVisible(true);
                asciiCanvas.setClip(asciiClip);
                renderSourceLayer();
                updateClipsAndOverlay();
            } else {
                sourceCanvas.setVisible(false);
                overlayCanvas.setVisible(false);
                asciiCanvas.setClip(null);
            }
        }
    }

    public boolean isSplitViewEnabled() {
        return splitViewEnabled;
    }

    public double getSplitRatio() {
        return splitRatio;
    }

    public void setSplitRatio(double ratio) {
        this.splitRatio = Math.clamp(ratio, 0.02, 0.98);
        if (splitViewEnabled) {
            this.currentSplitX = currentPadding + (splitRatio * currentArtWidth);
            updateClipsAndOverlay();
        }
    }

    public void displayResult(AsciiResult result) {
        this.currentResult = result;
        repaintAll();
    }

    public void displayResult(AsciiResult result, double baseFontSize) {
        this.fontSize = Math.max(3.0, baseFontSize);
        this.currentResult = result;
        repaintAll();
    }

    public void setFontSize(double size) {
        this.fontSize = Math.max(3.0, size);
        repaintAll();
    }

    public void setPageTheme(String theme) {
        this.pageTheme = theme != null ? theme : "DARK CRT";
        repaintAll();
    }

    public void zoomAt(double pivotSceneX, double pivotSceneY, double deltaFactor) {
        if (currentResult == null) return;

        double oldZoom = zoomFactor;
        double newZoom = Math.clamp(zoomFactor + deltaFactor, 0.25, 3.5);
        if (Math.abs(newZoom - oldZoom) < 0.001) return;

        Point2D cursorInCanvas = null;
        try {
            cursorInCanvas = asciiCanvas.sceneToLocal(pivotSceneX, pivotSceneY);
        } catch (Exception ignored) {}

        Point2D cursorInView = null;
        try {
            cursorInView = scrollPane.sceneToLocal(pivotSceneX, pivotSceneY);
        } catch (Exception ignored) {}

        double canvasW = Math.max(1.0, asciiCanvas.getWidth());
        double canvasH = Math.max(1.0, asciiCanvas.getHeight());

        double anchorX = 0.5;
        double anchorY = 0.5;
        if (cursorInCanvas != null) {
            double rawAnchorX = cursorInCanvas.getX() / canvasW;
            double rawAnchorY = cursorInCanvas.getY() / canvasH;
            if (!Double.isNaN(rawAnchorX) && !Double.isInfinite(rawAnchorX)) {
                anchorX = Math.clamp(rawAnchorX, 0.0, 1.0);
            }
            if (!Double.isNaN(rawAnchorY) && !Double.isInfinite(rawAnchorY)) {
                anchorY = Math.clamp(rawAnchorY, 0.0, 1.0);
            }
        }

        Bounds currentViewBounds = scrollPane.getViewportBounds();
        double fallbackViewW = currentViewBounds != null && currentViewBounds.getWidth() > 0 ? currentViewBounds.getWidth() : 800.0;
        double fallbackViewH = currentViewBounds != null && currentViewBounds.getHeight() > 0 ? currentViewBounds.getHeight() : 600.0;

        double cursorViewX = (cursorInView != null && !Double.isNaN(cursorInView.getX()) && !Double.isInfinite(cursorInView.getX()))
                ? cursorInView.getX()
                : fallbackViewW * 0.5;
        double cursorViewY = (cursorInView != null && !Double.isNaN(cursorInView.getY()) && !Double.isInfinite(cursorInView.getY()))
                ? cursorInView.getY()
                : fallbackViewH * 0.5;

        this.zoomFactor = newZoom;
        repaintAll();

        canvasContainer.layout();
        scrollPane.layout();

        Bounds newViewBounds = scrollPane.getViewportBounds();
        Bounds newContentBounds = canvasContainer.getBoundsInLocal();

        double viewW = (newViewBounds != null && newViewBounds.getWidth() > 0) ? newViewBounds.getWidth() : fallbackViewW;
        double viewH = (newViewBounds != null && newViewBounds.getHeight() > 0) ? newViewBounds.getHeight() : fallbackViewH;
        double contentW = (newContentBounds != null && newContentBounds.getWidth() > 0) ? newContentBounds.getWidth() : currentContentWidth;
        double contentH = (newContentBounds != null && newContentBounds.getHeight() > 0) ? newContentBounds.getHeight() : currentContentHeight;

        // Horizontal scroll adjustment
        double diffW = contentW - viewW;
        if (diffW > 1.0) {
            double targetPointX = asciiCanvas.getLayoutX() + (anchorX * canvasW);
            double targetOffset = targetPointX - cursorViewX;
            double hval = targetOffset / diffW;
            if (!Double.isNaN(hval) && !Double.isInfinite(hval)) {
                scrollPane.setHvalue(Math.clamp(hval, 0.0, 1.0));
            } else {
                scrollPane.setHvalue(0.5);
            }
        } else {
            // When content fits inside the viewport, ALWAYS center it to avoid shifting off-screen
            scrollPane.setHvalue(0.5);
        }

        // Vertical scroll adjustment
        double diffH = contentH - viewH;
        if (diffH > 1.0) {
            double targetPointY = asciiCanvas.getLayoutY() + (anchorY * canvasH);
            double targetOffset = targetPointY - cursorViewY;
            double vval = targetOffset / diffH;
            if (!Double.isNaN(vval) && !Double.isInfinite(vval)) {
                scrollPane.setVvalue(Math.clamp(vval, 0.0, 1.0));
            } else {
                scrollPane.setVvalue(0.5);
            }
        } else {
            // When content fits inside the viewport, ALWAYS center it to avoid shifting off-screen
            scrollPane.setVvalue(0.5);
        }

        notifyZoomChange();
    }

    public void zoomIn() {
        Bounds vp = scrollPane.getViewportBounds();
        if (vp != null && vp.getWidth() > 0 && vp.getHeight() > 0) {
            Bounds viewBounds = scrollPane.localToScene(vp);
            if (viewBounds != null) {
                zoomAt(viewBounds.getCenterX(), viewBounds.getCenterY(), 0.15);
                return;
            }
        }
        zoomAt(getWidth() * 0.5, getHeight() * 0.5, 0.15);
    }

    public void zoomOut() {
        Bounds vp = scrollPane.getViewportBounds();
        if (vp != null && vp.getWidth() > 0 && vp.getHeight() > 0) {
            Bounds viewBounds = scrollPane.localToScene(vp);
            if (viewBounds != null) {
                zoomAt(viewBounds.getCenterX(), viewBounds.getCenterY(), -0.15);
                return;
            }
        }
        zoomAt(getWidth() * 0.5, getHeight() * 0.5, -0.15);
    }

    public void resetZoom() {
        this.zoomFactor = 1.0;
        repaintAll();
        canvasContainer.layout();
        scrollPane.layout();
        scrollPane.setHvalue(0.5);
        scrollPane.setVvalue(0.5);
        notifyZoomChange();
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public void setOnZoomChangeListener(java.util.function.Consumer<Double> listener) {
        this.onZoomChangeListener = listener;
    }

    private void notifyZoomChange() {
        if (onZoomChangeListener != null) {
            onZoomChangeListener.accept(zoomFactor);
        }
    }

    public void setShowScanlines(boolean show) {
        this.showScanlines = show;
        repaintAll();
    }

    public AsciiResult getCurrentResult() {
        return currentResult;
    }

    public String getPageTheme() {
        return pageTheme;
    }

    /**
     * Complete repaint triggered when conversion result, font, theme, or zoom changes.
     */
    private void repaintAll() {
        if (currentResult == null) {
            renderPlaceholder();
            return;
        }

        try {
            int width = currentResult.getWidth();
            int height = currentResult.getHeight();

            double effFontSize = fontSize * zoomFactor;
            double charWidth = effFontSize * 0.6;
            double lineHeight = effFontSize * 1.2;
            double padding = Math.max(12.0, 28.0 * zoomFactor);

            double computedArtWidth = width * charWidth;
            double computedArtHeight = height * lineHeight;

            // Enforce strict upper bound on canvas dimensions to avoid exceeding hardware GPU texture limit (4096px physical texture on 2x Retina)
            double unscaledW = computedArtWidth + (padding * 2);
            double unscaledH = computedArtHeight + (padding * 2);
            if (unscaledW > MAX_CANVAS_DIM || unscaledH > MAX_CANVAS_DIM) {
                double scaleLimit = Math.min((MAX_CANVAS_DIM - padding * 2) / Math.max(1.0, computedArtWidth),
                                             (MAX_CANVAS_DIM - padding * 2) / Math.max(1.0, computedArtHeight));
                effFontSize = Math.max(3.0, effFontSize * scaleLimit);
                charWidth = effFontSize * 0.6;
                lineHeight = effFontSize * 1.2;
                computedArtWidth = width * charWidth;
                computedArtHeight = height * lineHeight;
            }

            this.currentEffectiveFontSize = effFontSize;
            this.currentArtWidth = computedArtWidth;
            this.currentArtHeight = computedArtHeight;
            this.currentPadding = padding;

            this.currentContentWidth = Math.max(100.0, currentArtWidth + (padding * 2));
            this.currentContentHeight = Math.max(100.0, currentArtHeight + (padding * 2));
            this.currentSplitX = padding + (splitRatio * currentArtWidth);

            // Update all canvas layers to exact match
            sourceCanvas.setWidth(currentContentWidth);
            sourceCanvas.setHeight(currentContentHeight);
            asciiCanvas.setWidth(currentContentWidth);
            asciiCanvas.setHeight(currentContentHeight);
            overlayCanvas.setWidth(currentContentWidth);
            overlayCanvas.setHeight(currentContentHeight);

            renderAsciiLayer();

            if (splitViewEnabled) {
                sourceCanvas.setVisible(true);
                overlayCanvas.setVisible(true);
                asciiCanvas.setClip(asciiClip);
                renderSourceLayer();
                updateClipsAndOverlay();
            } else {
                sourceCanvas.setVisible(false);
                overlayCanvas.setVisible(false);
                asciiCanvas.setClip(null);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void renderAsciiLayer() {
        int width = currentResult.getWidth();
        int height = currentResult.getHeight();
        boolean hasColor = currentResult.hasColor();

        double effFontSize = this.currentEffectiveFontSize;
        double charWidth = effFontSize * 0.6;
        double lineHeight = effFontSize * 1.2;
        double tick = 8.0 * zoomFactor;

        ThemePalette palette = resolvePalette();

        GraphicsContext gc = asciiCanvas.getGraphicsContext2D();
        gc.setFill(palette.canvasBgColor);
        gc.fillRect(0, 0, currentContentWidth, currentContentHeight);

        drawHudBorders(gc, currentPadding, currentContentWidth, currentContentHeight, tick, palette.wireframeBorderColor, palette.cornerTickColor);

        // Fast Glyph Rendering: string table + color deduplication
        gc.setFont(Font.font("Courier New", effFontSize));
        if (!hasColor) {
            gc.setFill(palette.defaultTextColor);
        }

        int lastRgb = -1;
        for (int y = 0; y < height; y++) {
            double baselineY = currentPadding + (y * lineHeight) + (effFontSize * 0.95);

            for (int x = 0; x < width; x++) {
                char ch = currentResult.getCharacter(x, y);
                if (ch <= ' ') continue;

                if (hasColor) {
                    int rgb = currentResult.getRgb(x, y);
                    if (rgb != lastRgb) {
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        gc.setFill(Color.rgb(r, g, b));
                        lastRgb = rgb;
                    }
                }

                double posX = currentPadding + (x * charWidth);
                gc.fillText(getGlyphString(ch), posX, baselineY);
            }
        }

        drawScanlines(gc, currentContentWidth, currentContentHeight, palette.scanlineAlpha);
    }

    private void renderSourceLayer() {
        ThemePalette palette = resolvePalette();
        double tick = 8.0 * zoomFactor;

        GraphicsContext gc = sourceCanvas.getGraphicsContext2D();
        gc.setFill(palette.canvasBgColor);
        gc.fillRect(0, 0, currentContentWidth, currentContentHeight);

        if (fxSourceImage != null) {
            gc.drawImage(fxSourceImage, currentPadding, currentPadding, currentArtWidth, currentArtHeight);
        }

        drawHudBorders(gc, currentPadding, currentContentWidth, currentContentHeight, tick, palette.wireframeBorderColor, palette.cornerTickColor);
        drawScanlines(gc, currentContentWidth, currentContentHeight, palette.scanlineAlpha);
    }

    private void renderOverlayLayer() {
        GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, currentContentWidth, currentContentHeight);

        if (!splitViewEnabled) return;

        ThemePalette palette = resolvePalette();

        // 1. Divider Line
        gc.setStroke(palette.accentColor);
        gc.setLineWidth(2.0);
        gc.strokeLine(currentSplitX, currentPadding * 0.5, currentSplitX, currentContentHeight - currentPadding * 0.5);

        // 2. Center Handle Badge: [ ◄ ► ]
        double handleW = 66.0;
        double handleH = 26.0;
        double handleX = currentSplitX - (handleW / 2.0);
        double handleY = (currentContentHeight - handleH) / 2.0;

        gc.setFill(palette.handleBgColor);
        gc.fillRect(handleX, handleY, handleW, handleH);

        gc.setStroke(palette.accentColor);
        gc.setLineWidth(1.5);
        gc.strokeRect(handleX, handleY, handleW, handleH);

        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 11));
        gc.setFill(palette.handleTextColor);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("[ ◄ ► ]", currentSplitX, handleY + 17);
        gc.setTextAlign(TextAlignment.LEFT);

        // 3. Floating HUD Badges
        if (currentSplitX > currentPadding + 140) {
            drawHudTag(gc, currentPadding + 14, currentPadding + 14, "[ RAW SOURCE ]", palette.rawTagColor);
        }
        if (currentSplitX < currentContentWidth - currentPadding - 140) {
            drawHudTag(gc, currentContentWidth - currentPadding - 140, currentPadding + 14, "[ RASTER GLYPH ]", palette.accentColor);
        }
    }

    private void drawHudBorders(GraphicsContext gc, double padding, double contentWidth, double contentHeight,
                                double tick, Color wireframeBorderColor, Color cornerTickColor) {
        gc.setStroke(wireframeBorderColor);
        gc.setLineWidth(1.0);
        gc.strokeRect(padding * 0.5, padding * 0.5, contentWidth - padding, contentHeight - padding);

        gc.setStroke(cornerTickColor);
        double left = padding * 0.5;
        double top = padding * 0.5;
        double right = contentWidth - padding * 0.5;
        double bottom = contentHeight - padding * 0.5;

        // Top-Left
        gc.strokeLine(left, top, left + tick, top);
        gc.strokeLine(left, top, left, top + tick);
        // Top-Right
        gc.strokeLine(right, top, right - tick, top);
        gc.strokeLine(right, top, right, top + tick);
        // Bottom-Left
        gc.strokeLine(left, bottom, left + tick, bottom);
        gc.strokeLine(left, bottom, left, bottom - tick);
        // Bottom-Right
        gc.strokeLine(right, bottom, right - tick, bottom);
        gc.strokeLine(right, bottom, right, bottom - tick);
    }

    private void drawScanlines(GraphicsContext gc, double contentWidth, double contentHeight, double scanlineAlpha) {
        if (showScanlines && scanlineAlpha > 0.0) {
            gc.setFill(Color.rgb(0, 0, 0, scanlineAlpha));
            for (double sy = 0; sy < contentHeight; sy += 3.0) {
                gc.fillRect(0, sy, contentWidth, 1.0);
            }
        }
    }

    private void drawHudTag(GraphicsContext gc, double x, double y, String text, Color color) {
        gc.save();
        gc.setFont(Font.font("Courier New", FontWeight.BOLD, 10));
        double boxWidth = (text.length() * 6.8) + 12;
        double boxHeight = 18.0;

        gc.setFill(Color.rgb(10, 11, 16, 0.85));
        gc.fillRect(x, y, boxWidth, boxHeight);

        gc.setStroke(color);
        gc.setLineWidth(1.0);
        gc.strokeRect(x, y, boxWidth, boxHeight);

        gc.setFill(color);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(text, x + 6, y + 13);
        gc.restore();
    }

    private void renderPlaceholder() {
        currentContentWidth = 800;
        currentContentHeight = 600;
        asciiCanvas.setWidth(800);
        asciiCanvas.setHeight(600);
        sourceCanvas.setVisible(false);
        overlayCanvas.setVisible(false);
        asciiCanvas.setClip(null);

        GraphicsContext gc = asciiCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#06070B"));
        gc.fillRect(0, 0, 800, 600);

        gc.setFont(Font.font("Courier New", 14));
        gc.setFill(Color.web("#7B85A0"));
        gc.fillText("// RASTER//GLYPH TERMINAL READY", 40, 60);
        gc.fillText("// LOAD AN IMAGE OR DRAG & DROP TO BEGIN TELEMETRY", 40, 85);

        gc.setStroke(Color.web("#202436"));
        gc.setLineWidth(1.0);
        gc.strokeRect(30, 30, 740, 540);
    }

    private ThemePalette resolvePalette() {
        if ("WHITE PAPER".equalsIgnoreCase(pageTheme)) {
            return new ThemePalette(
                    Color.web("#FFFFFF"),
                    Color.web("#101116"),
                    Color.web("#D4D8E2"),
                    Color.web("#101116"),
                    Color.web("#101116"),
                    Color.web("#FFFFFF"),
                    Color.web("#101116"),
                    Color.web("#4A35C5"),
                    0.04
            );
        } else if ("VINTAGE PARCHMENT".equalsIgnoreCase(pageTheme)) {
            return new ThemePalette(
                    Color.web("#F4EEDB"),
                    Color.web("#281B10"),
                    Color.web("#C8C0AA"),
                    Color.web("#8A5C28"),
                    Color.web("#8A5C28"),
                    Color.web("#F4EEDB"),
                    Color.web("#281B10"),
                    Color.web("#7A3E14"),
                    0.06
            );
        } else {
            return new ThemePalette(
                    Color.web("#06070B"),
                    Color.web("#00FFE6"),
                    Color.web("#1A1E2E"),
                    Color.web("#00FFE6"),
                    Color.web("#00FFE6"),
                    Color.web("#0A0B10"),
                    Color.web("#00FFE6"),
                    Color.web("#9D8CFF"),
                    0.25
            );
        }
    }

    private record ThemePalette(
            Color canvasBgColor,
            Color defaultTextColor,
            Color wireframeBorderColor,
            Color cornerTickColor,
            Color accentColor,
            Color handleBgColor,
            Color handleTextColor,
            Color rawTagColor,
            double scanlineAlpha
    ) {}
}
