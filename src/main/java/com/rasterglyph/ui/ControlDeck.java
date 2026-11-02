package com.rasterglyph.ui;

import com.rasterglyph.core.CharacterSet;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Tactical control deck housing sliders, toggles, page themes, and export triggers
 * styled with cyberpunk brackets and technical labels.
 */
public final class ControlDeck extends VBox {

    // --- State Properties ---
    private final Slider detailSlider = new Slider(10, 100, 45);
    private final Slider brightnessSlider = new Slider(-100, 100, 0);
    private final Slider contrastSlider = new Slider(0.2, 3.0, 1.0);

    private final CheckBox ditherCheck = new CheckBox("FLOYD-STEINBERG DITHER");
    private final CheckBox sobelCheck = new CheckBox("SOBEL EDGE OVERLAY");
    private final CheckBox invertCheck = new CheckBox("INVERT MAPPING");
    private final CheckBox scanlinesCheck = new CheckBox("CRT SCANLINES");
    private final CheckBox splitCheck = new CheckBox("SPLIT WIPE [ ◄ ► ]");

    private final ComboBox<String> pageThemeCombo = new ComboBox<>();
    private final ComboBox<String> charSetCombo = new ComboBox<>();
    private final ComboBox<String> paletteCombo = new ComboBox<>();

    private final Button importBtn = new Button("┌ [ IMPORT IMAGE ] ┐");
    private final Button exportPngBtn = new Button("┌ [ PNG IMAGE ] ┐");
    private final Button exportSvgBtn = new Button("┌ [ SVG VECTOR ] ┐");
    private final Button exportHtmlBtn = new Button("└ [ HTML DOC ] ┘");
    private final Button copyBtn = new Button("└ [ COPY TEXT ] ┘");

    private final Label detailValLabel = new Label("[ 45% // BALANCED ]");
    private final Label brightValLabel = new Label("[ 0 ]");
    private final Label contrastValLabel = new Label("[ 1.0 ]");

    private Runnable onChangeListener;

    public ControlDeck() {
        setSpacing(14);
        setPadding(new Insets(16));
        setPrefWidth(330);
        setMinWidth(300);
        setMaxWidth(360);
        getStyleClass().add("hud-panel");

        buildLayout();
        attachInternalListeners();
    }

    private void buildLayout() {
        // 1. Image Import
        importBtn.setMaxWidth(Double.MAX_VALUE);
        importBtn.getStyleClass().add("button-accent");

        // 2. Page Theme Selection (Dark CRT vs White Paper vs Parchment)
        pageThemeCombo.getItems().addAll(
                "DARK CRT (Black)",
                "WHITE PAPER (Clean)",
                "VINTAGE PARCHMENT"
        );
        pageThemeCombo.setValue("DARK CRT (Black)");
        pageThemeCombo.setMaxWidth(Double.MAX_VALUE);

        // 3. Character Set Selection
        charSetCombo.getItems().addAll("STANDARD (10 chars)", "EXTENDED (70 chars)", "BINARY (# )", "BLOCKS (█▓▒░ )");
        charSetCombo.setValue("STANDARD (10 chars)");
        charSetCombo.setMaxWidth(Double.MAX_VALUE);

        // 4. Color Palette Selection
        paletteCombo.getItems().addAll(
                "ORIGINAL RGB",
                "CYBERPUNK NEON",
                "PHOSPHOR GREEN",
                "AMBER CRT",
                "THERMAL HEATMAP",
                "MATRIX GREEN",
                "MONOCHROME INK"
        );
        paletteCombo.setValue("ORIGINAL RGB");
        paletteCombo.setMaxWidth(Double.MAX_VALUE);

        // 5. Parameter Sliders with bracketed value badges
        VBox detailBox = createSliderRow("DETAIL LEVEL", detailSlider, detailValLabel);
        VBox brightBox = createSliderRow("BRIGHTNESS DELTA", brightnessSlider, brightValLabel);
        VBox contrastBox = createSliderRow("CONTRAST MULTIPLIER", contrastSlider, contrastValLabel);

        // 6. Filter Toggles
        scanlinesCheck.setSelected(true);
        ditherCheck.setSelected(true);
        splitCheck.setSelected(false);

        VBox toggleBox = new VBox(8, splitCheck, ditherCheck, sobelCheck, invertCheck, scanlinesCheck);

        // 7. Export Action Suite (2x2 Grid so full names are completely visible)
        exportPngBtn.setMaxWidth(Double.MAX_VALUE);
        exportSvgBtn.setMaxWidth(Double.MAX_VALUE);
        exportHtmlBtn.setMaxWidth(Double.MAX_VALUE);
        copyBtn.setMaxWidth(Double.MAX_VALUE);

        HBox exportRow1 = new HBox(8, exportPngBtn, exportSvgBtn);
        HBox exportRow2 = new HBox(8, exportHtmlBtn, copyBtn);
        HBox.setHgrow(exportPngBtn, Priority.ALWAYS);
        HBox.setHgrow(exportSvgBtn, Priority.ALWAYS);
        HBox.setHgrow(exportHtmlBtn, Priority.ALWAYS);
        HBox.setHgrow(copyBtn, Priority.ALWAYS);

        VBox exportSuite = new VBox(6, exportRow1, exportRow2);

        // Assemble Deck
        getChildren().addAll(
                createSectionTitle("// 01: IMAGE INPUT"),
                importBtn,
                createSectionTitle("// 02: PAGE THEME & CANVAS"),
                pageThemeCombo,
                createSectionTitle("// 03: PARAMETERS"),
                detailBox,
                brightBox,
                contrastBox,
                createSectionTitle("// 04: CHARACTER & PALETTE"),
                charSetCombo,
                paletteCombo,
                createSectionTitle("// 05: FILTER MATRIX"),
                toggleBox,
                createSectionTitle("// 06: EXPORT SUITE"),
                exportSuite
        );
    }

