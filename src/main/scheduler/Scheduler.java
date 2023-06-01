package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Appointment;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    private static void prompt() {
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();
    }

    public static void main(String[] args) {
        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            prompt();
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the password is a strong password
        if (!isStrongPassword(password)) {
            System.out.println("Not a strong password, please enter a password with:");
            System.out.println("-At least 8 characters");
            System.out.println("-A mixture of both uppercase and lowercase letters");
            System.out.println("-A mixture of letters and numbers");
            System.out.println("-Inclusion of at least one special character, from “!”, “@”, “#”, “?”");
            return;
        }
        // check 3: check if the username has been taken already
        if (usernameExists(username, "Patients")) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save the patient information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the password is a strong password
        if (!isStrongPassword(password)) {
            System.out.println("Not a strong password, please enter a password with:");
            System.out.println("-At least 8 characters");
            System.out.println("-A mixture of both uppercase and lowercase letters");
            System.out.println("-A mixture of letters and numbers");
            System.out.println("-Inclusion of at least one special character, from “!”, “@”, “#”, “?”");
            return;
        }
        // check 3: check if the username has been taken already
        if (usernameExists(username, "Caregivers")) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean isStrongPassword(String password) {
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) { hasLower = true; }
            if (Character.isUpperCase(c)) { hasUpper = true; }
            if (Character.isLetter(c)) { hasLetter = true; }
            if (Character.isDigit(c)) { hasDigit = true; }
            if (c == '!' || c == '@' || c == '#' || c == '?') { hasSpecial = true; }
        }
        return (password.length() >= 8) && hasLower && hasUpper && hasLetter && hasDigit && hasSpecial;
    }

    private static boolean usernameExists(String username, String table) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM " + table + " WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
            return;
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
            return;
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        // search_caregiver_schedule <date>
        // check 1: either a caregiver or patient must be logged in first
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        Date date = Date.valueOf(tokens[1]);
        // Initiate connection to database
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            // Get combo set
            String selectUsernames = "SELECT A.Username, V.Name, V.Doses " +
                                       "FROM Availabilities A, Vaccines V " +
                                      "WHERE A.Time = ? AND V.Doses > 0 " +
                                      "ORDER BY A.Username ASC ";
            PreparedStatement joinStatement = con.prepareStatement(selectUsernames);
            joinStatement.setDate(1, date);
            ResultSet availableCombos = joinStatement.executeQuery();
            // Print combos of caregivers, vaccines and doses
            while (availableCombos.next()) {
                String caregiver = availableCombos.getString("Username");
                String vaccine = availableCombos.getString("Name");
                int doses = availableCombos.getInt("Doses");
                System.out.println(caregiver + " " + vaccine + " " + doses);
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        // reserve <date> <vaccine>
        // check 1: a patient must be logged in first
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        } else if (currentPatient == null) {
            System.out.println("Please login as a patient!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        Date time = Date.valueOf(tokens[1]);
        String vaccine = tokens[2];
        // Initiate connection to database
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            // Get sets
            // Times and caregivers
            String selectUsernames = "SELECT * FROM Availabilities WHERE Time = ? ORDER BY Username ASC";
            PreparedStatement usernameStatement = con.prepareStatement(selectUsernames);
            usernameStatement.setDate(1, time);
            ResultSet usernameSet = usernameStatement.executeQuery();
            // Vaccine doses
            String selectVaccines = "SELECT * FROM Vaccines WHERE Name = ? AND Doses > 0";
            PreparedStatement vaccineStatement = con.prepareStatement(selectVaccines);
            vaccineStatement.setString(1, vaccine);
            ResultSet vaccineSet = vaccineStatement.executeQuery();
            // check 3: make sure there is a caregiver and vaccine available for the appointment
            if (!usernameSet.isBeforeFirst()) {
                System.out.println("No Caregiver is available!");
                return;
            } else if (!vaccineSet.isBeforeFirst()) {
                System.out.println("Not enough available doses!");
                return;
            }
            // Select caregiver
            usernameSet.next();
            String caregiver = usernameSet.getString("Username");
            // Remove availability
            String removeAvailability = "DELETE FROM Availabilities WHERE Username = ? AND TIME = ?";
            PreparedStatement removeStatement = con.prepareStatement(removeAvailability);
            removeStatement.setString(1, caregiver);
            removeStatement.setDate(2, time);
            removeStatement.executeUpdate();
            // Remove dose from vaccine
            Vaccine vaccineDoses = new Vaccine.VaccineGetter(vaccine).get();
            vaccineDoses.decreaseAvailableDoses(1);
            // Create appointment
            Appointment appt = new Appointment.AppointmentBuilder(time, caregiver, vaccine,
                                                                  currentPatient.getUsername()).build();
            // Print information
            System.out.println("Appointment ID: " + appt.getAppointmentID() + ", Caregiver username: " + caregiver);
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        // cancel <appointment_id>
        // check 1: either a caregiver or patient must be logged in first
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        int appointmentID = Integer.parseInt(tokens[1]);
        Appointment appt = null;
        try {
            appt = new Appointment.AppointmentGetter(appointmentID).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when retrieving appointment");
            e.printStackTrace();
            return;
        }
        // check 3: if getter returns null, it means the appointment doesn't exist
        if (appt == null) {
            System.out.println("No appointments with that ID exist");
            return;
        }
        // check 4: make sure the appointment they're canceling is related to them
        if ((currentCaregiver != null && !appt.getCaregiver().equals(currentCaregiver.getUsername()))
            || (currentPatient != null && !appt.getPatient().equals(currentPatient.getUsername()))) {
            System.out.println("You do not have access to cancel this appointment.");
            return;
        }
        // Initiate connection to database
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            // Remove appointment
            String removeAppointment = "Delete FROM Appointments WHERE TIME = ? AND Caregiver = ?";
            PreparedStatement appointmentStatement = con.prepareStatement(removeAppointment);
            appointmentStatement.setDate(1, appt.getTime());
            appointmentStatement.setString(2, appt.getCaregiver());
            appointmentStatement.executeUpdate();
            // Add availability
            String addAvailability = "Insert INTO Availabilities VALUES (?, ?)";
            PreparedStatement addStatement = con.prepareStatement(addAvailability);
            addStatement.setDate(1, appt.getTime());
            addStatement.setString(2, appt.getCaregiver());
            addStatement.executeUpdate();
            // Add dose to vaccine
            Vaccine vaccineDoses = new Vaccine.VaccineGetter(appt.getVaccine()).get();
            vaccineDoses.increaseAvailableDoses(1);
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        System.out.println("Appointment ID: " + appt.getAppointmentID() + ", Caregiver username: " + appt.getCaregiver()
                           + ", Patient username: " + appt.getPatient());
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        // show_appointments
        // check 1: check if user is logged in as either a patient or caregiver
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 1 since extra no info necessary
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        // Initiate connection to database
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        try {
            // Setup up constants to work with both caregivers and patients
            String columnInput = currentCaregiver != null ? "Caregiver" : "Patient";
            String name = currentCaregiver != null ? currentCaregiver.getUsername() : currentPatient.getUsername();
            String columnOutput = currentCaregiver != null ? "Patient" : "Caregiver";
            // Get data
            String getAppts = "SELECT * FROM Appointments WHERE " + columnInput + " = ? ORDER BY Appointment_id ASC";
            PreparedStatement apptStatement = con.prepareStatement(getAppts);
            apptStatement.setString(1, name);
            ResultSet appts = apptStatement.executeQuery();
            // Print appointments
            while(appts.next()) {
                int apptID = appts.getInt("Appointment_id");
                String vaccine = appts.getString("Vaccine");
                String date = appts.getDate("Time").toString();
                String username = appts.getString(columnOutput);
                System.out.println(apptID + " " + vaccine + " " + date + " " + username);
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        // logout
        // check 1: check if user is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }
        // check 2: the length for tokens need to be exactly 1 since extra no info necessary
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        currentCaregiver = null;
        currentPatient = null;
        System.out.println("Successfully logged out!");
    }
}
