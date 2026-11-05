package clinic;

public class Patient extends Person {
    public enum PatientType { STUDENT, STAFF_MEMBER, VISITOR }

    private final PatientType patientType;
    private boolean emergency;

    public Patient(String name, int id) { this(name, id, PatientType.VISITOR); }

    public Patient(String name, int id, PatientType patientType) {
        super(name, id);
        if (patientType == null) throw new IllegalArgumentException("Patient type is required.");
        this.patientType = patientType;
    }

    @Override public String role() { return "Patient"; }
    public PatientType getPatientType() { return patientType; }
    public boolean isEmergency() { return emergency; }
    public void setEmergency(boolean emergency) { this.emergency = emergency; }

    /** Discount is polymorphic: no type checks are needed by Clinic. */
    public double discountRate() {
        return switch (patientType) {
            case STUDENT -> 0.50;
            case STAFF_MEMBER -> 1.00;
            case VISITOR -> 0.00;
        };
    }

    public double amountPayable(double staffFee) {
        return staffFee * (1.0 - discountRate());
    }
}
