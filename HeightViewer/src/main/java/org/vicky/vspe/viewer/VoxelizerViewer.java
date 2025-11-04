package org.vicky.vspe.viewer;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VoxelizerViewer extends Application {

    private final Group structureGroup = new Group();
    public static ResolvedStructure<Object> SAMPLE = createSampleStructure();
    private final Set<KeyCode> keysDown = new HashSet<>();
    private final Group root3D = new Group();
    private final Group worldGroup = new Group();
    private final Group cameraGroup = new Group();
    private final MeshView meshView = new MeshView();
    private final SubScene subScene;
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    // ---------------------------------------------------
    private final Translate cameraPosition = new Translate(0, -20, -200);
    private final Rotate cameraYaw = new Rotate(0, Rotate.Y_AXIS);    // horizontal
    private final Rotate cameraPitch = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateX = new Rotate(-30, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-30, Rotate.Y_AXIS);
    private double lastMouseX, lastMouseY;
    private boolean isMousePressed = false;
    private final javafx.scene.control.Label cameraCoordsLabel = new javafx.scene.control.Label();

    public VoxelizerViewer() {
        subScene = new SubScene(worldGroup, 900, 800, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.LIGHTGRAY);
    }

    public static StructureBox computeBounds(ResolvedStructure<?> s) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (List<? extends BlockPlacement<?>> list : s.placementsByChunk.values()) {
            for (BlockPlacement<?> p : list) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                minZ = Math.min(minZ, p.z);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
                maxZ = Math.max(maxZ, p.z);
            }
        }
        if (minX == Integer.MAX_VALUE) return new StructureBox(0, 0, 0, 0, 0, 0);
        return new StructureBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // ----- Sample structure -----
    private static ResolvedStructure<Object> createSampleStructure() {
        ConcurrentHashMap<ChunkCoord, List<BlockPlacement<Object>>> map = new ConcurrentHashMap<>();
        List<BlockPlacement<Object>> list = new ArrayList<>();

        for (int y = 0; y <= 6; y++) {
            int r = 6 - y;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) + Math.abs(z) <= r) {
                        list.add(new BlockPlacement<>(x, y, z, null));
                    }
                }
            }
        }
        map.put(new ChunkCoord(0, 0), list);
        StructureBox bounds = computeBounds(new ResolvedStructure<>(map, null));
        return new ResolvedStructure<>(map, bounds);
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        BorderPane rootPane = new BorderPane();
        // basic camera setup
        camera.setNearClip(0.1);
        camera.setFarClip(1000000000000000000000.0);
        camera.getTransforms().addAll(cameraYaw, cameraPitch, cameraPosition);
        updateCameraPosition();

        subScene.setCamera(camera);
        subScene.setFocusTraversable(true);
        subScene.requestFocus();
        installMouseControls(subScene);

        root3D.getChildren().add(cameraGroup);

        PointLight light = new PointLight(Color.WHITE);
        light.setTranslateX(100);
        light.setTranslateY(200);
        light.setTranslateZ(-300);
        worldGroup.getChildren().add(light);
        worldGroup.getChildren().add(new AmbientLight(Color.color(0.35, 0.35, 0.35)));

        meshView.setCullFace(CullFace.NONE);
        meshView.getTransforms().addAll(rotateX, rotateY);

        worldGroup.getChildren().add(meshView);

        root3D.getChildren().add(worldGroup);
        renderResolvedStructure(SAMPLE);

        rootPane.setCenter(subScene);
        StackPane overlay = new StackPane(cameraCoordsLabel);
        overlay.setMouseTransparent(true); // let mouse go through to 3D scene
        StackPane.setAlignment(cameraCoordsLabel, javafx.geometry.Pos.TOP_RIGHT);

        BorderPane wrapper = new BorderPane(overlay);
        overlay.setMouseTransparent(true);
        wrapper.setMouseTransparent(true);
        rootPane.setCenter(new StackPane(subScene, wrapper));

        Scene scene = new Scene(rootPane, 900, 800, true);
        stage.setScene(scene);
        Group axes = createAxes(100);
        Group grid = createGrid(5000, 10);
        grid.setTranslateY(0.1);
        axes.setTranslateY(0.1);
        grid.setDepthTest(DepthTest.ENABLE);
        axes.setDepthTest(DepthTest.ENABLE);

        // worldGroup.getChildren().addAll(grid, axes);
        worldGroup.getChildren().add(structureGroup);


        stage.setTitle("Structure Viewer (JavaFX)");
        stage.show();
    }

    private double clamp(double v, double min, double max) {
        if (v < min) return min;
        return Math.min(v, max);
    }

    private void renderResolvedStructure(ResolvedStructure<?> structure) {
        Platform.runLater(() -> {
            structureGroup.getChildren().clear();

            StructureBox bounds = structure.bounds != null
                    ? structure.bounds : computeBounds(structure);

            double blockSize = 12.0;
            double centerX = (bounds.minX + bounds.maxX + 1) / 2.0;
            double centerY = (bounds.minY + bounds.maxY + 1) / 2.0;
            double centerZ = (bounds.minZ + bounds.maxZ + 1) / 2.0;

            // Track already rendered coordinates to avoid duplicates
            HashSet<String> seenPositions = new HashSet<>();

            for (var entry : structure.placementsByChunk.entrySet()) {
                for (BlockPlacement<?> p : entry.getValue()) {
                    String key = p.x + ":" + p.y + ":" + p.z;
                    if (seenPositions.add(key)) { // only render if not seen yet
                        PhongMaterial mat = new PhongMaterial(Color.LIGHTGRAY);
                        mat.setDiffuseColor(ColorUtil.hexToRgb(p.state.toString()));
                        mat.setSpecularColor(Color.BLACK);
                        Box b = new Box(blockSize, blockSize, blockSize);
                        b.setCullFace(CullFace.BACK);
                        b.setDrawMode(DrawMode.FILL);
                        b.setMaterial(mat);

                        double cx = (p.x + 0.5) - centerX;
                        double cy = (p.y + 0.5) - centerY;
                        double cz = (p.z + 0.5) - centerZ;

                        b.setTranslateX(cx * blockSize);
                        b.setTranslateY(-cy * blockSize);
                        b.setTranslateZ(cz * blockSize);

                        structureGroup.getChildren().add(b);
                    }
                }
            }

            // adjust camera distance
            int sizeX = bounds.maxX - bounds.minX + 1;
            int sizeY = bounds.maxY - bounds.minY + 1;
            int sizeZ = bounds.maxZ - bounds.minZ + 1;
            int maxDim = Math.max(sizeX, Math.max(sizeY, sizeZ));
            double camDist = Math.max(200.0, maxDim * blockSize * 1.5);
            cameraPosition.setZ(-camDist);
        });
    }

    private void updateCameraPosition() {
        camera.setTranslateZ(-800);
        camera.setTranslateY(-20);
        camera.setFieldOfView(50);
    }

    private void installMouseControls(SubScene s) {
        s.setOnKeyPressed(e -> keysDown.add(e.getCode()));
        s.setOnKeyReleased(e -> keysDown.remove(e.getCode()));
        s.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                lastMouseX = e.getSceneX();
                lastMouseY = e.getSceneY();
                isMousePressed = true;
            }
        });
        s.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                isMousePressed = false;
            }
        });
        s.setOnMouseDragged(e -> {
            if (!isMousePressed) return;

            double dx = e.getSceneX() - lastMouseX;
            double dy = e.getSceneY() - lastMouseY;
            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();

            double sensitivity = 0.07; // tweak for faster/slower turning

            cameraYaw.setAngle(cameraYaw.getAngle() - dx * sensitivity * -1); // yaw
            cameraPitch.setAngle(clamp(cameraPitch.getAngle() - dy * sensitivity, -90, 90)); // pitch
        });


        AnimationTimer movementTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                rotateY.setAngle((rotateY.getAngle() + 0.2) % 360);
                double speed = 2.2; // units per frame
                Platform.runLater(() -> {
                    double x = cameraPosition.getX();
                    double y = cameraPosition.getY();
                    double z = cameraPosition.getZ();

                    String text = String.format(
                            "X: %.1f   Y: %.1f   Z: %.1f",
                            x, y, z
                    );
                });


                // Convert angles to radians
                double yawRad = Math.toRadians(cameraYaw.getAngle());
                double pitchRad = Math.toRadians(cameraPitch.getAngle());

                // Forward vector
                double fx = -Math.sin(yawRad) * Math.cos(pitchRad);
                double fy = Math.sin(pitchRad);
                double fz = -Math.cos(yawRad) * Math.cos(pitchRad);

                // Right vector
                double rx = Math.cos(yawRad);
                double rz = -Math.sin(yawRad);

                double dx = 0, dy = 0, dz = 0;

                if (keysDown.contains(KeyCode.CONTROL)) {
                    speed *= 2;
                }
                if (keysDown.contains(KeyCode.S)) {
                    dx += fx * speed;
                    dy += fy * speed;
                    dz += fz * speed;
                }
                if (keysDown.contains(KeyCode.W)) {
                    dx -= fx * speed;
                    dy -= fy * speed;
                    dz -= fz * speed;
                }
                if (keysDown.contains(KeyCode.A)) {
                    dx -= rx * speed;
                    dz -= rz * speed;
                }
                if (keysDown.contains(KeyCode.D)) {
                    dx += rx * speed;
                    dz += rz * speed;
                }
                if (keysDown.contains(KeyCode.SPACE)) {
                    dy -= speed;
                }   // up
                if (keysDown.contains(KeyCode.SHIFT)) {
                    dy += speed;
                }   // down

                cameraPosition.setX(cameraPosition.getX() + dx);
                cameraPosition.setY(cameraPosition.getY() + dy);
                cameraPosition.setZ(cameraPosition.getZ() + dz);
            }
        };
        movementTimer.start();
    }

    // ----- Data model (adapt to your own classes) -----
    public record ChunkCoord(int x, int z) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ChunkCoord(int x1, int z1))) return false;
            return x1 == x && z1 == z;
        }
    }

    public record StructureBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }

    public record BlockPlacement<T>(int x, int y, int z, T state) {
    }

    public static class ResolvedStructure<T> {
        public final ConcurrentHashMap<ChunkCoord, List<BlockPlacement<T>>> placementsByChunk;
        public final StructureBox bounds;

        public ResolvedStructure(ConcurrentHashMap<ChunkCoord, List<BlockPlacement<T>>> map, StructureBox box) {
            this.placementsByChunk = map;
            this.bounds = box;
        }
    }

    public static class ColorUtil {
        public static Color hexToRgb(String hex) {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            if (hex.length() != 6) {
                throw new IllegalArgumentException("Hex must be 6 characters long");
            }

            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            return Color.rgb(r, g, b);
        }
    }

    /**
     * Create XYZ axis lines with color coding: X=Red, Y=Green, Z=Blue
     */
    private Group createAxes(double axisLength) {
        Group axes = new Group();

        // X-axis (red)
        Cylinder xAxis = new Cylinder(0.2, axisLength);
        xAxis.setMaterial(new PhongMaterial(Color.RED));
        xAxis.getTransforms().addAll(
                new Rotate(90, Rotate.Z_AXIS),
                new Translate(axisLength / 2, 0, 0)
        );

        // Y-axis (green)
        Cylinder yAxis = new Cylinder(0.2, axisLength);
        yAxis.setMaterial(new PhongMaterial(Color.LIMEGREEN));
        yAxis.getTransforms().add(new Translate(0, -axisLength / 2, 0));

        // Z-axis (blue)
        Cylinder zAxis = new Cylinder(0.2, axisLength);
        zAxis.setMaterial(new PhongMaterial(Color.DEEPSKYBLUE));
        zAxis.getTransforms().addAll(
                new Rotate(90, Rotate.X_AXIS),
                new Translate(0, 0, axisLength / 2)
        );

        axes.getChildren().addAll(xAxis, yAxis, zAxis);
        return axes;
    }

    /**
     * Create a grid floor aligned on the XZ-plane
     */
    private Group createGrid(int halfSize, double spacing) {
        Group grid = new Group();
        PhongMaterial lineMat = new PhongMaterial(Color.GRAY);

        for (int i = -halfSize; i <= halfSize; i++) {
            // Lines parallel to X-axis
            Box lineX = new Box(halfSize * 2 * spacing, 0.05, 0.05);
            lineX.setMaterial(lineMat);
            lineX.setTranslateX(0);
            lineX.setTranslateY(0);
            lineX.setTranslateZ(i * spacing);
            grid.getChildren().add(lineX);

            // Lines parallel to Z-axis
            Box lineZ = new Box(0.05, 0.05, halfSize * 2 * spacing);
            lineZ.setMaterial(lineMat);
            lineZ.setTranslateX(i * spacing);
            lineZ.setTranslateY(0);
            lineZ.setTranslateZ(0);
            grid.getChildren().add(lineZ);
        }

        return grid;
    }

}