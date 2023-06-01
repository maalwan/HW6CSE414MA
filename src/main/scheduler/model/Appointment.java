package scheduler.model;

import scheduler.db.ConnectionManager;
import java.sql.*;

public class Appointment {
    private final Date time;
    private final String caregiver;
    private final String vaccine;
    private final String patient;
    private final int appointmentID;

    public Date getTime() {  return time; }

    public String getCaregiver() { return caregiver; }

    public String getVaccine() { return vaccine; }

    public String getPatient() { return patient; }

    public int getAppointmentID() { return appointmentID; }

    private Appointment(AppointmentGetter getter) {
        this.time = getter.time;
        this.caregiver = getter.caregiver;
        this.vaccine = getter.vaccine;
        this.patient = getter.patient;
        this.appointmentID = getter.appointmentID;
    }

    private Appointment(AppointmentBuilder builder) {
        this.time = builder.time;
        this.caregiver = builder.caregiver;
        this.vaccine = builder.vaccine;
        this.patient = builder.patient;
        this.appointmentID = builder.appointmentID;
    }

    public static class AppointmentGetter {
        private Date time;
        private String caregiver;
        private String vaccine;
        private String patient;
        private final int appointmentID;

        public AppointmentGetter(int appointmentID) {
            this.appointmentID = appointmentID;
        }

        public Appointment get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            try {
                // Get appointment
                String getAppt = "SELECT * FROM Appointments WHERE Appointment_id = ?";
                PreparedStatement apptStatement = con.prepareStatement(getAppt);
                apptStatement.setInt(1, appointmentID);
                ResultSet appts = apptStatement.executeQuery();
                if (appts.isBeforeFirst()) {
                    appts.next();
                    this.time = appts.getDate("Time");
                    this.caregiver = appts.getString("Caregiver");
                    this.vaccine = appts.getString("Vaccine");
                    this.patient = appts.getString("Patient");
                    return new Appointment(this);
                } else {
                    return null;
                }
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }

    public static class AppointmentBuilder {
        private final Date time;
        private final String caregiver;
        private final String vaccine;
        private final String patient;
        private int appointmentID;

        public AppointmentBuilder(Date time, String caregiver, String vaccine, String patient) {
            this.time = time;
            this.caregiver = caregiver;
            this.vaccine = vaccine;
            this.patient = patient;
        }

        public Appointment build() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            try {
                // Add appointment
                String insertAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?)";
                PreparedStatement appointmentStatement = con.prepareStatement(insertAppointment);
                appointmentStatement.setDate(1, time);
                appointmentStatement.setString(2, caregiver);
                appointmentStatement.setString(3, vaccine);
                appointmentStatement.setString(4, patient);
                appointmentStatement.executeUpdate();
                // Get appointment ID
                String getID = "SELECT Appointment_id FROM Appointments WHERE Time = ? AND Caregiver = ?";
                PreparedStatement IDStatement = con.prepareStatement(getID);
                IDStatement.setDate(1, time);
                IDStatement.setString(2, caregiver);
                ResultSet ID = IDStatement.executeQuery();
                ID.next();
                this.appointmentID = ID.getInt("Appointment_id");
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
            return new Appointment(this);
        }
    }
}
