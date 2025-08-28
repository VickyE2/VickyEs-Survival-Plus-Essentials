package org.vicky.vspe.utilities.Math;

import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;

import java.util.Comparator;

public final class Vector3 {
    public static final Vector3 ZERO = new Vector3(0.0, 0.0, 0.0);
    public static final Vector3 UNIT_X = new Vector3(1.0, 0.0, 0.0);
    public static final Vector3 UNIT_Y = new Vector3(0.0, 1.0, 0.0);
    public static final Vector3 UNIT_Z = new Vector3(0.0, 0.0, 1.0);
    public static final Vector3 ONE = new Vector3(1.0, 1.0, 1.0);
    public final double x;
    public final double y;
    public final double z;

    public static Vector3 at(double x, double y, double z) {
        int yTrunc = (int)y;
        switch (yTrunc) {
            case 0:
                if (x == 0.0 && y == 0.0 && z == 0.0) {
                    return ZERO;
                }
                break;
            case 1:
                if (x == 1.0 && y == 1.0 && z == 1.0) {
                    return ONE;
                }
        }

        return new Vector3(x, y, z);
    }

    public static Comparator<Vector3> sortByCoordsYzx() {
        return YzxOrderComparator.YZX_ORDER;
    }

    private Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return this.x;
    }

    public Vector3 withX(double x) {
        return at(x, this.y, this.z);
    }

    public double getY() {
        return this.y;
    }

    public Vector3 withY(double y) {
        return at(this.x, y, this.z);
    }

    public double getZ() {
        return this.z;
    }

    public Vector3 withZ(double z) {
        return at(this.x, this.y, z);
    }

    public Vector3 add(Vector3 other) {
        return this.add(other.x, other.y, other.z);
    }

    public Vector3 add(double x, double y, double z) {
        return at(this.x + x, this.y + y, this.z + z);
    }

    public Vector3 add(Vector3... others) {
        double newX = this.x;
        double newY = this.y;
        double newZ = this.z;
        Vector3[] var8 = others;
        int var9 = others.length;

        for(int var10 = 0; var10 < var9; ++var10) {
            Vector3 other = var8[var10];
            newX += other.x;
            newY += other.y;
            newZ += other.z;
        }

        return at(newX, newY, newZ);
    }

    public Vector3 subtract(Vector3 other) {
        return this.subtract(other.x, other.y, other.z);
    }

    public Vector3 subtract(double x, double y, double z) {
        return at(this.x - x, this.y - y, this.z - z);
    }

    public Vector3 subtract(Vector3... others) {
        double newX = this.x;
        double newY = this.y;
        double newZ = this.z;
        Vector3[] var8 = others;
        int var9 = others.length;

        for(int var10 = 0; var10 < var9; ++var10) {
            Vector3 other = var8[var10];
            newX -= other.x;
            newY -= other.y;
            newZ -= other.z;
        }

        return at(newX, newY, newZ);
    }

    public Vector3 multiply(Vector3 other) {
        return this.multiply(other.x, other.y, other.z);
    }

    public Vector3 multiply(double x, double y, double z) {
        return at(this.x * x, this.y * y, this.z * z);
    }

    public Vector3 multiply(Vector3... others) {
        double newX = this.x;
        double newY = this.y;
        double newZ = this.z;
        Vector3[] var8 = others;
        int var9 = others.length;

        for(int var10 = 0; var10 < var9; ++var10) {
            Vector3 other = var8[var10];
            newX *= other.x;
            newY *= other.y;
            newZ *= other.z;
        }

        return at(newX, newY, newZ);
    }

    public Vector3 multiply(double n) {
        return this.multiply(n, n, n);
    }

    public Vector3 divide(Vector3 other) {
        return this.divide(other.x, other.y, other.z);
    }

    public Vector3 divide(double x, double y, double z) {
        return at(this.x / x, this.y / y, this.z / z);
    }

    public Vector3 divide(double n) {
        return this.divide(n, n, n);
    }

    public double length() {
        return Math.sqrt(this.lengthSq());
    }

    public double lengthSq() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public double distance(Vector3 other) {
        return Math.sqrt(this.distanceSq(other));
    }

    public double distanceSq(Vector3 other) {
        double dx = other.x - this.x;
        double dy = other.y - this.y;
        double dz = other.z - this.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public Vector3 normalize() {
        return this.divide(this.length());
    }

    public double dot(Vector3 other) {
        return this.x * other.x + this.y * other.y + this.z * other.z;
    }

    public Vector3 cross(Vector3 other) {
        return new Vector3(this.y * other.z - this.z * other.y, this.z * other.x - this.x * other.z, this.x * other.y - this.y * other.x);
    }

    public boolean containedWithin(Vector3 min, Vector3 max) {
        return this.x >= min.x && this.x <= max.x && this.y >= min.y && this.y <= max.y && this.z >= min.z && this.z <= max.z;
    }

    public Vector3 clampY(int min, int max) {
        Preconditions.checkArgument(min <= max, "minimum cannot be greater than maximum");
        if (this.y < (double)min) {
            return at(this.x, min, this.z);
        } else {
            return this.y > (double)max ? at(this.x, max, this.z) : this;
        }
    }

    public Vector3 floor() {
        return at(Math.floor(this.x), Math.floor(this.y), Math.floor(this.z));
    }

    public Vector3 ceil() {
        return at(Math.ceil(this.x), Math.ceil(this.y), Math.ceil(this.z));
    }

    public Vector3 round() {
        return at(Math.floor(this.x + 0.5), Math.floor(this.y + 0.5), Math.floor(this.z + 0.5));
    }

    public Vector3 abs() {
        return at(Math.abs(this.x), Math.abs(this.y), Math.abs(this.z));
    }

    public Vector3 transform2D(double angle, double aboutX, double aboutZ, double translateX, double translateZ) {
        angle = Math.toRadians(angle);
        double x = this.x - aboutX;
        double z = this.z - aboutZ;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x2 = x * cos - z * sin;
        double z2 = x * sin + z * cos;
        return new Vector3(x2 + aboutX + translateX, this.y, z2 + aboutZ + translateZ);
    }

    public double toPitch() {
        double x = this.getX();
        double z = this.getZ();
        if (x == 0.0 && z == 0.0) {
            return this.getY() > 0.0 ? -90.0 : 90.0;
        } else {
            double x2 = x * x;
            double z2 = z * z;
            double xz = Math.sqrt(x2 + z2);
            return Math.toDegrees(Math.atan(-this.getY() / xz));
        }
    }

    public double toYaw() {
        double x = this.getX();
        double z = this.getZ();
        double t = Math.atan2(-x, z);
        double tau = Math.TAU;
        return Math.toDegrees((t + tau) % tau);
    }

    public Vector3 getMinimum(Vector3 v2) {
        return new Vector3(Math.min(this.x, v2.x), Math.min(this.y, v2.y), Math.min(this.z, v2.z));
    }

    public Vector3 getMaximum(Vector3 v2) {
        return new Vector3(Math.max(this.x, v2.x), Math.max(this.y, v2.y), Math.max(this.z, v2.z));
    }
    public boolean equals(Object obj) {
        if (!(obj instanceof Vector3 other)) {
            return false;
        } else {
            return other.x == this.x && other.y == this.y && other.z == this.z;
        }
    }

    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + Double.hashCode(this.x);
        hash = 31 * hash + Double.hashCode(this.y);
        hash = 31 * hash + Double.hashCode(this.z);
        return hash;
    }

    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ")";
    }

    public String toParserString() {
        return this.x + "," + this.y + "," + this.z;
    }

    private static final class YzxOrderComparator {
        private static final Comparator<Vector3> YZX_ORDER = (a, b) -> {
            return ComparisonChain.start().compare(a.y, b.y).compare(a.z, b.z).compare(a.x, b.x).result();
        };

        private YzxOrderComparator() {
        }
    }
}
