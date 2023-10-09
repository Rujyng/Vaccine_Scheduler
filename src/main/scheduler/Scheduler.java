package scheduler;

import scheduler.db.ConnectionManager;
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

// Scheduler handles the state of either a caregiver or a patient where each user can perform
// certain commands that they are assigned to. It also manages interactions between caregivers and patients
// where they can either make appointments with each other or cancel an existing appointment.
public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");
        System.out.println("> reserve <date> <vaccine>");
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");
        System.out.println("> logout");
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
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

    // Creates a new patient account and record it in the system
    // Parameters:
    //      String[] tokens - should contain ["create_patient", <username>, <password>]
    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    // Checks if the patient username has already existed in the system
    // Parameters:
    //      String username - the input username
    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
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

    // Creates a new caregiver account and record it in the system
    // Parameters:
    //      String[] tokens - should contain ["create_caregiver", <username>, <password>]
    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    // Checks if the caregiver username has already existed in the system
    // Parameters:
    //      String username - the input username
    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
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

    // log a patient in based on their input for username and password
    // Parameters:
    //      String[] tokens - should contains ["login_patient" <username> <password>]
    private static void loginPatient(String[] tokens) {
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
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    // log a caregiver in based on their input for username and password
    // Parameters:
    //      String[] tokens - should contains ["login_caregiver" <username> <password>]
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
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    // Prints out a list of available caregivers and available vaccine doses
    // on a specified day
    // Parameters:
    //      String[] - should contain ["search_caregiver_schedule", <date>]
    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        } // check for login

        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        } // check for valid input
        String date = tokens[1];

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectSchedule = "SELECT a.Username, v.Name, v.Doses \n" +
                "FROM [dbo].[Availabilities] a, [dbo].[Vaccines] v\n" +
                "WHERE a.Available = 1\n" +
                "AND a.Time = ?\n" +
                "ORDER BY a.Username;"; // select all the caregivers available for the day

        try {
            Date d = Date.valueOf(date);
            PreparedStatement statement = con.prepareStatement(selectSchedule);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery(); // select all the caregivers available for the day
            while (resultSet.next()) {
                System.out.println(resultSet.getString("Username") +
                             " " + resultSet.getString("Name") +
                             " " + resultSet.getString("Doses")); // print the list out
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    // Makes an appointment for the patient, caregiver cannot perform this
    // This will make the caregiver unavailable for the specified day and number of
    // specified vaccine doses decrease by 1
    // Parameters:
    //      String[] tokens - should contain ["reserve", <date>, <vaccine>]
    private static void reserve(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        } else if (currentCaregiver != null && currentPatient == null) {
            System.out.println("Please login as a patient");
            return;
        } // check patient login

        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        } // check for valid input

        String date = tokens[1];
        String vaccine = tokens[2];

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectCaregiver = "SELECT TOP 1 a.Username\n" +
                "FROM [dbo].[Availabilities] a\n" +
                "WHERE a.Time = ?\n" +
                "\tAND a.Available = 1\n" +
                "ORDER BY a.Username"; // get the caregiver available for the day (first one ordered by alphabet)

        String checkDoses = "SELECT *\n" +
                "FROM [dbo].[Vaccines]\n" +
                "WHERE Name = ?;"; // get the vaccine name and dose count

        String makeAppointment = "INSERT [dbo].[Appointments] (Time, PatientName, CaregiverName, VaccineName)\n" +
                "VALUES (?, ?, ?, ?);"; // add the appointment to the system

        String markUnavailable = "UPDATE [dbo].[Availabilities]\n" +
                "SET Available = 0\n" +
                "WHERE Time = ?" +
                "\tAND Username = ?;"; // make the selected caregiver unavailable for the day

        String updateVaccine = "UPDATE Vaccines\n" +
                "SET Doses = Doses - 1\n" +
                "WHERE Name = ?;"; // update the number of doses for the vaccine (-1)

        String getID = "SELECT id\n" +
                "FROM [dbo].[Appointments]\n" +
                "WHERE time = ?\n" +
                "\tAND CaregiverName = ?;"; // get the appointment ID that was just created

        try {
            Date d = Date.valueOf(date);
            PreparedStatement statement1 = con.prepareStatement(selectCaregiver);
            PreparedStatement statement2 = con.prepareStatement(checkDoses);
            PreparedStatement statement3 = con.prepareStatement(makeAppointment);
            PreparedStatement statement4 = con.prepareStatement(markUnavailable);
            PreparedStatement statement5 = con.prepareStatement(updateVaccine);
            PreparedStatement statement6 = con.prepareStatement(getID);

            statement1.setDate(1, d);
            statement2.setString(1, vaccine);

            ResultSet resultSet1 = statement1.executeQuery(); // get the caregiver available for the day (first one
                                                              // ordered by alphabet)
            ResultSet resultSet2 = statement2.executeQuery(); // get the vaccine and dose count for the vaccine

            String currCaregiver = null;
            while (resultSet1.next()) {
                currCaregiver = resultSet1.getString("Username"); // record the selected caregiver
            }
            String doseName = null;
            int dosesCount = 0;
            while (resultSet2.next()) {
                doseName = resultSet2.getString("Name"); // record the vaccine name
                dosesCount = resultSet2.getInt("Doses"); // record the number of doses
            }

            if (currCaregiver != null) { // check if caregiver is available/exist
                if (doseName != null) { // check if the input vaccine exists in the system
                    if (dosesCount > 0) { // check if there is still vaccine left
                        statement3.setDate(1, d);
                        statement3.setString(2, currentPatient.getUsername());
                        statement3.setString(3, currCaregiver);
                        statement3.setString(4, vaccine);
                        statement4.setDate(1, d);
                        statement4.setString(2, currCaregiver);
                        statement5.setString(1, vaccine);
                        statement6.setDate(1, d);
                        statement6.setString(2, currCaregiver);

                        statement3.execute(); // add the appointment to the system
                        statement4.execute(); // make the selected caregiver unavailable for the day
                        statement5.execute(); // update the number of doses for the vaccine (-1)
                        ResultSet resultSet6 = statement6.executeQuery(); // get the appointment ID that was just created

                        String id = null;
                        while (resultSet6.next()) {
                            id = resultSet6.getString("id");
                        }

                        System.out.println("Appointment ID: " + id + ", Caregiver username: " + currCaregiver);
                    } else {
                        System.out.println("Not enough available doses!");
                    }
                } else {
                    System.out.println("No matching vaccine based on your input!");
                }
            } else {
                System.out.println("No Caregiver is available!");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
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

    // Removes the appointment from the system which can be performed by the caregiver/patient
    // The appointment can only be removed if that appointment is of the current caregiver/patient
    // Update the system accordingly after the removal
    // Parameters:
    //      String[] tokens: should contain ["cancel", <Appointment ID>]
    private static void cancel(String[] tokens) {
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        } // check for valid input

        String appointmentID = tokens[1];
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getAppointment = "SELECT *\n" +
                "FROM [dbo].[Appointments]\n" +
                "WHERE id = ?\n" +
                "\tAND (PatientName = ? OR CaregiverName = ?);"; // get the appointment associated to the id but only
                                                                 // if the appointment matches the current
                                                                 // caregiver/patient

        String removeAppointment = "DELETE FROM [dbo].[Appointments]\n" +
                "WHERE id = ?;"; // remove the appointment from the system

        String updateAvailability = "UPDATE [dbo].[Availabilities]\n" +
                "SET Available = 1\n" +
                "WHERE Time = ?\n" +
                "\tAND Username = ?;"; // make the caregiver available again after cancel

        String updateVaccine = "UPDATE [dbo].[Vaccines]\n" +
                "SET Doses = Doses + 1\n" +
                "WHERE Name = ?;"; // update the number of vaccine doses after cancel (+1)

        String patientUsername = null;
        String caregiverUsername = null;

        if (currentPatient != null) {
            patientUsername = currentPatient.getUsername();
        }
        if (currentCaregiver != null) {
            caregiverUsername = currentCaregiver.getUsername();
        } // store the username because can't call getUsername() when object is null

        try {
            PreparedStatement statement = con.prepareStatement(getAppointment);
            statement.setString(1, appointmentID);
            statement.setString(2, patientUsername);
            statement.setString(3, caregiverUsername);

            ResultSet resultSet = statement.executeQuery();

            String time = null;
            String caregiver = null;
            String vaccine = null;
            while (resultSet.next()) {
                time = resultSet.getString("Time");
                caregiver = resultSet.getString("CaregiverName");
                vaccine = resultSet.getString("VaccineName");
            } // get the appointment associated to the id first
            if (time != null && caregiver != null) { // check if the searched appointment is found in the system
                PreparedStatement statement2 = con.prepareStatement(removeAppointment);
                PreparedStatement statement3 = con.prepareStatement(updateAvailability);
                PreparedStatement statement4 = con.prepareStatement(updateVaccine);
                statement2.setString(1, appointmentID);
                statement3.setString(1, time);
                statement3.setString(2, caregiver);
                statement4.setString(1, vaccine);

                statement2.execute(); // remove the appointment from the system
                statement3.execute(); // make the caregiver available again after cancel
                statement4.execute(); // update the number of vaccine doses after cancel (+1)

                System.out.println("Appointment canceled succesfully!");
            } else {
                System.out.println("No matching appointment based on your input");
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    // Update/Add vaccine doses to the system which can only performed by the caregivers
    // Parameters:
    //      String[] tokens - should contain ["add_doses", <vaccine>, <number>]
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

    // Shows all the appointments of the current caregiver/patient which will include
    // the appointment id, the vaccine name, the time of the appointment and the caregiver/patient name
    // Parameter:
    //      String[] tokens - should contain ["show_appointments"]
    private static void showAppointments(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        } // check for login
        if (tokens.length > 1) {
            System.out.println("Please try again!");
            return;
        } // check valid input

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        if (currentPatient == null && currentCaregiver != null) { // caregiver
            String getAppointment = "SELECT *\n" +
                    "FROM [dbo].[Appointments]\n" +
                    "WHERE CaregiverName = ?\n" +
                    "ORDER BY id;"; // get all the appointments that has this caregiver's name
            try {
                PreparedStatement statement = con.prepareStatement(getAppointment);
                statement.setString(1, currentCaregiver.getUsername());
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    System.out.println(resultSet.getString("id") +
                            " " + resultSet.getString("VaccineName") +
                            " " + resultSet.getString("Time") +
                            " " + resultSet.getString("PatientName")); // print it out
                }
            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        } else if (currentPatient != null && currentCaregiver == null) { // patient
            String getAppointment = "SELECT *\n" +
                    "FROM [dbo].[Appointments]\n" +
                    "WHERE Patientname = ?\n" +
                    "ORDER BY id;"; // get all the appointments that has this patient's name
            try {
                PreparedStatement statement = con.prepareStatement(getAppointment);
                statement.setString(1, currentPatient.getUsername());
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    System.out.println(resultSet.getString("id") +
                            " " + resultSet.getString("VaccineName") +
                            " " + resultSet.getString("Time") +
                            " " + resultSet.getString("CaregiverName")); // print it out
                }
            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        }
    }

    // Logouts of the current user
    // Parameters:
    //      String[] tokens - Should only contain ["logout"]
    private static void logout(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        currentPatient = null;
        currentCaregiver = null;
        System.out.println("Succesfully logged out!");
    }
}
