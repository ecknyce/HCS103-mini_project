package clinic;

public class Pharmacist extends Staff {
    public Pharmacist(String name, int id) { super(name, id); }
    @Override public String role() { return "Pharmacist"; }
    @Override public double fee() { return 5; }
    @Override public int bufferHours() { return 0; }
}
