package org.vicky.vspe.nms.utils;

import org.jetbrains.annotations.NotNull;

/**
 * This interface is used to mark classes that have a key (or a unique identifier) which can be used
 * for {@link Registry}.
 */
public interface Keyable {
    @NotNull String getKey();
}
