package clinic;

import java.util.*;

/*
 * Clinic invariants:
 * - all registered Person IDs and appointment IDs are unique in this Clinic.
 * - every appointment refers to registered patient and staff objects.
 * - no patient or staff member has overlapping non-cancelled appointments.
 * - staff buffer rules are respected; cancelled appointments do not block slots.
 * - internal collections are private and never returned directly.
 * - every public action is logged, including failed actions.
 */
public class Clinic {
    private final Map<Integer, Patient> patients = new LinkedHashMap<>();
    private final Map<Integer, Staff> staffMembers = new LinkedHashMap<>();
    private final Map<Integer, Appointment> appointments = new LinkedHashMap<>();
    private final Map<Integer, List<Patient>> waitingLists = new HashMap<>();
    private final Clock clock;
    private final AppointmentLog log;
    private int nextAppointmentId = 1;

    public Clinic(Clock clock, AppointmentLog log) {
        this.clock = Objects.requireNonNull(clock);
        this.log = Objects.requireNonNull(log);
    }

    private void action(String description, Runnable operation) {
        try {
            operation.run();
            log.write("SUCCESS " + description);
        } catch (RuntimeException ex) {
            log.write("FAILED " + description + ": " + ex.getMessage());
            throw ex;
        }
    }

    public void registerPatient(Patient patient) {
        action("register patient #" + patient.getId(), () -> {
            ensureUniquePersonId(patient.getId());
            patients.put(patient.getId(), patient);
        });
    }

    public void registerStaff(Staff member) {
        action("register staff #" + member.getId(), () -> {
            ensureUniquePersonId(member.getId());
            staffMembers.put(member.getId(), member);
            waitingLists.put(member.getId(), new ArrayList<>());
        });
    }

    private void ensureUniquePersonId(int id) {
        if (patients.containsKey(id) || staffMembers.containsKey(id))
            throw new IllegalArgumentException("Person ID " + id + " is already registered.");
    }

    public Appointment book(Patient patient, Staff member, int startHour) {
        final Appointment[] result = new Appointment[1];
        action("book patient #" + patient.getId() + " with staff #" + member.getId() + " at " + startHour, () -> {
            if (patients.get(patient.getId()) != patient || staffMembers.get(member.getId()) != member)
                throw new IllegalArgumentException("Patient and staff must be registered with this clinic.");
            if (startHour < 0 || startHour > 167) throw new IllegalArgumentException("Start hour must be 0..167.");
            ensureNoConflict(patient, member, startHour, null);
            ensureBuffer(member, startHour, null);
            int id = nextAppointmentId++;
            Appointment appointment = new Appointment(id, patient, member, startHour);
            if (appointments.containsKey(id)) {
                throw new IllegalStateException("Appointments should have unique ids");
            }
            appointments.put(id, appointment);
            result[0] = appointment;
            log.write("BOOKED " + appointment);
        });
        return result[0];
    }

    public void confirm(int appointmentId) {
        action("confirm #" + appointmentId, () -> getAppointment(appointmentId).confirm());
    }

    public void cancel(int appointmentId, String reason) {
        action("cancel #" + appointmentId, () -> {
            Appointment appointment = getAppointment(appointmentId);
            appointment.cancel(clock.now(), reason);
            log.write("CANCELLED #" + appointmentId + " reason=" + appointment.getCancellationReason());
            offerWaitingPatient(appointment);
        });
    }

    private Appointment getAppointment(int id) {
        Appointment appointment = appointments.get(id);
        if (appointment == null) throw new IllegalArgumentException("Appointment #" + id + " does not exist.");
        return appointment;
    }

    private void ensureNoConflict(Patient patient, Staff member, int hour, Appointment ignored) {
        for (Appointment existing : appointments.values()) {
            if (existing == ignored || existing.getStatus() == Appointment.Status.CANCELLED) continue;
            if (existing.getStartHour() == hour &&
                    (existing.getPatient().getId() == patient.getId() ||
                     existing.getStaff().getId() == member.getId()))
                throw new IllegalStateException("Patient or staff member already has an appointment at hour " + hour + ".");
        }
    }

