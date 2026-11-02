package com.rasterglyph;

import com.rasterglyph.core.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * Demonstrates the complete RASTER//GLYPH engine in action.
 *
 * Shows:
 * 1. Standard monochrome conversion
 * 2. Floyd–Steinberg error diffusion dithering (smooth gradients)
 * 3. Custom Color Gradient Shading (Cyberpunk neon palette)
 * 4. Sobel edge detection (directional outlines)
 * 5. Cel-shaded hybrid (shading + edges)
 * 6. 24-bit True-Color ANSI output
 * 7. Exported HTML file (retro_art.html)
 * 8. Exported PNG image (retro_ascii.png)
 * 9. Exported Vector SVG (retro_ascii.svg)
 */
public class RasterGlyphDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("           RASTER//GLYPH Core Engine Demo            ");
        System.out.println("==================================================\n");

        // 1. Create a colorful retro synthwave badge image
        BufferedImage demoImage = createRetroColorImage(200, 100);

        // 2. Standard Monochrome (Notice banding in flat character regions)
        System.out.println(">>> 1. Standard Monochrome (No Dithering):");
        AsciiConverter monochromeConverter = new AsciiConverter();
        AsciiResult monoResult = monochromeConverter.convert(demoImage, 60);
        System.out.println(monoResult);

        // 3. With Floyd–Steinberg Dithering (Smooth stippled gradient transitions!)
        System.out.println("\n>>> 2. With Floyd–Steinberg Dithering (Stippled Halftone):");
        AsciiConverter ditheredConverter = new AsciiConverter(
                CharacterMapper.standard(),
                ImageResizer.DEFAULT_CHAR_ASPECT_RATIO,
                false, // monochrome
                true   // dither enabled!
        );
        AsciiResult ditheredResult = ditheredConverter.convert(demoImage, 60);
        System.out.println(ditheredResult);

        // 4. Custom Color Gradient (Cyberpunk Neon mapping)
        System.out.println("\n>>> 3. Cyberpunk Color Gradient Mapping (ColorGradient.CYBERPUNK):");
        AsciiResult cyberpunkResult = ColorGradient.CYBERPUNK.apply(ditheredResult, CharacterSet.STANDARD);
        System.out.println(AnsiRenderer.render(cyberpunkResult));

        // 5. Sobel Edge Detection (Structural Line Art: |, -, /, \)
        System.out.println("\n>>> 4. Sobel Edge Detection (Directional Line Art Outlines):");
        AsciiResult edgeResult = SobelEdgeDetector.toEdgeAscii(demoImage, 60, 45.0);
        System.out.println(edgeResult);

        // 6. Cel-Shaded Hybrid (Shading + Overlaid Edge Strokes)
        System.out.println("\n>>> 5. Cel-Shaded Hybrid (Shading + Overlaid Edges):");
        BufferedImage scaled = ImageResizer.resize(demoImage, 60);
        AsciiResult celShaded = SobelEdgeDetector.overlayEdges(monoResult, scaled, 45.0);
        System.out.println(celShaded);

        // 7. True-Color ANSI 24-bit Output (Original Image Colors)
        System.out.println("\n>>> 6. Original Colors (ANSI 24-bit True-Color):");
        AsciiConverter colorConverter = new AsciiConverter(
                CharacterMapper.standard(),
                ImageResizer.DEFAULT_CHAR_ASPECT_RATIO,
                true,  // Capture RGB colors
                false  // Direct color mapping
        );
        AsciiResult colorResult = colorConverter.convert(demoImage, 60);
        System.out.println(AnsiRenderer.render(colorResult));

        // 8. Export to standalone HTML file
        try {
            Path htmlPath = Path.of("retro_art.html");
            HtmlExporter.exportToFile(cyberpunkResult, htmlPath, HtmlExporter.HtmlOptions.DEFAULT);
            System.out.println("\n>>> 7. Exported HTML file:");
            System.out.println("Saved to: " + htmlPath.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Could not export HTML: " + e.getMessage());
        }

        // 9. Export to PNG raster image file
        try {
            Path pngPath = Path.of("retro_ascii.png");
            ImageExporter.exportToPng(cyberpunkResult, pngPath, ImageExporter.ImageExportOptions.DEFAULT);
            System.out.println("\n>>> 8. Exported PNG image file:");
            System.out.println("Saved to: " + pngPath.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Could not export PNG: " + e.getMessage());
        }

        // 10. Export to resolution-independent SVG vector file
        try {
            Path svgPath = Path.of("retro_ascii.svg");
            SvgExporter.exportToFile(cyberpunkResult, svgPath, SvgExporter.SvgOptions.DEFAULT);
            System.out.println("\n>>> 9. Exported Scalable Vector Graphics (SVG):");
            System.out.println("Saved to: " + svgPath.toAbsolutePath());
            System.out.println("You can zoom in 10,000% in your browser or Figma with zero pixelation!");
        } catch (Exception e) {
            System.err.println("Could not export SVG: " + e.getMessage());
        }

        System.out.println("\n==================================================");
        System.out.println("All demonstrations completed successfully!");
    }

    /**
     * Synthesizes a colorful retro radial gradient graphic.
     */
    private static BufferedImage createRetroColorImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dark background
        g.setColor(new Color(15, 10, 25));
        g.fillRect(0, 0, width, height);

        // Radiant multi-color radial gradient (Cyan -> Magenta -> Amber -> Dark)
        Point2D center = new Point2D.Float(width / 2.0f, height / 2.0f);
        float radius = Math.min(width, height) / 2.0f;
        float[] dist = {0.0f, 0.4f, 0.8f, 1.0f};
        Color[] colors = {
                new Color(0, 255, 230),   // Bright Cyan
                new Color(255, 0, 128),   // Hot Pink / Magenta
                new Color(255, 160, 0),   // Amber Orange
                new Color(20, 10, 40)     // Deep Purple edge
        };
        RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
        g.setPaint(p);
        g.fillOval((int) (center.getX() - radius), (int) (center.getY() - radius), (int) (radius * 2), (int) (radius * 2));

        g.dispose();
        return img;
    }
}
