package org.vicky.vspe.platform.utilities;

import java.io.IOException;

public interface OutputTarget {
    void write(String fileName, String content) throws IOException;
}