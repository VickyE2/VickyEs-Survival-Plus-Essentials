package org.vicky.vspe.nms;

import org.bukkit.Bukkit;
import org.vicky.platform.PlatformPlugin;
import org.vicky.vspe.nms.block.BlockCompatibility;
import org.vicky.vspe.nms.command.CommandCompatibility;
import org.vicky.vspe.nms.entity.EntityCompatibility;
import org.vicky.vspe.nms.nbt.NBTCompatibility;
import org.vicky.vspe.nms.utils.MinecraftVersions;
import org.vicky.vspe.nms.utils.ReflectionUtil;
import org.vicky.vspe.nms.vault.IVaultCompatibility;
import org.vicky.vspe.nms.worldguard.NoWorldGuard;
import org.vicky.vspe.nms.worldguard.WorldGuardCompatibility;

import java.lang.reflect.Constructor;

public final class CompatibilityAPI {

    private static ICompatibility compatibility;
    private static WorldGuardCompatibility worldGuardCompatibility;
    private static IVaultCompatibility vaultCompatibility;
    private static boolean isPaper;

    static {
        try {
            boolean isPaper1;
            try {
                Class.forName("com.destroystokyo.paper.VersionHistoryManager$VersionData");
                isPaper1 = true;
            } catch (ClassNotFoundException ex) {
                isPaper1 = false;
            }
            isPaper = isPaper1;

            compatibility = new CompatibilitySetup().getCompatibleVersion(ICompatibility.class, "org.vicky.vspe.nms.impl");

            // This happens when a server is using an unsupported version of
            // minecraft, like 1.18.1, 1.8.8, etc.
            if (compatibility == null) {
                PlatformPlugin.logger().error("Unsupported server version: " + Bukkit.getVersion() + " (" + Bukkit.getBukkitVersion() + ")\n" +
                        "Remember that MechanicsCore supports all major versions 1.12.2+, HOWEVER it doesn't support outdated versions\n" +
                        "For example, 1.18.1 is NOT a support version, but 1.18.2 IS a supported version\n" +
                        "If you are running a brand new version of Minecraft, ask DeeCaaD or CJCrafter to update the plugin\n" +
                        "\n" +
                        "!!! CRITICAL ERROR !!!");
            }

            // * ----- World Guard ----- * //
            WorldGuardCompatibility worldGuardCompatibility1;
            try {
                // Check if WorldGuard is there
                Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
                if (!MinecraftVersions.UPDATE_AQUATIC.isAtLeast()) {
                    // World Guard V6 for 1.12.2 support
                    Constructor<?> worldGuardV6Constructor = ReflectionUtil.getConstructor(Class.forName("me.deecaad.core.compatibility.worldguard.WorldGuardV6"));
                    worldGuardCompatibility1 = (WorldGuardCompatibility) ReflectionUtil.newInstance(worldGuardV6Constructor);
                } else {
                    // World Guard V7 for 1.13+ support
                    Constructor<?> worldGuardV7Constructor = ReflectionUtil.getConstructor(Class.forName("me.deecaad.core.compatibility.worldguard.WorldGuardV7"));
                    worldGuardCompatibility1 = (WorldGuardCompatibility) ReflectionUtil.newInstance(worldGuardV7Constructor);
                }
            } catch (Throwable e) {
                worldGuardCompatibility1 = new NoWorldGuard();
            }
            worldGuardCompatibility = worldGuardCompatibility1;
        } catch (Throwable ex) {
            PlatformPlugin.logger().error("Failed to init CompatibilityAPI", ex);
        }
    }

    public static boolean isPaper() {
        return isPaper;
    }

    public static ICompatibility getCompatibility() {
        return compatibility;
    }

    public static EntityCompatibility getEntityCompatibility() {
        return compatibility.getEntityCompatibility();
    }

    public static BlockCompatibility getBlockCompatibility() {
        return compatibility.getBlockCompatibility();
    }

    public static NBTCompatibility getNBTCompatibility() {
        return compatibility.getNBTCompatibility();
    }

    public static CommandCompatibility getCommandCompatibility() {
        return compatibility.getCommandCompatibility();
    }

    public static WorldGuardCompatibility getWorldGuardCompatibility() {
        return worldGuardCompatibility;
    }

    public static IVaultCompatibility getVaultCompatibility() {
        if (vaultCompatibility == null) {
            // * ----- Vault ----- * //
            boolean hasVault = Bukkit.getPluginManager().getPlugin("Vault") != null;
            String path = "me.deecaad.core.compatibility.vault." + (hasVault ? "VaultCompatibility" : "NoVaultCompatibility");
            vaultCompatibility = ReflectionUtil.newInstance(ReflectionUtil.getClass(path));
        }
        return vaultCompatibility;
    }
}