    private void ensureBuffer(Staff member, int hour, Appointment ignored) {
        int requiredGap = member.bufferHours(); // polymorphism: Clinic does not inspect staff types.
        for (Appointment existing : appointments.values()) {
            if (existing == ignored || existing.getStatus() == Appointment.Status.CANCELLED) continue;
            if (existing.getStaff().getId() == member.getId()) {
                int distance = Math.abs(hour - existing.getStartHour());
                if (distance < 1 + requiredGap)
                    throw new IllegalStateException("Staff buffer of " + requiredGap + " hour(s) is required.");
            }
        }
    }

    /** Emergency rule: bump only a PENDING appointment at the same hour. */
    public Appointment emergencyBook(Patient emergencyPatient, Staff member, int hour) {
        if (!emergencyPatient.isEmergency()) throw new IllegalArgumentException("Patient is not flagged as emergency.");
        final Appointment[] result = new Appointment[1];
        action("emergency book patient #" + emergencyPatient.getId() + " at " + hour, () -> {
            Appointment pendingToBump = null;
            for (Appointment a : appointments.values()) {
                if (a.getStaff().getId() == member.getId() && a.getStartHour() == hour
                        && a.getStatus() == Appointment.Status.PENDING) {
                    pendingToBump = a;
                    break;
                }
                if (a.getStaff().getId() == member.getId() && a.getStartHour() == hour
                        && a.getStatus() == Appointment.Status.CONFIRMED)
                    throw new IllegalStateException("A confirmed appointment can never be bumped.");
            }
            if (pendingToBump != null) pendingToBump.cancel(clock.now(), "bumped");
            ensureNoConflict(emergencyPatient, member, hour, null);
            ensureBuffer(member, hour, pendingToBump);
            Appointment created = new Appointment(nextAppointmentId++, emergencyPatient, member, hour);
            appointments.put(created.getId(), created);
            result[0] = created;
            log.write("EMERGENCY BOOKED " + created);
        });
        return result[0];
    }

    public void joinWaitingList(Patient patient, Staff member) {
        action("waiting-list join patient #" + patient.getId() + " staff #" + member.getId(), () -> {
            if (patients.get(patient.getId()) != patient || staffMembers.get(member.getId()) != member)
                throw new IllegalArgumentException("Patient and staff must be registered.");
            List<Patient> queue = waitingLists.get(member.getId());
            if (queue.contains(patient)) throw new IllegalStateException("Patient is already waiting.");
            queue.add(patient);
        });
    }

    private void offerWaitingPatient(Appointment cancelled) {
        List<Patient> queue = waitingLists.get(cancelled.getStaff().getId());
        if (queue == null) return;
        Iterator<Patient> iterator = queue.iterator();
        while (iterator.hasNext()) {
            Patient candidate = iterator.next();
            try {
                ensureNoConflict(candidate, cancelled.getStaff(), cancelled.getStartHour(), cancelled);
                ensureBuffer(cancelled.getStaff(), cancelled.getStartHour(), cancelled);
                Appointment offered = new Appointment(nextAppointmentId++, candidate,
                        cancelled.getStaff(), cancelled.getStartHour());
                appointments.put(offered.getId(), offered);
                iterator.remove();
                log.write("WAITING-LIST OFFERED " + offered);
                return;
            } catch (IllegalStateException conflict) {
                // Keep searching; the first eligible waiting patient receives the slot.
            }
        }
    }

    public List<Appointment> getAppointments() {
        return Collections.unmodifiableList(new ArrayList<>(appointments.values()));
    }

    public List<Patient> getWaitingList(Staff member) {
        List<Patient> queue = waitingLists.get(member.getId());
        if (queue == null) return List.of();
        return Collections.unmodifiableList(new ArrayList<>(queue));
    }

    public List<Staff> getStaffMembers() {
        return Collections.unmodifiableList(new ArrayList<>(staffMembers.values()));
    }
}
