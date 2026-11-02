# RasterGlyph

```
██████╗  █████╗ ███████╗████████╗███████╗██████╗  ██████╗ ██╗  ██╗   ██╗██████╗ ██╗  ██╗
██╔══██╗██╔══██╗██╔════╝╚══██╔══╝██╔════╝██╔══██╗██╔════╝ ██║  ╚██╗ ██╔╝██╔══██╗██║  ██║
██████╔╝███████║███████╗   ██║   █████╗  ██████╔╝██║  ███╗██║   ╚████╔╝ ██████╔╝███████║
██╔══██╗██╔══██║╚════██║   ██║   ██╔══╝  ██╔══██╗██║   ██║██║    ╚██╔╝  ██╔═══╝ ██╔══██║
██║  ██║██║  ██║███████║   ██║   ███████╗██║  ██║╚██████╔╝███████╗██║   ██║     ██║  ██║
╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝   ╚═╝   ╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝   ╚═╝     ╚═╝  ╚═╝
```

<div align="center">

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX 21](https://img.shields.io/badge/JavaFX-21-007396?style=for-the-badge&logo=java&logoColor=white)
![Release](https://img.shields.io/github/v/release/texatii01/rasterglyph?style=for-the-badge&color=00FFE6&logo=github)
![Tests Passing](https://img.shields.io/badge/Tests-78%2F78%20PASSING-00FFE6?style=for-the-badge&logo=checkmarx&logoColor=black)
![License MIT](https://img.shields.io/badge/License-MIT-9D8CFF?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-macOS%20|%20Linux%20|%20Windows-white?style=for-the-badge)

*An interactive desktop application and headless Java library for converting images to ASCII art, ANSI terminal output, SVG vectors, and standalone HTML.*

</div>

---

## Overview

RasterGlyph is an image-to-ASCII converter built with **Java 21** and **JavaFX**. It combines real-time image processing filters (Floyd–Steinberg error diffusion, Sobel convolution edge overlays, and false-color gradients) with an interactive desktop interface featuring a draggable before/after split wipe.

The codebase is split into two cleanly separated layers:
- **`com.rasterglyph.core`**: A headless conversion engine with zero GUI dependencies, suitable for command-line tools or server-side rendering.
- **`com.rasterglyph.ui`**: A hardware-accelerated JavaFX desktop application with real-time viewport controls, zoom and pan, and custom palettes.

---

## Features

### Interactive Before / After Split Wipe
- Side-by-side comparison between the original bitmap and the rendered ASCII output.
- A draggable center handle lets you scrub across the viewport to inspect character alignment and detail retention.
- Uses JavaFX hardware scissor clipping (`Canvas.setClip()`) to slide the split boundary smoothly without re-evaluating or re-rasterizing glyphs during drag.

### Image Processing & Filter Pipeline
- **Floyd–Steinberg Dithering**: 2D error diffusion spreads quantization errors to neighboring pixels, preventing banding across smooth gradients.
- **Sobel Edge Detection**: Applies $3 \times 3$ horizontal and vertical convolution kernels to detect contours, then maps directional angles ($0^\circ, 45^\circ, 90^\circ, 135^\circ$) to character strokes (`-`, `/`, `|`, `\`).
- **Edge Overlay Mode**: Injects structural edge strokes directly over shaded character fills to retain silhouette clarity.
- **Aspect Ratio Correction**: Corrects for standard rectangular monospace font proportions (typically 1:2 width-to-height ratio) so output images do not stretch vertically.

### Color Modes & Palettes
- **Source RGB**: Extracts 24-bit True-Color values per character directly from the source image.
- **Color Gradients**: Multi-stop interpolated palettes including Cyberpunk Neon (cyan to purple), Phosphor Green (CRT style), Amber CRT, Thermal Heatmap, and Monochrome Grayscale.

### Canvas Themes
- **Dark CRT**: Dark slate background with subtle scanlines and neon highlights.
- **White Paper**: Clean white background with dark charcoal text for print and document publishing.
- **Vintage Parchment**: Warm textured paper with sepia ink tones.

### Export Formats
- **PNG**: High-resolution raster image rendered using monospace font metrics.
- **SVG**: Resolution-independent vector document with selectable text and grouped color spans.
- **HTML**: Self-contained HTML file styled with monospace font, responsive layout, and optional CRT scanlines.
- **Plain Text & ANSI**: Direct clipboard copy for plain text or terminal escape sequences with 24-bit color.

---

## Performance Considerations

- **Resize-First Processing**: Source images are downscaled to the target character grid dimensions before running brightness, contrast, and dithering passes. This avoids running convolution loops across millions of unused raw pixels.
- **Direct Raster Array Access**: Pixel manipulation operates directly on underlying 1D `DataBufferInt` arrays instead of using `BufferedImage.getRGB(x, y)` coordinate lookups.
- **Debounced Controls**: Slider parameter changes are debounced (20ms) to avoid queue backlog on the JavaFX application thread during continuous user adjustments.
- **Scissor-Based Wipe**: The before/after split wipe updates rectangular clipping bounds rather than triggering canvas redraws.

---

## Architecture

```
com.rasterglyph
│
├── core/                       # Headless Conversion Engine (Zero GUI Dependencies)
│   ├── CharacterSet.java       # Predefined character ramps (Standard, Extended, Binary, Blocks)
│   ├── CharacterMapper.java    # Luminance-to-glyph mapping and inversion logic
│   ├── AsciiResult.java        # Immutable 2D grid containing characters and RGB values
│   ├── ImageLoader.java        # Image loading with format and boundary checks
│   ├── ImageResizer.java       # Aspect-ratio corrected bilinear downsampler
│   ├── ImageProcessor.java     # Brightness, contrast, and inversion algorithms
│   ├── AsciiConverter.java     # Pipeline coordinator tying together resizing and mapping
│   ├── DitherFilter.java       # Floyd–Steinberg 2D error diffusion implementation
│   ├── SobelEdgeDetector.java  # 3x3 convolution kernels and directional stroke mapping
│   ├── ColorGradient.java      # Multi-stop linear RGB gradient interpolator
│   ├── AnsiRenderer.java       # 24-bit ANSI terminal escape sequence renderer
│   ├── ImageExporter.java      # Monospace font rasterizer to PNG
│   ├── SvgExporter.java        # Vector SVG exporter with color grouping
│   └── HtmlExporter.java       # Self-contained HTML document exporter
│
└── ui/                         # JavaFX Desktop Interface
    ├── Main.java               # Application entry point
    ├── RasterGlyphApp.java     # Primary application layout and drag-and-drop handling
    ├── ControlDeck.java        # Sidebar controls for filters, characters, and palettes
    ├── AsciiViewport.java      # Layered canvas viewport with split-wipe and zoom
    ├── FxImageUtils.java       # BufferedImage to JavaFX Image converter
    └── styles/retro-hud.css    # Dark monospace UI stylesheet
```

---

## Download & Installation

Pre-packaged installers bundle a dedicated runtime—**no separate Java installation is required**:

| Platform | Package | Format | Description |
| :--- | :--- | :--- | :--- |
| **macOS** | [**`RasterGlyph-1.0.0.dmg`**](https://github.com/texatii01/rasterglyph/releases/latest) | Apple Disk Image (`.dmg`) | Drag-and-drop to `/Applications`. Runs on Apple Silicon and Intel. |
| **Windows** | [**`RasterGlyph-1.0.0.msi`**](https://github.com/texatii01/rasterglyph/releases/latest) | Windows Installer (`.msi`) | Standard setup wizard; creates desktop and Start menu shortcuts. |
| **Linux** | [**`rasterglyph_1.0.0_amd64.deb`**](https://github.com/texatii01/rasterglyph/releases/latest) | Debian / Ubuntu Package (`.deb`) | Install via `sudo dpkg -i rasterglyph_1.0.0_amd64.deb`. |
| **Universal** | [**`rasterglyph-1.0.0-all.jar`**](https://github.com/texatii01/rasterglyph/releases/latest) | Executable JAR | Run via `java -jar rasterglyph-1.0.0-all.jar` (Java 21+ required). |

---

## Building from Source

### Prerequisites
- **JDK 21** or later
- **Gradle 8.5+** (or use the included `./gradlew`)

### Running Locally
```bash
git clone https://github.com/texatii01/rasterglyph.git
cd rasterglyph

./gradlew run
```

### Running Tests
Execute the automated test suite (78 unit tests covering image processing, dithering, and exporters):
```bash
./gradlew test
```

---

## Library Usage (Headless)

`com.rasterglyph.core` can be imported and used independently in non-GUI applications:

```java
import com.rasterglyph.core.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

public class Example {
    public static void main(String[] args) throws Exception {
        // 1. Load the input image
        BufferedImage image = ImageLoader.load(new File("photo.jpg"));

        // 2. Configure the converter (extended character set, color enabled, Floyd-Steinberg dithering)
        CharacterMapper mapper = new CharacterMapper(CharacterSet.EXTENDED, false);
        AsciiConverter converter = new AsciiConverter(mapper, ImageResizer.DEFAULT_CHAR_ASPECT_RATIO, true, true);

        // 3. Convert to 120-column ASCII output
        AsciiResult result = converter.convert(image, 120);

        // 4. Print ANSI True-Color output to the terminal
        System.out.println(AnsiRenderer.render(result));

        // 5. Export to SVG and HTML files
        SvgExporter.exportToFile(result, Path.of("output.svg"));
        HtmlExporter.exportToFile(result, Path.of("output.html"));
    }
}
```

---

## Controls & Shortcuts

| Action | Input | Description |
| :--- | :--- | :--- |
| **Zoom In / Out** | `Scroll Wheel` | Zooms toward cursor position ($25\% \dots 600\%$) |
| **Reset Zoom** | `Double-Click` or click `[ ZOOM ]` | Resets zoom factor to $100\%$ and centers image |
| **Pan Viewport** | `Click & Drag Canvas` | Pans the canvas when zoomed in |
| **Before / After Wipe** | `Click & Drag [ ◄ ► ]` | Drags split divider between original image and ASCII |
| **Import Image** | `Drag & Drop` | Drop any PNG, JPG, or BMP file onto the window |

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
