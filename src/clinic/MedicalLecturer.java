package clinic;

public class MedicalLecturer extends Doctor implements Lecturer {
    private final String moduleName;

    public MedicalLecturer(String name, int id, String moduleName) {
        super(name, id); // Doctor -> Staff -> Person; Person is constructed exactly once.
        if (moduleName == null || moduleName.trim().isEmpty())
            throw new IllegalArgumentException("Module name cannot be empty.");
        this.moduleName = moduleName.trim();
    }

    @Override public String role() { return "Doctor and Lecturer"; }
    @Override public String moduleName() { return moduleName; }
    // fee() is inherited from Doctor: a MedicalLecturer is still a doctor for billing.
}
