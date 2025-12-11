package org.vicky.vspe.platform.utilities;

public class Quad<A, B, C, D> {
    public final A first;
    public final B second;
    public final C third;
    public final D fourth;

    public Quad(A first, B second, C third, D fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    public static <A, B, C, D> Quad<A, B, C, D> of(A a, B b, C c, D d) {
        return new Quad<>(a, b, c, d);
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ", " + third + ", " + fourth + ")";
    }
}

