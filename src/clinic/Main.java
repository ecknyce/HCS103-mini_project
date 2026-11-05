package clinic;

import java.nio.file.Path;

public class Main {
    // we used P as Student number
    private static final int STUDENT_NUMBER_P = 2420449;  

    public static void main(String[] args) {
        ManualClock clock = new ManualClock(10);
        try (AppointmentLog log = new AppointmentLog(Path.of("appointments.log"))) {
            Clinic clinic = new Clinic(clock, log);

            Patient patient = new Patient("Philip Chamisa", 101, Patient.PatientType.STUDENT);
            Patient secondPatient = new Patient("Ecknyce Phiri", 102, Patient.PatientType.VISITOR);
            Patient thirdPatient = new Patient("Rudo Ncube", 103, Patient.PatientType.STAFF_MEMBER);
            Doctor doctor = new Doctor("Dr. Ndlovu", 201);
            Nurse nurse = new Nurse("Nurse Moyo", 202);
            Pharmacist pharmacist = new Pharmacist("Pharmacist Dube", 203);

            clinic.registerPatient(patient);
            clinic.registerPatient(secondPatient);
            clinic.registerPatient(thirdPatient);
            clinic.registerStaff(doctor);
            clinic.registerStaff(nurse);
            clinic.registerStaff(pharmacist);

            System.out.println("\n--- POLYMORPHISM: ONE LOOP, NO TYPE CHECKS ---");
            for (Staff member : clinic.getStaffMembers())
                System.out.println(member.role() + " fee: $" + member.fee());

            System.out.println("\n--- BOOKING AND CONFIRMATION ---");
            Appointment first = clinic.book(patient, nurse, 20);
            clinic.confirm(first.getId());
            System.out.println("Price after patient discount: $" + first.price());

            System.out.println("\n--- DELIBERATE FAILURE: CONFLICT ---");
            try { clinic.book(secondPatient, doctor, 20); }
            catch (RuntimeException ex) { System.out.println("Expected failure: " + ex.getMessage()); }

            System.out.println("\n--- TWIST P MOD 4 = " + (STUDENT_NUMBER_P % 4) + " ---");
            runTwist(clinic, clock, patient, secondPatient, thirdPatient, doctor, nurse);

            System.out.println("\n--- MEDICAL LECTURER: SINGLE PERSON CONSTRUCTION ---");
            int before = Person.getConstructorCount();
            MedicalLecturer lecturer = new MedicalLecturer("Dr. Chuma", 301, "Human Anatomy");
            int after = Person.getConstructorCount();
            System.out.println(lecturer.role() + ", module: " + lecturer.moduleName());
            System.out.println("Doctor fee inherited: $" + lecturer.fee());
            System.out.println("Person constructor calls for MedicalLecturer: " + (after - before));

            System.out.println("\n--- ALL APPOINTMENTS ---");
            for (Appointment appointment : clinic.getAppointments()) System.out.println(appointment);

            System.out.println("\n--- CANCELLATION RULE FAILURE ---");
            clock.setNow(19); // One hour before the confirmed appointment: deliberately too late.
            try { clinic.cancel(first.getId(), "late cancellation"); }
            catch (RuntimeException ex) { System.out.println("Expected failure: " + ex.getMessage()); }

        } catch (Exception e) {
            System.err.println("Application/log error: " + e.getMessage());
        }
        // try-with-resources closes the log once, even if an exception leaves the block.
    }

    private static void runTwist(Clinic clinic, ManualClock clock, Patient p1,
                                 Patient p2, Patient p3, Doctor doctor, Nurse nurse) {
        switch (STUDENT_NUMBER_P % 4) {
            case 0 -> {
                clinic.joinWaitingList(p2, doctor);
                clinic.joinWaitingList(p3, doctor);
                Appointment slot = clinic.book(p1, doctor, 30);
                clock.setNow(20);
                clinic.cancel(slot.getId(), "patient unavailable");
                System.out.println("Waiting list after automatic offer: " + clinic.getWaitingList(doctor).size());
            }
            case 1 -> {
                Appointment pending = clinic.book(p2, nurse, 40);
                p1.setEmergency(true);
                Appointment emergency = clinic.emergencyBook(p1, nurse, 40);
                System.out.println("Pending appointment bumped: " + pending.getCancellationReason());
                System.out.println("Emergency appointment: " + emergency);
            }
            case 2 -> {
                clinic.book(p1, doctor, 50);
                try { clinic.book(p2, doctor, 52); }
                catch (RuntimeException ex) { System.out.println("Expected buffer failure: " + ex.getMessage()); }
                clinic.book(p3, nurse, 60);
                try { clinic.book(p2, nurse, 61); }
                catch (RuntimeException ex) { System.out.println("Expected nurse buffer failure: " + ex.getMessage()); }
            }
            case 3 -> {
                System.out.println("Student pays: $" + p1.amountPayable(doctor.fee()));
                System.out.println("Visitor pays: $" + p2.amountPayable(doctor.fee()));
                System.out.println("Staff member pays: $" + p3.amountPayable(doctor.fee()));
            }
            default -> throw new IllegalStateException("Unexpected twist.");
        }
    }
}
