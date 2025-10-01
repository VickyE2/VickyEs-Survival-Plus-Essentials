package org.vicky.vspe.viewer;

// File: ChunkHeightViewer.java
// Java 17+
// Requires JavaFX on the classpath/module-path.

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Basic JavaFX 3D viewer for int[] heights (chunkWidth x chunkWidth).
 * Plug a HeightProvider (chunkX, chunkZ) -> int[] and press "Regenerate".
 */
public class ChunkHeightViewer extends Application {
    // Set this before launching, or replace in start() with a real provider.
    public static HeightProvider PROVIDER = createDemoProvider();
    private final Group worldGroup = new Group();

    // default chunk size (16)
    private final MeshView meshView = new MeshView();
    private final SubScene subScene;
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private double cameraDistance = 80;
    private final Rotate rotateX = new Rotate(-30, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-30, Rotate.Y_AXIS);
    // Debounce: wait this long after last change before regenerating
    private final PauseTransition refreshDelay = new PauseTransition(Duration.millis(200));
    // background executor for generation work
    private final ExecutorService genExecutor = Executors.newSingleThreadExecutor();
    private final Slider scaleSlider = new Slider(0.0, 200.0, 10.0);
    private final Spinner<Integer> chunkXSpinner = new Spinner<>(-1000, 1000, 0);
    private final Spinner<Integer> chunkZSpinner = new Spinner<>(-1000, 1000, 0);
    private final Spinner<Integer> upsampleSpinner = new Spinner<>(1, 8, 1);
    private final Spinner<Integer> gridSizeSpinner = new Spinner<>(8, 256, 16, 8);
    private final Spinner<Integer> regionChunksXSpinner = new Spinner<>(1, 64, 1, 1);
    private final Spinner<Integer> regionChunksZSpinner = new Spinner<>(1, 64, 1, 1);
    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    // optional: a flag so we don't schedule two concurrent builds
    private volatile boolean building = false;
    private final int lastChunkX = 0;
    private final int lastChunkZ = 0;
    public ChunkHeightViewer() {
        // create subscene with worldGroup and camera
        subScene = new SubScene(worldGroup, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#85c1e9"));
    }

    /**
     * Calls provider for each chunk in the rectangular region [startChunkX..startChunkX+chunksX-1]
     * x [startChunkZ..startChunkZ+chunksZ-1] and stitches them into one big int[] of size
     * (chunksZ * chunkSize) * (chunksX * chunkSize) in row-major (z*width + x).
     * <p>
     * Provider signature: int[] getChunkHeights(int chunkX, int chunkZ, int requestedChunkSize)
     */
    private static int[] stitchChunks(HeightProvider provider,
                                      int startChunkX, int startChunkZ,
                                      int chunksX, int chunksZ,
                                      int requestedChunkSize) {

        // query one chunk to determine canonical per-chunk size
        int[] sample = provider.getChunkHeights(startChunkX, startChunkZ, requestedChunkSize);
        if (sample == null) return null;

        int baseChunkSize = (int) Math.round(Math.sqrt(sample.length));
        if (baseChunkSize * baseChunkSize != sample.length) {
            throw new IllegalStateException("Provider returned non-square chunk size: length=" + sample.length);
        }

        final int outW = chunksX * baseChunkSize;
        final int outH = chunksZ * baseChunkSize;
        final int[] out = new int[outW * outH];

        for (int rz = 0; rz < chunksZ; rz++) {
            for (int rx = 0; rx < chunksX; rx++) {
                int cx = startChunkX + rx;
                int cz = startChunkZ + rz;
                int[] chunk = provider.getChunkHeights(cx, cz, requestedChunkSize);

                if (chunk == null) {
                    // fill the whole chunk rectangle with zeros (all rows)
                    for (int row = 0; row < baseChunkSize; row++) {
                        int dstRowStart = (rz * baseChunkSize + row) * outW + (rx * baseChunkSize);
                        Arrays.fill(out, dstRowStart, dstRowStart + baseChunkSize, 0);
                    }
                    continue;
                }

                int chunkSize = (int) Math.round(Math.sqrt(chunk.length));
                if (chunkSize * chunkSize != chunk.length) {
                    // if provider is broken, attempt best-effort: treat as linear row and copy as much as possible
                    chunkSize = (int) Math.round(Math.sqrt(chunk.length)); // keep it anyway
                }

                // if chunk size differs from baseChunkSize, resize it first
                if (chunkSize != baseChunkSize) {
                    chunk = resizeChunkArray(chunk, chunkSize, baseChunkSize);
                    chunkSize = baseChunkSize;
                }

                // copy rows into the destination mosaic
                for (int z = 0; z < chunkSize; z++) {
                    int srcRow = z * chunkSize;
                    int dstRow = (rz * baseChunkSize + z) * outW + (rx * baseChunkSize);
                    System.arraycopy(chunk, srcRow, out, dstRow, chunkSize);
                }
            }
        }
        return out;
    }

    private static int[] resizeChunkArray(int[] src, int oldSize, int newSize) {
        if (oldSize == newSize) return src;
        int[] out = new int[newSize * newSize];

        // If oldSize==1, just duplicate the single value
        if (oldSize == 1) {
            Arrays.fill(out, src[0]);
            return out;
        }

        for (int z = 0; z < newSize; z++) {
            // map target pixel center to source coordinate in [0, oldSize-1]
            double gz = (z / (double) (newSize - 1)) * (oldSize - 1);
            int iz = (int) Math.floor(gz);
            double tz = gz - iz;
            iz = Math.min(iz, oldSize - 2);

            for (int x = 0; x < newSize; x++) {
                double gx = (x / (double) (newSize - 1)) * (oldSize - 1);
                int ix = (int) Math.floor(gx);
                double tx = gx - ix;
                ix = Math.min(ix, oldSize - 2);

                // fetch four neighbors
                int a = src[iz * oldSize + ix];
                int b = src[iz * oldSize + ix + 1];
                int c = src[(iz + 1) * oldSize + ix];
                int d = src[(iz + 1) * oldSize + ix + 1];

                // bilerp each color as integer heights (treat as numbers)
                double v = bilerp(a, b, c, d, tx, tz);
                out[z * newSize + x] = (int) Math.round(v);
            }
        }
        return out;
    }



    // Upsample using bilinear interpolation to size * upsample
    private static int[] upsampleHeights(int[] base, int size, int upsample) {
        int newSize = size * upsample;
        int[] out = new int[newSize * newSize];
        for (int z = 0; z < newSize; z++) {
            double gz = (z / (double) (newSize - 1)) * (size - 1);
            int iz = (int) Math.floor(gz);
            double tz = gz - iz;
            iz = Math.min(iz, size - 2);
            for (int x = 0; x < newSize; x++) {
                double gx = (x / (double) (newSize - 1)) * (size - 1);
                int ix = (int) Math.floor(gx);
                double tx = gx - ix;
                ix = Math.min(ix, size - 2);

                int a = base[iz * size + ix];
                int b = base[iz * size + (ix + 1)];
                int c = base[(iz + 1) * size + ix];
                int d = base[(iz + 1) * size + (ix + 1)];
                double v = bilerp(a, b, c, d, tx, tz);
                out[z * newSize + x] = (int) Math.round(v);
            }
        }
        return out;
    }

    private static double bilerp(double a, double b, double c, double d, double tx, double ty) {
        double ab = a + (b - a) * tx;
        double cd = c + (d - c) * tx;
        return ab + (cd - ab) * ty;
    }

    /**
     * Build a WritableImage colored by height (terrain palette).
     */
    private static WritableImage buildHeightmapImage(int[] heights, int w, int h) {
        int min = Arrays.stream(heights).min().orElse(0);
        int max = Arrays.stream(heights).max().orElse(min + 1);
        int range = Math.max(1, max - min);
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                int v = heights[z * w + x];
                double t = (v - min) / (double) range;
                int rgb = terrainColor(t);
                bi.setRGB(x, z, rgb);
            }
        }
        return SwingFXUtils.toFXImage(bi, null);
    }

    /**
     * A simple terrain gradient (sea->sand->grass->rock->snow) -> ARGB int
     */
    private static int terrainColor(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int r, g, b;
        if (t < 0.2) { // deep -> shallow
            // blue water
            r = (int) (20 + t * 50);
            g = (int) (40 + t * 60);
            b = (int) (120 + t * 100);
        } else if (t < 0.28) {
            // sand/beach
            r = (int) (210 + (t - 0.2) * 100);
            g = (int) (190 + (t - 0.2) * 80);
            b = (int) (140 + (t - 0.2) * 40);
        } else if (t < 0.7) {
            // grass
            r = (int) (40 + (t - 0.28) * 120);
            g = (int) (160 + (t - 0.28) * 80);
            b = (int) (40 + (t - 0.28) * 40);
        } else if (t < 0.9) {
            // rock
            r = (int) (120 + (t - 0.7) * 70);
            g = (int) (120 + (t - 0.7) * 70);
            b = (int) (120 + (t - 0.7) * 70);
        } else {
            // snow
            r = g = b = (int) (220 + (t - 0.9) * 35);
        }
        r = clamp(r, 0, 255);
        g = clamp(g, 0, 255);
        b = clamp(b, 0, 255);
        return (255 << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v, int a, int b) {
        return v < a ? a : v > b ? b : v;
    }

    /**
     * Build mesh and assign to MeshView. Each vertex has matching texcoord.
     */
    private static void buildMeshFromHeights(MeshView mv, int[] heights, int size, float verticalScale) {
        TriangleMesh mesh = new TriangleMesh();
        // points: for each (x,z) -> (x, height * scale, z)
        float[] points = new float[size * size * 3];
        float[] texCoords = new float[size * size * 2];

        int min = Arrays.stream(heights).min().orElse(0);
        int max = Arrays.stream(heights).max().orElse(min + 1);
        int range = Math.max(1, max - min);

        int pIdx = 0, tIdx = 0;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int v = heights[z * size + x];
                float fy = (v - min) / (float) range * verticalScale;
                points[pIdx++] = (float) x;
                points[pIdx++] = fy;
                points[pIdx++] = (float) z;

                // uv
                texCoords[tIdx++] = x / (float) (size - 1);
                texCoords[tIdx++] = z / (float) (size - 1);
            }
        }
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);

        // faces (two triangles per cell)
        int[] faces = new int[(size - 1) * (size - 1) * 6 * 2]; // 6 int entries per triangle (p,t), 2 triangles
        int f = 0;
        for (int z = 0; z < size - 1; z++) {
            for (int x = 0; x < size - 1; x++) {
                int v00 = z * size + x;
                int v10 = z * size + (x + 1);
                int v01 = (z + 1) * size + x;
                int v11 = (z + 1) * size + (x + 1);

                // triangle 1: v00, v01, v11
                faces[f++] = v00;
                faces[f++] = v00;
                faces[f++] = v01;
                faces[f++] = v01;
                faces[f++] = v11;
                faces[f++] = v11;

                // triangle 2: v00, v11, v10
                faces[f++] = v00;
                faces[f++] = v00;
                faces[f++] = v11;
                faces[f++] = v11;
                faces[f++] = v10;
                faces[f++] = v10;
            }
        }
        mesh.getFaces().setAll(faces);
        mesh.getFaceSmoothingGroups().setAll(new int[(size - 1) * (size - 1) * 2]); // default smoothing groups (zeros ok)

        mv.setMesh(mesh);
    }

    private static HeightProvider createDemoProvider() {
        return (cx, cz, size) -> {
            // create gentle hills + small FBM-style randomness
            int[] out = new int[size * size];
            Random r = new Random(cx * 73428767L ^ cz * 912783L);
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    double wx = (cx * size + x) * 0.03;
                    double wz = (cz * size + z) * 0.03;
                    // base plateau ~ 73, small bumps, some larger bumps
                    double base = 73.0;
                    double bump = (simpleNoise(wx, wz) * 6.0); // small bumps
                    double big = (simpleNoise(wx * 0.2, wz * 0.2) * 30.0); // occasional hills
                    double val = base + bump + big;
                    out[z * size + x] = (int) Math.round(val);
                }
            }
            return out;
        };
    }

    // create BufferedImage off-thread (similar to buildHeightmapImage but returns BufferedImage)
    private static BufferedImage buildHeightmapBufferedImage(int[] heights, int w, int h) {
        int min = Arrays.stream(heights).min().orElse(0);
        int max = Arrays.stream(heights).max().orElse(min + 1);
        int range = Math.max(1, max - min);
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                int v = heights[z * w + x];
                double t = (v - min) / (double) range;
                bi.setRGB(x, z, terrainColor(t));
            }
        }
        return bi;
    }

    // crater provider
    private static HeightProvider createCraterProvider() {
        return (cx, cz, size) -> {
            int[] out = new int[size * size];
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    double wx = x + cx * size;
                    double wz = z + cz * size;
                    double cx0 = size / 2.0, cz0 = size / 2.0;
                    double d = Math.hypot(wx - cx0, wz - cz0);
                    double base = 120;
                    double crater = Math.max(0, 60 - d * 10) * Math.exp(-d * 0.2);
                    out[z * size + x] = (int) Math.round(base - crater);
                }
            }
            return out;
        };
    }

    // simple pseudo-noise: fast and deterministic
    private static double simpleNoise(double x, double y) {
        long n = Double.doubleToLongBits(x * 374761393 + y * 668265263);
        n = (n ^ (n >> 13)) * 1274126177;
        double r = (double) ((n & 0x7fffffff) % 10000) / 10000.0;
        return r * 2.0 - 1.0;
    }

    // ---- main ----
    public static void main(String[] args) {
        // Example: if you have a ChunkHeightProvider instance called 'provider',
        // you can set:
        // ChunkHeightViewer.PROVIDER = (cx, cz) -> provider.getChunkHeights(cx, cz);
        // Then launch:
        launch(args);
    }

    private void setControlsDisabled(boolean disabled) {
        chunkXSpinner.setDisable(disabled);
        chunkZSpinner.setDisable(disabled);
        upsampleSpinner.setDisable(disabled);
        scaleSlider.setDisable(disabled);
        gridSizeSpinner.setDisable(disabled);
        regionChunksXSpinner.setDisable(disabled);
        regionChunksZSpinner.setDisable(disabled);
    }

    // call this after controls are constructed
    private void installAutoRefresh() {
        // rebuild immediately on double-click / regenerate button (already set)
        // rebuild after a short pause when any control changes
        // add all valueProperty listeners
        chunkXSpinner.valueProperty().addListener((obs, oldV, newV) -> refreshDelay.playFromStart());
        chunkZSpinner.valueProperty().addListener((obs, oldV, newV) -> refreshDelay.playFromStart());
        upsampleSpinner.valueProperty().addListener((obs, oldV, newV) -> refreshDelay.playFromStart());
        gridSizeSpinner.valueProperty().addListener((obs, oldV, newV) -> refreshDelay.playFromStart());
        scaleSlider.valueProperty().addListener((obs, oldV, newV) -> refreshDelay.playFromStart());

        // if you added region spinners:
        regionChunksXSpinner.valueProperty().addListener((obs, oldV, newV) -> refreshDelay.playFromStart());
        regionChunksZSpinner.valueProperty().addListener((obs, oldV, newV) -> refreshDelay.playFromStart());

        // When pause finishes, run async regenerate
        refreshDelay.setOnFinished(evt -> regenerateAndBuildAsync());
    }

    // ---- Demo providers so you can run the viewer without hooking NMS code ----

    @Override
    public void start(Stage primaryStage) {
        // basic camera setup
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        updateCameraPosition();

        subScene.setCamera(camera);
        installAutoRefresh();

        // add a light
        PointLight light = new PointLight(Color.WHITE);
        light.setTranslateX(100);
        light.setTranslateY(-200);
        light.setTranslateZ(-300);
        worldGroup.getChildren().add(light);
        worldGroup.getChildren().add(new AmbientLight(Color.color(0.35, 0.35, 0.35)));

        meshView.setCullFace(CullFace.NONE);
        meshView.getTransforms().addAll(rotateX, rotateY);

        worldGroup.getChildren().add(meshView);

        BorderPane root = new BorderPane();
        root.setCenter(subScene);

        // right-side UI
        VBox controls = new VBox(8);
        controls.setPadding(new Insets(8));
        controls.setPrefWidth(300);

        Label title = new Label("Chunk Height Viewer");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        controls.getChildren().add(title);

        HBox chunkBox = new HBox(6, new Label("chunkX:"), chunkXSpinner, new Label("chunkZ:"), chunkZSpinner);
        controls.getChildren().add(chunkBox);

        HBox ups = new HBox(6, new Label("Upsample:"), upsampleSpinner);
        controls.getChildren().add(ups);

        HBox grid = new HBox(6, new Label("GridSize:"), gridSizeSpinner);
        controls.getChildren().add(grid);

        HBox regionBox = new HBox(6, new Label("Region chunks X:"), regionChunksXSpinner,
                new Label("Z:"), regionChunksZSpinner);
        controls.getChildren().add(regionBox);

        scaleSlider.setShowTickMarks(true);
        scaleSlider.setShowTickLabels(true);
        scaleSlider.setMajorTickUnit(50);
        scaleSlider.setBlockIncrement(1);
        controls.getChildren().add(new Label("Vertical scale:"));
        controls.getChildren().add(scaleSlider);

        Button regen = new Button("Regenerate");
        regen.setOnAction(e -> regenerateAndBuild());
        controls.getChildren().add(regen);

        // quick random/noise demo buttons
        Button demoBtn = new Button("Demo: random hills");
        demoBtn.setOnAction(e -> {
            PROVIDER = createDemoProvider();
            regenerateAndBuild();
        });
        controls.getChildren().add(demoBtn);

        Button circleBtn = new Button("Demo: crater");
        circleBtn.setOnAction(e -> {
            PROVIDER = createCraterProvider();
            regenerateAndBuild();
        });
        controls.getChildren().add(circleBtn);

        // status area
        TextArea status = new TextArea();
        status.setPrefRowCount(8);
        status.setEditable(false);
        controls.getChildren().add(new Label("Status / info:"));
        controls.getChildren().add(status);

        // build layout
        root.setRight(controls);

        Scene scene = new Scene(root, 1100, 700, true);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Chunk Height Viewer");

        // wire mouse controls for rotate/zoom/pan on subScene
        installMouseControls(subScene);

        // initial mesh
        regenerateAndBuild();

        primaryStage.show();
    }

    private void updateCameraPosition() {
        camera.setTranslateZ(-cameraDistance);
        camera.setTranslateY(-20);
        camera.setFieldOfView(50);
    }

    private void installMouseControls(SubScene s) {
        s.setOnScroll(ev -> {
            double delta = ev.getDeltaY();
            cameraDistance -= delta * 0.2;
            cameraDistance = Math.max(10, Math.min(2000, cameraDistance));
            updateCameraPosition();
        });

        s.setOnMousePressed(ev -> {
            anchorX = ev.getSceneX();
            anchorY = ev.getSceneY();
            anchorAngleX = rotateX.getAngle();
            anchorAngleY = rotateY.getAngle();
        });

        s.setOnMouseDragged(ev -> {
            double dx = ev.getSceneX() - anchorX;
            double dy = ev.getSceneY() - anchorY;
            if (ev.getButton() == MouseButton.PRIMARY) {
                rotateY.setAngle(anchorAngleY + dx * 0.5);
                rotateX.setAngle(anchorAngleX - dy * 0.5);
            } else if (ev.getButton() == MouseButton.SECONDARY) {
                // pan: translate the worldGroup
                worldGroup.translateXProperty().set(worldGroup.getTranslateX() + dx * 0.2);
                worldGroup.translateYProperty().set(worldGroup.getTranslateY() + dy * 0.2);
                anchorX = ev.getSceneX();
                anchorY = ev.getSceneY();
            }
        });

        // double-click to regenerate
        s.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) regenerateAndBuild();
        });
    }

    /**
     * Regenerate using provider and rebuild mesh + texture.
     */
    private void regenerateAndBuild() {
        int startChunkX = chunkXSpinner.getValue();
        int startChunkZ = chunkZSpinner.getValue();
        int upsample = upsampleSpinner.getValue();
        double vscale = scaleSlider.getValue();

        int requestChunkSize = gridSizeSpinner.getValue();   // e.g. 16
        int regionChunksX = regionChunksXSpinner.getValue();
        int regionChunksZ = regionChunksZSpinner.getValue();

        // stitch region of chunks
        int[] heightsBase = stitchChunks(PROVIDER, startChunkX, startChunkZ, regionChunksX, regionChunksZ, requestChunkSize);
        if (heightsBase == null) {
            System.err.println("Provider returned null heights for region");
            return;
        }

        int baseSize = (int) Math.round(Math.sqrt(heightsBase.length)); // total width/height of the mosaic
        // optionally additional upsampling
        int[] heights = upsample > 1 ? upsampleHeights(heightsBase, baseSize, upsample) : heightsBase;
        int finalSize = baseSize * (upsample > 1 ? upsample : 1);

        buildMeshFromHeights(meshView, heights, finalSize, (float) vscale);

        WritableImage img = buildHeightmapImage(heights, finalSize, finalSize);
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseMap(img);
        mat.setSpecularColor(Color.rgb(40, 40, 40));
        meshView.setMaterial(mat);

        meshView.setTranslateX(-finalSize / 2.0);
        meshView.setTranslateZ(-finalSize / 2.0);
    }
    private void regenerateAndBuildAsync() {

        // Avoid concurrent builds
        if (building) return;
        building = true;
        setControlsDisabled(true);

        // capture UI state (do this on FX thread)
        final int startChunkX = chunkXSpinner.getValue();
        final int startChunkZ = chunkZSpinner.getValue();
        final int upsample = upsampleSpinner.getValue();
        final double vscale = scaleSlider.getValue();
        final int requestChunkSize = gridSizeSpinner.getValue();
        // if you added region spinners:
        final int regionChunksX = (regionChunksXSpinner != null) ? regionChunksXSpinner.getValue() : 1;
        final int regionChunksZ = (regionChunksZSpinner != null) ? regionChunksZSpinner.getValue() : 1;

        // Offload generation to background executor
        CompletableFuture.supplyAsync(() -> {

            // 1) stitch / call provider to get big heightmap
            int[] heightsBase = stitchChunks(PROVIDER, startChunkX, startChunkZ, regionChunksX, regionChunksZ, requestChunkSize);
            if (heightsBase == null) return null;

            // 2) upsample if needed
            int baseSize = (int) Math.round(Math.sqrt(heightsBase.length));
            if (baseSize * baseSize != heightsBase.length) {
                System.err.println("ERROR: stitched region produced non-square result: len=" + heightsBase.length);
                return null;
            }
            int[] heights = upsample > 1 ? upsampleHeights(heightsBase, baseSize, upsample) : heightsBase;
            int finalSize = baseSize * (Math.max(upsample, 1));

            // 3) generate an image snapshot (BufferedImage) for the texture.
            //    building a BufferedImage can be done off-thread
            BufferedImage bi = buildHeightmapBufferedImage(heights, finalSize, finalSize);

            // return a small result object
            return new GenResult(heights, finalSize, vscale, bi);
        }, genExecutor).whenComplete((res, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    setControlsDisabled(false);
                    building = false;
                });
                return;
            }
            if (res == null) {
                Platform.runLater(() -> {
                    setControlsDisabled(false);
                    building = false;
                });
                return;
            }

            // Back on FX thread: apply mesh and texture
            Platform.runLater(() -> {
                try {
                    buildMeshFromHeights(meshView, res.heights, res.size, (float) res.vscale);

                    // convert BufferedImage -> WritableImage & set as material
                    WritableImage img = SwingFXUtils.toFXImage(res.image, null);
                    PhongMaterial mat = new PhongMaterial();
                    mat.setDiffuseMap(img);
                    mat.setSpecularColor(Color.rgb(40, 40, 40));
                    meshView.setMaterial(mat);

                    meshView.setTranslateX(-res.size / 2.0);
                    meshView.setTranslateZ(-res.size / 2.0);
                } finally {
                    setControlsDisabled(false);
                    building = false;
                }
            });
        });
    }

    // Interface for plugging your provider
    @FunctionalInterface
    public interface HeightProvider {
        // returns int[width*width], row-major (z * width + x)
        int[] getChunkHeights(int chunkX, int chunkZ, int size);
    }

    // small holder
    private static class GenResult {
        final int[] heights;
        final int size;
        final double vscale;
        final BufferedImage image;

        GenResult(int[] heights, int size, double vscale, BufferedImage image) {
            this.heights = heights;
            this.size = size;
            this.vscale = vscale;
            this.image = image;
        }
    }
}

