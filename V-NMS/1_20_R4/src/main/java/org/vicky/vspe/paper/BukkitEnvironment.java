package org.vicky.vspe.paper;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.platform.PlatformEnvironment;

public class BukkitEnvironment implements PlatformEnvironment {

    private final World.Environment enviroment;

    private BukkitEnvironment(World.Environment environment) {
        this.enviroment = environment;
    }

    public static BukkitEnvironment of(World.Environment environment) {
        return new BukkitEnvironment(environment);
    }

    @Override
    public String getName() {
        return enviroment.name();
    }

    public @NotNull World.Environment getNative() {
        return enviroment;
    }
}
