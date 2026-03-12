package org.vicky.vspe.platform.utilities;

import org.vicky.utilities.ANSIColor;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ProgressTracker {
    private final int totalTicks;
    private int currentTick = 0;
    private final long globalStartTime;

    // Ordered list of stages for consistent rendering
    private final List<String> stageOrder = new ArrayList<>();
    private final Map<String, StageStats> stages = new LinkedHashMap<>();
    private String currentActiveStage = "";
    private String currentActiveSubStage = "";
    private long currentStageStart = 0;
    private long currentSubStageStart = 0;
    private boolean showDashboard = true;

    private static final int BAR_WIDTH = 30;
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";

    public ProgressTracker(int totalTicks, boolean showDashboard) {
        this.totalTicks = totalTicks;
        this.globalStartTime = System.nanoTime();
        this.showDashboard = showDashboard;

        // Pre-define stages so they always appear in the same order in the list
        registerStage("Attractor Genertion");
        registerStage("Initialization");
        registerStage("Attractors");
        registerStage("Growth (Parallel)");
        registerStage("Shedding");

        if (!showDashboard) return;
        System.out.print("\n".repeat(stageOrder.size() + 3));
    }

    private void registerStage(String name) {
        if (!showDashboard) return;
        if (!stages.containsKey(name)) {
            stageOrder.add(name);
            stages.put(name, new StageStats());
        }
    }

    public void startTick(int tick) {
        if (!showDashboard) return;
        this.currentTick = tick;
        render();
    }

    public void startPhase(String name) {
        if (!showDashboard) return;
        currentActiveStage = name;
        currentActiveSubStage = ""; // Reset sub-stage
        currentStageStart = System.nanoTime();
        registerStage(name); // Safety in case a new stage appears dynamically
        render();
    }

    public void startSubPhase(String subName) {
        if (!showDashboard) return;
        if (currentActiveStage.isEmpty()) return;
        currentActiveSubStage = subName;
        currentSubStageStart = System.nanoTime();
        // Ensure parent stage exists
        StageStats parent = stages.get(currentActiveStage);
        if (parent != null) {
            if (!parent.subStages.containsKey(subName)) {
                parent.subStageOrder.add(subName);
                parent.subStages.put(subName, new StageStats());
            }
        }
        render();
    }

    public void endSubPhase(String subName) {
        if (!showDashboard) return;
        if (currentActiveStage.isEmpty()) return;
        long elapsed = System.nanoTime() - currentSubStageStart;
        StageStats parent = stages.get(currentActiveStage);
        if (parent != null) {
            StageStats sub = parent.subStages.get(subName);
            if (sub != null) {
                if (elapsed > sub.maxTime) sub.maxTime = elapsed;
                sub.totalTime += elapsed;
                sub.count++;
            }
        }
        currentActiveSubStage = "";
    }

    public void endPhase(String name) {
        if (!showDashboard) return;
        long elapsed = System.nanoTime() - currentStageStart;
        StageStats stats = stages.get(name);
        if (stats != null) {
            if (elapsed > stats.maxTime) stats.maxTime = elapsed;
            stats.totalTime += elapsed;
            stats.count++;
        }
        currentActiveStage = "";
        // Don't render here to save console IO, wait for next startPhase or tick
    }

    private void render() {
        if (!showDashboard) return;
        // 1. Move cursor UP to the start of our dashboard
        // Line count calculation needs to include sub-stages
        int subStageLines = 0;
        for (StageStats s : stages.values()) {
            subStageLines += s.subStageOrder.size();
        }
        int linesToMoveUp = stageOrder.size() + subStageLines + 3;
        System.out.print("\033[" + linesToMoveUp + "A");

        // 2. Render Main Progress Bar
        float percent = (float) currentTick / totalTicks;
        int filled = (int) (percent * BAR_WIDTH);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\r  Tree Gen: [%s%s] %s%3d%%%s (Age %d/%d)    %n",
                "=".repeat(filled), " ".repeat(BAR_WIDTH - filled),
                ANSI_GREEN, (int)(percent * 100), ANSI_RESET,
                currentTick, totalTicks));
        sb.append("  ------------------------------------------------------------\n");

        // 3. Render Stage List
        long totalSimTime = System.nanoTime() - globalStartTime;

        for (String stageName : stageOrder) {
            StageStats stats = stages.get(stageName);
            boolean isActive = stageName.equals(currentActiveStage);

            String indicator = isActive ? ANSI_CYAN + ">" + ANSI_RESET : " ";
            String nameCol = isActive ? ANSI_CYAN + String.format("%-20s", stageName) + ANSI_RESET : String.format("%-20s", stageName);

            // Calculate percentage of total time this stage is taking
            double stagePct = totalSimTime > 0 ? (stats.totalTime * 100.0 / totalSimTime) : 0;

            // If active, add current elapsed time to the display
            long displayTime = stats.totalTime;
            if (isActive) {
                displayTime += (System.nanoTime() - currentStageStart);
            }

            sb.append(String.format("  %s %s | %10s | Avg: %8s | Max: %8s | %s%4.1f%%%s%n",
                    indicator,
                    nameCol,
                    formatDuration(displayTime),
                    formatDuration(stats.count > 0 ? stats.totalTime / stats.count : 0),
                    formatDuration(stats.maxTime),
                    ANSI_YELLOW, stagePct, ANSI_RESET
            ));

            // Render Sub-stages
            for (String subName : stats.subStageOrder) {
                StageStats subStats = stats.subStages.get(subName);
                boolean isSubActive = isActive && subName.equals(currentActiveSubStage);
                
                String subIndicator = isSubActive ? ANSI_CYAN + "  -" + ANSI_RESET : ANSIColor.LIGHT_GRAY + "  +" + ANSI_RESET;
                String subNameCol = isSubActive ? ANSI_CYAN + String.format("%-18s", subName) + ANSI_RESET :
                        ANSIColor.LIGHT_GRAY + String.format("%-18s", subName) + ANSI_RESET;
                
                // Sub-stage percentage relative to PARENT stage
                double subPct = displayTime > 0 ? (subStats.totalTime * 100.0 / displayTime) : 0;
                
                long subDisplayTime = subStats.totalTime;
                if (isSubActive) {
                    subDisplayTime += (System.nanoTime() - currentSubStageStart);
                }

                sb.append(String.format("  %s %s | %10s | Avg: %8s | Max: %8s | %s%4.1f%%%s%n",
                        subIndicator,
                        subNameCol,
                        formatDuration(subDisplayTime),
                        formatDuration(subStats.count > 0 ? subStats.totalTime / subStats.count : 0),
                        formatDuration(subStats.maxTime),
                        ANSIColor.PURPLE, subPct, ANSI_RESET
                ));
            }
        }
        sb.append("  --------------------------------------------------------------------");

        // Print everything in one go to reduce flicker
        // Clear lines while printing
        String[] lines = sb.toString().split("\n");
        for (String line : lines) {
            System.out.print("\033[2K"); // Clear line
            System.out.println(line);
        }
    }

    public void finish() {
        if (!showDashboard) return;
        render(); // Final render
        System.out.println("\nSimulation Complete.");
    }

    private String formatDuration(long nanos) {
        if (nanos < 1_000_000) return String.format("%.2f ms", nanos / 1e6);
        if (nanos < 1_000_000_000) return String.format("%3.0f ms", nanos / 1e6);
        return String.format("%.2f s ", nanos / 1e9);
    }

    private static class StageStats {
        long totalTime = 0;
        long maxTime = 0;
        int count = 0;
        // Sub-stage data
        final List<String> subStageOrder = new ArrayList<>();
        final Map<String, StageStats> subStages = new LinkedHashMap<>();
    }
}