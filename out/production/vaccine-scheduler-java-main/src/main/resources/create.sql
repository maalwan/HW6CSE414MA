CREATE TABLE Caregivers (
    Username VARCHAR(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time DATE,
    Username VARCHAR(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name VARCHAR(255),
    Doses INT,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
    Username VARCHAR(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Appointments (
    Time DATE,
    Caregiver VARCHAR(255) REFERENCES Caregivers(Username),
    Vaccine VARCHAR(255) REFERENCES Vaccines(Name),
    Patient VARCHAR(255) REFERENCES Patients(Username),
    Appointment_id INT IDENTITY(1,1),
    PRIMARY KEY (Appointment_id)
);