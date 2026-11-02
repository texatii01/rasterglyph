# Contributing to RASTER//GLYPH

Thank you for your interest in contributing to **RASTER//GLYPH**! We welcome bug reports, feature proposals, and code contributions that align with our performance standards and retro-cyberpunk telemetry aesthetic.

---

## Architectural Guidelines

1. **Strict Core / UI Decoupling**:
   - `com.rasterglyph.core`: **Must remain 100% headless** and free of JavaFX dependencies. This package must run and test cleanly in headless CI environments.
   - `com.rasterglyph.ui`: Contains JavaFX controls, viewport rendering, and custom HUD styling.

2. **Performance First**:
   - **Zero Heap Churn in Viewport Loops**: The viewport rendering pipeline must avoid heap object allocations per frame (e.g., use cached string arrays for glyphs, color state deduplication, and direct array buffers).
   - **Direct Raster Access**: When performing pixel operations in `com.rasterglyph.core.ImageProcessor`, prioritize direct `DataBufferInt` 1D array operations over individual `getRGB` / `setRGB` calls.
   - **Hardware Scissor Clipping**: Split wipes and interactive overlays must mutate GPU clipping bounds rather than re-evaluating or re-rasterizing the full text canvas.

3. **Aspect Ratio Preservation**:
   - Terminal font characters are roughly twice as tall as wide (~1:2 ratio). Always ensure aspect ratio compensation (default `charAspectRatio = 0.5`) is preserved across resizing and edge detection routines.

---

## Development Workflow

### Prerequisites
- **JDK 21** or later
- **Gradle 8.5+** (or use the included `./gradlew` wrapper)

### Running Tests
All pull requests must pass the existing automated test suite:
```bash
./gradlew test
```

### Running the Application
Launch the tactical desktop HUD locally:
```bash
./gradlew run
```
Or run the standalone launcher class:
```bash
com.rasterglyph.Main
```

---

## AI Usage & Contributions

This project was built with the assistance of AI development tools. AI-assisted contributions and tooling are welcomed, provided that all generated code adheres strictly to the architectural constraints (e.g. keeping `core` headless), maintains 0-heap allocation performance in viewport loops, and passes all unit tests with full human review.

---

## Pull Request Guidelines

1. Fork the repository and create your feature branch from `main`:
   ```bash
   git checkout -b feature/tactical-filter
   ```
2. Write unit tests for new core functionality in `src/test/java/com/rasterglyph/core/`.
3. Verify that `./gradlew test` passes with 0 failures.
4. Keep commit messages clear, concise, and descriptive:
   - `feat: add matrix green rain animation`
   - `perf: optimize sobel convolution kernel`
   - `fix: prevent clipping artifact on ultra-wide images`
5. Submit a Pull Request targeting `main`.
