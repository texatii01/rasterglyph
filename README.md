# RASTER // GLYPH

```
██████╗  █████╗ ███████╗████████╗███████╗██████╗  ██████╗ ██╗  ██╗   ██╗██████╗ ██╗  ██╗
██╔══██╗██╔══██╗██╔════╝╚══██╔══╝██╔════╝██╔══██╗██╔════╝ ██║  ╚██╗ ██╔╝██╔══██╗██║  ██║
██████╔╝███████║███████╗   ██║   █████╗  ██████╔╝██║  ███╗██║   ╚████╔╝ ██████╔╝███████║
██╔══██╗██╔══██║╚════██║   ██║   ██╔══╝  ██╔══██╗██║   ██║██║    ╚██╔╝  ██╔═══╝ ██╔══██║
██║  ██║██║  ██║███████║   ██║   ███████╗██║  ██║╚██████╔╝███████╗██║   ██║     ██║  ██║
╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝   ╚═╝   ╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝   ╚═╝     ╚═╝  ╚═╝
```

<div align="center">

**[ TACTICAL RASTER-TO-GLYPH SYNTHESIS ENGINE v1.0 ]**

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX 21](https://img.shields.io/badge/JavaFX-21-007396?style=for-the-badge&logo=java&logoColor=white)
![Release](https://img.shields.io/github/v/release/texatii01/rasterglyph?style=for-the-badge&color=00FFE6&logo=github)
![Tests Passing](https://img.shields.io/badge/Tests-78%2F78%20PASSING-00FFE6?style=for-the-badge&logo=checkmarx&logoColor=black)
![License MIT](https://img.shields.io/badge/License-MIT-9D8CFF?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-macOS%20|%20Linux%20|%20Windows-white?style=for-the-badge)

*A high-performance, retro-cyberpunk desktop studio and headless engine for transforming bitmap images into high-density ASCII, ANSI true-color, vector SVG, and HTML telemetry art.*

</div>

---

## Overview

**RASTER//GLYPH** bridges the gap between retro 1970s terminal telemetry displays and modern computer graphics pipelines. Built on **Java 21** and **JavaFX**, it offers instantaneous, real-time conversion with Floyd–Steinberg error diffusion dithering, Sobel cel-shaded edge overlays, multi-stop false-color gradients, and an interactive **Before / After Split-Screen Wipe**.

Designed with a strict separation between a **100% headless, zero-dependency conversion engine** (`com.rasterglyph.core`) and an **ultra-responsive, hardware-accelerated Cyberpunk HUD desktop client** (`com.rasterglyph.ui`).

---

## Key Features

### Interactive Before / After Split Wipe
- Real-time vertical split screen comparing the raw input image directly against the live ASCII rendering.
- Drag the tactile glowing handle `[ ◄ ► ]` left and right to wipe across the viewport.
- **Triple-Layer GPU Scissor Pipeline**: Mutates hardware scissor clipping masks in `<0.05ms`, maintaining rock-solid **120 FPS** responsiveness without re-evaluating or re-rasterizing text glyphs during drag.

### Ultra-High-Performance Architecture
- **Resize-First Pipeline**: Downsamples high-resolution camera images (12MP–40MP) *before* pixel loops, dropping processing latency from 2,500ms down to **~10ms**.
- **Direct Raster Array Operations**: Uses direct `DataBufferInt` 1D array operations, bypassing 2D coordinate lookups and bounds checks.
- **Zero-Allocation Glyph Renderer**: Employs static ASCII glyph lookup tables and color state deduplication, eliminating tens of thousands of heap allocations per frame.
- **Smooth 20ms Slider Debouncing**: Keeps slider thumbs completely liquid smooth at 60 FPS without event queue backlog.

### Cursor-Anchored Viewport Zoom
- Direct scroll-wheel zoom spanning **25% to 600%**.
- Mathematically anchored: the exact pixel under your cursor remains pinned in place while zooming in or out.
- Double-click anywhere on the canvas to immediately snap back to 100% view.

### Color Gradients & Palettes
- **Original RGB**: 24-bit True-Color extraction directly from the source image.
- **Cyberpunk Neon**: Electric cyan `#00FFE6` to phosphor violet `#9D8CFF`.
- **Phosphor Green**: P1 monochrome terminal phosphor `#33FF33`.
- **Amber CRT**: P3 warm monochrome amber `#FFB000`.
- **Thermal Heatmap**: False-color infrared spectrum (Blue $\rightarrow$ Cyan $\rightarrow$ Yellow $\rightarrow$ Red).
- **Matrix Green**: High-contrast phosphor green gradient.
- **Monochrome Ink**: Clean grayscale terminal output.

### Multi-Theme Canvas
- **DARK CRT**: Void-black background (`#06070B`), neon accents, subtle CRT scanlines, and technical HUD corner brackets.
- **WHITE PAPER**: Stark clean white paper (`#FFFFFF`) with dark charcoal ink (`#101116`) for print and editorial publishing.
- **VINTAGE PARCHMENT**: Warm textured cream paper (`#F4EEDB`) with sepia ink (`#281B10`).

### Advanced Filters & Edge Detection
- **Floyd–Steinberg Error Diffusion**: Eliminates tonal banding across smooth gradients with uniform halftone stippling.
- **Sobel Directional Line Art**: $3 \times 3$ convolution kernels compute gradient magnitude and map directional angles to character strokes (`|`, `-`, `/`, `\`).
- **Cel-Shaded Hybrid Overlay**: Injects structural edge strokes directly over shaded ASCII character fills.

### Quad-Export Action Suite
1. **`[ PNG IMAGE ]`**: High-resolution rasterization rendered with clean monospace font metrics.
2. **`[ SVG VECTOR ]`**: Resolution-independent XML vector art with preserved character positions and embedded styles.
3. **`[ HTML DOC ]`**: Standalone web document with responsive CSS, deduplicated color spans, and optional scanlines.
4. **`[ COPY TEXT ]`**: Copies pure plain-text ASCII art directly to system clipboard.

---

## Architecture

```
com.rasterglyph
│
├── core/                       # 100% Headless Conversion Engine (Zero GUI Dependencies)
│   ├── CharacterSet.java       # Standard, Extended, Binary, and Block character palettes
│   ├── CharacterMapper.java    # Uniform luminance-to-glyph mapping & inversion
│   ├── AsciiResult.java        # Immutable character & RGB 2D grid bundle
│   ├── ImageLoader.java        # Safe loading and extension validation for PNG, JPG, BMP
│   ├── ImageResizer.java       # Aspect-ratio corrected bilinear downsampler
│   ├── ImageProcessor.java     # Direct-raster luminance, contrast, brightness, & inversion
│   ├── AsciiConverter.java     # Pipeline coordinator with direct & dithered pipelines
│   ├── DitherFilter.java       # Floyd–Steinberg 2D error diffusion algorithm
│   ├── SobelEdgeDetector.java  # 3x3 convolution edge detection & directional strokes
│   ├── ColorGradient.java      # Multi-stop linear RGB gradient interpolator
│   ├── AnsiRenderer.java       # 24-bit ANSI terminal escape sequence renderer
│   ├── ImageExporter.java      # Monospace font rasterizer to PNG
│   ├── SvgExporter.java        # Resolution-independent vector SVG exporter
│   └── HtmlExporter.java       # CRT styled standalone HTML exporter
│
└── ui/                         # JavaFX Cyberpunk HUD & Viewport Layer
    ├── Main.java               # Standalone JVM launcher bypassing module-path checks
    ├── RasterGlyphApp.java     # Primary HUD window, drag-and-drop, & telemetry header
    ├── ControlDeck.java        # Tactical control sidebar with bracketed parameter badges
    ├── AsciiViewport.java      # Triple-canvas hardware scissor viewport & 120 FPS split wipe
    ├── FxImageUtils.java       # Fast AWT BufferedImage <-> JavaFX Image bridge
    └── styles/retro-hud.css    # Void-black, neon cyan, and square-thumb stylesheet
```

---

## Download & Installation

Pre-packaged native installers bundle their own lightweight Java runtime—**no Java installation required**:

| Platform | Package | Format | Description |
| :--- | :--- | :--- | :--- |
| **macOS** | [**`RasterGlyph-1.0.0.dmg`**](https://github.com/texatii01/rasterglyph/releases/latest) | Apple Disk Image (`.dmg`) | Drag-and-drop to `/Applications`. Runs natively on Apple Silicon & Intel. |
| **Windows** | [**`RasterGlyph-1.0.0.msi`**](https://github.com/texatii01/rasterglyph/releases/latest) | Windows Installer (`.msi`) | Setup wizard; adds Start Menu and Desktop shortcuts. |
| **Linux** | [**`rasterglyph_1.0.0_amd64.deb`**](https://github.com/texatii01/rasterglyph/releases/latest) | Debian / Ubuntu Package (`.deb`) | Install via `sudo dpkg -i rasterglyph_1.0.0_amd64.deb`. |
| **Universal** | [**`rasterglyph-1.0.0-all.jar`**](https://github.com/texatii01/rasterglyph/releases/latest) | Standalone Executable JAR | Run anywhere via `java -jar rasterglyph-1.0.0-all.jar` (Java 21+). |

---

## Building from Source (Developers)

### Prerequisites
- **JDK 21** or later (LTS recommended)
- **Gradle 8.5+** (or use the included `./gradlew`)

### Running the Desktop App
```bash
# Clone the repository
git clone https://github.com/texatii01/rasterglyph.git
cd rasterglyph

# Launch via Gradle
./gradlew run
```

*Alternatively, run `com.rasterglyph.Main` directly inside your favorite IDE (IntelliJ IDEA, Eclipse, VS Code).*

### Running Tests
Execute the full automated unit test suite (78 tests covering all conversion filters, exporters, and resizers):
```bash
./gradlew test
```

---

## Headless API Usage

You can use `com.rasterglyph.core` as a standalone Java library without launching the GUI:

```java
import com.rasterglyph.core.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class Example {
    public static void main(String[] args) {
        // 1. Load source image
        BufferedImage image = ImageLoader.load(new File("photo.jpg"));

        // 2. Configure converter with Floyd-Steinberg dithering
        CharacterMapper mapper = new CharacterMapper(CharacterSet.EXTENDED, false);
        AsciiConverter converter = new AsciiConverter(mapper, 0.5, true, true);

        // 3. Convert image to 120-column ASCII result
        AsciiResult result = converter.convert(image, 120);

        // 4. Print 24-bit ANSI True-Color directly to terminal
        System.out.println(AnsiRenderer.render(result));

        // 5. Export to Vector SVG and standalone HTML
        SvgExporter.export(result, new File("output.svg"));
        HtmlExporter.export(result, new File("output.html"), true);
    }
}
```

---

## Telemetry Controls & Shortcuts

| Action | Control | Description |
| :--- | :--- | :--- |
| **Zoom In / Out** | `Scroll Wheel` | Zooms into the exact pixel under the mouse cursor ($25\% \dots 600\%$) |
| **Reset Zoom** | `Double-Click` or click `[ ZOOM ]` | Resets zoom factor to $100\%$ and centers artwork |
| **Pan Viewport** | `Click & Drag Canvas` | Pans canvas when artwork exceeds viewport bounds |
| **Before / After Wipe** | `Click & Drag [ ◄ ► ]` | Slides split divider to compare raw image vs ASCII |
| **Import Image** | `Drag & Drop` | Drop any PNG, JPG, or BMP file directly onto the window |

---

## AI Assistance Disclosure

This project was developed with the assistance of AI tools for code generation, architecture planning, performance optimization, and documentation, under human engineering direction and review.

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
