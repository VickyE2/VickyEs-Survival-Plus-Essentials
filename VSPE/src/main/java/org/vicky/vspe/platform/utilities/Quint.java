package org.vicky.vspe.platform.utilities;

public class Quint<A, B, C, D, E> {
    public final A first;
    public final B second;
    public final C third;
    public final D fourth;
    public final E fifth;

    public Quint(A first, B second, C third, D fourth, E fifth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
        this.fifth = fifth;
    }

    public static <A, B, C, D, E> Quint<A, B, C, D, E> of(A a, B b, C c, D d, E e) {
        return new Quint<>(a, b, c, d, e);
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ", " + third + ", " + fourth + ", " + fifth + ")";
    }
}
