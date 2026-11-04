package com.rasterglyph.ui;

import com.rasterglyph.core.AsciiResult;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ViewportZoomTest {

    @BeforeAll
    public static void initJFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testZoomInAndOutRepeatedly() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        boolean[] success = new boolean[1];

        Platform.runLater(() -> {
            try {
                AsciiViewport viewport = new AsciiViewport();
                char[][] chars = new char[100][220]; // 220 columns high detail
                int[][] rgb = new int[100][220];
                for (int y = 0; y < 100; y++) {
                    for (int x = 0; x < 220; x++) {
                        chars[y][x] = '#';
                        rgb[y][x] = 0xFF00FFE6;
                    }
                }
                AsciiResult result = new AsciiResult(chars, rgb);
                viewport.displayResult(result, 12.0);

                Stage stage = new Stage();
                stage.setScene(new Scene(viewport, 800, 600));
                stage.show();

                // Test rapid zooming in and out
                for (int cycle = 0; cycle < 5; cycle++) {
                    for (int i = 0; i < 20; i++) {
                        viewport.zoomAt(400, 300, 0.15);
                        assertValidScrollValues(viewport);
                    }
                    for (int i = 0; i < 25; i++) {
                        viewport.zoomAt(400, 300, -0.15);
                        assertValidScrollValues(viewport);
                    }
                }

                // Test split-view zoom interaction
                viewport.setSplitViewEnabled(true);
                for (int i = 0; i < 15; i++) {
                    viewport.zoomAt(350, 250, 0.12);
                    assertValidScrollValues(viewport);
                }
                for (int i = 0; i < 20; i++) {
                    viewport.zoomAt(350, 250, -0.12);
                    assertValidScrollValues(viewport);
                }

                // Test zoom reset
                viewport.resetZoom();
                assertValidScrollValues(viewport);
                assertTrue(Math.abs(viewport.getZoomFactor() - 1.0) < 0.001);

                // Test extreme off-canvas cursor coordinates
                for (double x : new double[]{-500, 0, 400, 800, 2500}) {
                    for (double y : new double[]{-500, 0, 300, 600, 2500}) {
                        viewport.zoomAt(x, y, 0.15);
                        viewport.zoomAt(x, y, -0.15);
                        assertValidScrollValues(viewport);
                    }
                }

                stage.close();
                success[0] = true;
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertTrue(success[0]);
    }

    private void assertValidScrollValues(AsciiViewport vp) {
        javafx.scene.control.ScrollPane sp = (javafx.scene.control.ScrollPane) vp.getChildren().get(0);
        double hval = sp.getHvalue();
        double vval = sp.getVvalue();

        assertFalse(Double.isNaN(hval), "Hvalue must never be NaN");
        assertFalse(Double.isInfinite(hval), "Hvalue must never be Infinite");
        assertTrue(hval >= 0.0 && hval <= 1.0, "Hvalue must be within [0.0, 1.0]: " + hval);

        assertFalse(Double.isNaN(vval), "Vvalue must never be NaN");
        assertFalse(Double.isInfinite(vval), "Vvalue must never be Infinite");
        assertTrue(vval >= 0.0 && vval <= 1.0, "Vvalue must be within [0.0, 1.0]: " + vval);
    }
}
