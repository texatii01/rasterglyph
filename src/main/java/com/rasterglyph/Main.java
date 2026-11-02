package com.rasterglyph;

import com.rasterglyph.ui.RasterGlyphApp;

/**
 * Main application launcher for RASTER//GLYPH.
 *
 * In modern Java (Java 11+), directly running a class that extends
 * {@link javafx.application.Application} triggers a strict JVM module-path check
 * unless run with VM arguments.
 *
 * Running this separate launcher class (which does not extend Application)
 * bypasses that check and launches JavaFX cleanly on all IDEs and platforms.
 */
public class Main {

    public static void main(String[] args) {
        RasterGlyphApp.main(args);
    }
}
