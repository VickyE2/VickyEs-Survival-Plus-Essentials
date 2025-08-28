package org.vicky.vspe.paper;

import org.bukkit.Bukkit;
import org.vicky.platform.PlatformScheduler;
import org.vicky.vspe.VSPE;

public class VSPEBukkitPlatformScheduler implements PlatformScheduler {
    @Override
    public void runMain(Runnable runnable) {
        Bukkit.getScheduler().runTask(VSPE.getPlugin(), runnable);
    }

    @Override
    public void runScheduled(Runnable runnable, Long aLong) {
        Bukkit.getScheduler().runTaskLater(VSPE.getPlugin(), runnable, aLong);
    }
}
