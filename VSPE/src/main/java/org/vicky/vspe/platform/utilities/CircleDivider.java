package org.vicky.vspe.platform.utilities;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class CircleDivider {

    public static class Circle {
        public final Point2D.Double center;
        public final double radius;

        public Circle(double x, double y, double radius) {
            this.center = new Point2D.Double(x, y);
            this.radius = radius;
        }

        @Override
        public String toString() {
            return String.format("Circle{center=(%.3f, %.3f), radius=%.3f}", center.x, center.y, radius);
        }
    }

    /**
     * Pack small circles of radius rSmall inside a big circle of radius RBig, up to maxCount.
     */
    public static List<Circle> pack(double RBig, double rSmall, int maxCount) {
        List<Circle> circles = new ArrayList<>();

        if (rSmall > RBig || maxCount <= 0) return circles;

        // Always start with one in the center
        circles.add(new Circle(0, 0, rSmall));
        if (maxCount == 1) return circles;

        double ringGap = 2 * rSmall; // distance between ring layers

        int placed = 1;
        int ring = 1;

        while (true) {
            double ringRadius = ring * ringGap * Math.sin(Math.PI / 6); // hex spacing approx
            if (ringRadius + rSmall > RBig) break;

            // how many fit around this ring
            double circumference = 2 * Math.PI * ringRadius;
            int k = Math.max(6 * ring, (int) Math.floor(circumference / (2 * rSmall)));
            if (k <= 0) break;

            for (int i = 0; i < k && placed < maxCount; i++) {
                double angle = 2 * Math.PI * i / k;
                double x = ringRadius * Math.cos(angle);
                double y = ringRadius * Math.sin(angle);
                circles.add(new Circle(x, y, rSmall));
                placed++;
            }

            ring++;
            if (placed >= maxCount) break;
        }

        return circles;
    }

    /**
     * Pack circles based on maximum count and spacing between centers.
     *
     * @param RBig     Radius of the large circle
     * @param spacing  Minimum distance between circle centers
     * @param Nmax     Maximum number of circles
     * @param rSmall   Radius of each small circle (optional, can be 0 for points)
     */
    public static List<Circle> packBySpacing(double RBig, double rSmall, double spacing, int Nmax) {
        List<Circle> circles = new ArrayList<>();

        if (RBig <= 0 || Nmax <= 0) return circles;
        spacing += rSmall * 2;

        // 1. Center circle
        circles.add(new Circle(0, 0, rSmall));
        if (Nmax == 1) return circles;

        int placed = 1;
        int ring = 1;

        // 2. Concentric ring placement
        while (true) {
            double ringRadius = ring * spacing;
            if (ringRadius + rSmall > RBig) break;

            // number of circles around ring (circumference / spacing)
            int count = Math.max(6 * ring, (int) Math.floor(2 * Math.PI * ringRadius / spacing));
            if (count <= 0) break;

            for (int i = 0; i < count && placed < Nmax; i++) {
                double angle = 2 * Math.PI * i / count;
                double x = ringRadius * Math.cos(angle);
                double y = ringRadius * Math.sin(angle);
                circles.add(new Circle(x, y, rSmall));
                placed++;
            }

            ring++;
            if (placed >= Nmax) break;
        }

        return circles;
    }

    /**
     * Divide a large circle (radius R) into N equal smaller circles (symmetrically placed).
     * If N == 1 → one circle same as the big one.
     * If N == 2 → two circles on opposite sides.
     * For N >= 3, places (N-1) on a ring + 1 at center.
     */
    public static List<Circle> divide(double R, int N) {
        List<Circle> result = new ArrayList<>();

        if (N <= 0) return result;

        if (N == 1) {
            result.add(new Circle(0, 0, R));
            return result;
        }

        int k = N - 1; // number of circles around
        double r = R * Math.sin(Math.PI / k) / (1 + Math.sin(Math.PI / k));
        double ringRadius = R - r;

        // Optional: Add center circle
        result.add(new Circle(0, 0, r));

        // Place k circles evenly around
        for (int i = 0; i < k; i++) {
            double angle = 2 * Math.PI * i / k;
            double x = ringRadius * Math.cos(angle);
            double y = ringRadius * Math.sin(angle);
            result.add(new Circle(x, y, r));
        }

        return result;
    }
}