    private VBox createSliderRow(String title, Slider slider, Label valueLabel) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #7B85A0; -fx-font-size: 10px;");

        valueLabel.setStyle("-fx-text-fill: #00FFE6; -fx-font-size: 10px; -fx-font-weight: bold;");

        HBox labelRow = new HBox(titleLabel, valueLabel);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        labelRow.setAlignment(Pos.CENTER_LEFT);

        slider.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, labelRow, slider);
    }

    private Label createSectionTitle(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("hud-panel-title");
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    private void attachInternalListeners() {
        detailSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int val = newVal.intValue();
            String mode;
            if (val <= 30) {
                mode = "LO-FI RETRO";
            } else if (val <= 70) {
                mode = "BALANCED";
            } else {
                mode = "ULTRA HD";
            }
            detailValLabel.setText(String.format("[ %d%% // %s ]", val, mode));
            triggerChange();
        });

        brightnessSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            brightValLabel.setText(String.format("[ %d ]", newVal.intValue()));
            triggerChange();
        });

        contrastSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            contrastValLabel.setText(String.format("[ %.1f ]", newVal.doubleValue()));
            triggerChange();
        });

        pageThemeCombo.valueProperty().addListener((obs, o, n) -> triggerChange());
        charSetCombo.valueProperty().addListener((obs, o, n) -> triggerChange());
        paletteCombo.valueProperty().addListener((obs, o, n) -> triggerChange());
        ditherCheck.selectedProperty().addListener((obs, o, n) -> triggerChange());
        sobelCheck.selectedProperty().addListener((obs, o, n) -> triggerChange());
        invertCheck.selectedProperty().addListener((obs, o, n) -> triggerChange());
        splitCheck.selectedProperty().addListener((obs, o, n) -> triggerChange());
    }

    private void triggerChange() {
        if (onChangeListener != null) {
            onChangeListener.run();
        }
    }

    // --- Accessors for Pipeline State ---
    public int getDetailPercentage() {
        return (int) Math.round(detailSlider.getValue());
    }

    /**
     * Translates the Detail Percentage (10% to 100%) into an optimal target ASCII width (35 to 220 characters).
     */
    public int getTargetWidth() {
        int pct = getDetailPercentage();
        return 35 + (int) Math.round((pct / 100.0) * 185);
    }

    /**
     * Calculates the optimal font size based on target width to ensure the ASCII artwork
     * remains comfortably framed and centered inside the viewport across all detail levels.
     */
    public double getOptimalFontSize() {
        int w = getTargetWidth();
        return Math.clamp(900.0 / w, 5.0, 16.0);
    }

    public int getBrightness() {
        return (int) Math.round(brightnessSlider.getValue());
    }

    public double getContrast() {
        return contrastSlider.getValue();
    }

    public boolean isDither() {
        return ditherCheck.isSelected();
    }

    public boolean isSobel() {
        return sobelCheck.isSelected();
    }

    public boolean isInvert() {
        return invertCheck.isSelected();
    }

    public boolean isScanlines() {
        return scanlinesCheck.isSelected();
    }

    public boolean isSplitView() {
        return splitCheck.isSelected();
    }

    public String getPageTheme() {
        String val = pageThemeCombo.getValue();
        if (val != null && val.contains("WHITE PAPER")) return "WHITE PAPER";
        if (val != null && val.contains("PARCHMENT")) return "VINTAGE PARCHMENT";
        return "DARK CRT";
    }

    public CharacterSet getSelectedCharacterSet() {
        String val = charSetCombo.getValue();
        if (val.startsWith("EXTENDED")) return CharacterSet.EXTENDED;
        if (val.startsWith("BINARY")) return CharacterSet.BINARY;
        if (val.startsWith("BLOCKS")) return CharacterSet.BLOCKS;
        return CharacterSet.STANDARD;
    }

    public String getSelectedPalette() {
        return paletteCombo.getValue();
    }

    public void setOnChangeListener(Runnable listener) {
        this.onChangeListener = listener;
    }

    public void setOnImport(Runnable action) {
        importBtn.setOnAction(e -> action.run());
    }

    public void setOnExportPng(Runnable action) {
        exportPngBtn.setOnAction(e -> action.run());
    }

    public void setOnExportSvg(Runnable action) {
        exportSvgBtn.setOnAction(e -> action.run());
    }

    public void setOnExportHtml(Runnable action) {
        exportHtmlBtn.setOnAction(e -> action.run());
    }

    public void setOnCopy(Runnable action) {
        copyBtn.setOnAction(e -> action.run());
    }

    public CheckBox getScanlinesCheck() {
        return scanlinesCheck;
    }
}
