package clinic;

public abstract class Staff extends Person {
    protected Staff(String name, int id) { super(name, id); }
    public abstract double fee();
    public abstract int bufferHours();
}
