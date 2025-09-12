package org.vicky.vspe_forge.forgeplatform.useables;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.vicky.vspe.platform.systems.dimension.DimensionDescriptor;

public class ForgeDescriptorDispensableDimensionEffects extends DimensionSpecialEffects {

    private final DimensionDescriptor descriptor;

    public ForgeDescriptorDispensableDimensionEffects(DimensionDescriptor descriptor) {
        super(128, // cloud level
                descriptor.hasSkyLight(), // has skylight
                SkyType.NORMAL, // sky type
                false, // force bright lightmap
                descriptor.ambientAlways()); // constant ambient light
        this.descriptor = descriptor;
    }

    @Override
    public @NotNull Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return color.multiply(0.8, 0.6, 0.9); // tinted fog
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return descriptor.resolver().foggyAt(x, z); // disable Nether-like fog
    }


}
