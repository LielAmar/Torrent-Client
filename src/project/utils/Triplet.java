package project.utils;

public class Triplet<E, K, V> {

    private E first;
    private K second;
    private V third;

    public Triplet(E first, K second, V third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public E getFirst() {
        return first;
    }

    public K getSecond() {
        return second;
    }

    public V getThird() {
        return third;
    }
}
