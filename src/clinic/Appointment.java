package clinic;

/*
 * Appointment invariants:
 * - id is positive; startHour is 0..167 and the appointment lasts exactly one hour.
 * - patient and staff are never null.
 * - status starts PENDING and can only move PENDING -> CONFIRMED/CANCELLED
 *   or CONFIRMED -> CANCELLED. CANCELLED is terminal.
 * - a confirmed appointment cannot be cancelled less than 2 hours before start;
 *   no appointment can be cancelled at or after its start hour.
 * - status changes are controlled by Clinic through package-private methods.
 */
public final class Appointment {
    public enum Status { PENDING, CONFIRMED, CANCELLED }

    private final int id;
    private final Patient patient;
    private final Staff staff;
    private final int startHour;
    private Status status;
    private String cancellationReason = "";

    Appointment(int id, Patient patient, Staff staff, int startHour) {
        if (id <= 0) throw new IllegalArgumentException("Appointment ID must be positive.");
        if (patient == null || staff == null) throw new IllegalArgumentException("Patient and staff are required.");
        if (startHour < 0 || startHour > 167) throw new IllegalArgumentException("Start hour must be 0..167.");
        this.id = id;
        this.patient = patient;
        this.staff = staff;
        this.startHour = startHour;
        this.status = Status.PENDING;
    }

    public int getId() { return id; }
    public Patient getPatient() { return patient; }
    public Staff getStaff() { return staff; }
    public int getStartHour() { return startHour; }
    public int getEndHour() { return startHour + 1; }
    public Status getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }
    public double price() { return patient.amountPayable(staff.fee()); }

    void confirm() {
        if (status != Status.PENDING) throw new IllegalStateException("Only pending appointments can be confirmed.");
        status = Status.CONFIRMED;
    }

    void cancel(int now, String reason) {
        if (status == Status.CANCELLED) throw new IllegalStateException("Cancelled appointments cannot return or be cancelled again.");
        if (now >= startHour) throw new IllegalStateException("Appointment has already started.");
        if (status == Status.CONFIRMED && startHour - now < 2)
            throw new IllegalStateException("Too late: confirmed appointments require 2 hours' notice.");
        status = Status.CANCELLED;
        cancellationReason = (reason == null || reason.isBlank()) ? "cancelled" : reason;
    }

    @Override public String toString() {
        return "#" + id + " " + patient.getName() + " with " + staff.getName()
                + " at hour " + startHour + " [" + status + "]";
    }
}
