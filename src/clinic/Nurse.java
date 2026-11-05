package clinic;

public class Nurse extends Staff {
    public Nurse(String name, int id) { super(name, id); }
    @Override public String role() { return "Nurse"; }
    @Override public double fee() { return 10 + (getId() % 3); }
    @Override public int bufferHours() { return 1; }
}
