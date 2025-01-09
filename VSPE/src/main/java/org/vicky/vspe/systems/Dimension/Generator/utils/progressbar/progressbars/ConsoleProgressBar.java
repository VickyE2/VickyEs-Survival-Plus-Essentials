package org.vicky.vspe.systems.Dimension.Generator.utils.progressbar.progressbars;

import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.VSPE;
import org.vicky.vspe.systems.Dimension.Generator.utils.progressbar.ProgressListener;

public class ConsoleProgressBar implements ProgressListener {
    @NotNull
    private static StringBuilder getStringBuilder(float progressFraction, int totalWidth) {
        int completedBlocks = (int) progressFraction; // Whole blocks completed
        int partialBlockIndex = (int) ((progressFraction - completedBlocks) * 8); // Fractional block index

        // Block characters for partial progress
        char[] blockChars = {' ', '▏', '▎', '▍', '▌', '▋', '▊', '▉', '█'};

        // Build the progress bar
        StringBuilder progressBar = new StringBuilder("[");
        for (int i = 0; i < totalWidth; i++) {
            if (i < completedBlocks) {
                progressBar.append("█");
            } else if (i == completedBlocks && partialBlockIndex > 0) {
                progressBar.append(blockChars[partialBlockIndex]);
            } else {
                progressBar.append(" ");
            }
        }
        progressBar.append("]");
        return progressBar;
    }

    @Override
    public void onProgressUpdate(int progress, String currentProcess) {
        int totalWidth = 30;
        float progressFraction = (progress / 100.0f) * totalWidth;
        StringBuilder progressBar = getStringBuilder(progressFraction, totalWidth);
        String output = String.format("\r%s %3d%% Current Process: %s", progressBar, progress, currentProcess);
        VSPE.getInstancedLogger().info(output.trim());
        if (progress == 100) {
            VSPE.getInstancedLogger().info("Done!");  // No newline after "Done!"
        }
    }


}

