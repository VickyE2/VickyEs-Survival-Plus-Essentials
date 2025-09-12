package org.vicky.vspe.platform.systems.dimension.globalDimensions;

import kotlin.Unit;
import org.vicky.platform.PlatformPlugin;
import org.vicky.platform.utils.Direction;
import org.vicky.platform.utils.ResourceLocation;
import org.vicky.platform.utils.Vec3;
import org.vicky.vspe.platform.NativeTypeMapper;
import org.vicky.vspe.platform.VSPEPlatformPlugin;
import org.vicky.vspe.platform.systems.dimension.*;
import org.vicky.vspe.systems.dimension.PortalContext;
import org.vicky.vspe.systems.dimension.PortalLinkedStrategy;

import java.util.Arrays;
import java.util.List;

public final class DimensionDescriptors {
    public static final DimensionDescriptor CRYMORRA =
            new DimensionDescriptor(
                    "Crymorra",
                    "A land of frozen war...",
                    true,
                    List.of(DimensionType.FROZEN_WORLD, DimensionType.ELEMENTAL_WORLD),
                    "vspe:crymorra",
                    BiomeResolvers.getInstance().CRYMORRA_BIOME_RESOLVER(),
                    64,
                    PlatformPlugin.stateFactory().getBlockState("minecraft:water"),
                    24000,
                    true,
                    false,
                    false,
                    false,
                    true,
                    false,
                    false,
                    0.7f,
                    4.0f,
                    15,
                    15,
                    218,
                    -64,
                    319,
                    (player) -> new PortalContext<>(
                            player.getLocation(),
                            player,
                            (x, y, z, dimension) -> {
                                PortalFrameUtil.PortalPattern nether = PortalFrameUtil.create(
                                        PortalFrameUtil.PortalShape.RECTANGLE, 3, 4, Direction.NORTH
                                );
                                for (int[] c : nether.frame) {
                                    System.out.println(Arrays.toString(c));
                                }
                                for (int[] c : nether.interior) {
                                    System.out.println(Arrays.toString(c));
                                }
                                Vec3 origin = new Vec3(x, y, z);
                                List<Vec3> frameVec3 = PortalFrameUtil.toVec3List(nether.frame, origin);
                                List<Vec3> portalVec3 = PortalFrameUtil.toVec3List(nether.interior, origin);
                                for (var frame : frameVec3) {
                                    dimension.getWorld().setPlatformBlockState(frame, VSPEPlatformPlugin.blockStateCreator().getBlockState(ResourceLocation.from(NativeTypeMapper.getFor("frozen_portal_frame"))));
                                }
                                for (var portal : portalVec3) {
                                    dimension.getWorld().setPlatformBlockState(portal, VSPEPlatformPlugin.blockStateCreator().getBlockState(ResourceLocation.from(NativeTypeMapper.getFor("frozen_portal"))));
                                }
                                return Unit.INSTANCE;
                            }
                    ),
                    new PortalLinkedStrategy<>(4.0f),
                    TimeCurve.EASE_IN_OUT_QUINT
            );

    static {
        // automatic core-side registration — safe even before platform exists
        CoreDimensionRegistry.register(CRYMORRA);
    }
}
