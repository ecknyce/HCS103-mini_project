package clinic;

public class Doctor extends Staff {
    public Doctor(String name, int id) { super(name, id); }
    @Override public String role() { return "Doctor"; }
    @Override public double fee() { return 40 + 5 * (getId() % 5); }
    @Override public int bufferHours() { return 2; }
}
