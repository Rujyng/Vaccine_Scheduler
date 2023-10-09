CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    Available bit,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Appointments (
    id int IDENTITY(1,1),
    Time date,
    PatientName varchar(255) REFERENCES Patients,
    CaregiverName varchar(255) REFERENCES Caregivers,
    VaccineName varchar(255) REFERENCES Vaccines,
    PRIMARY KEY (Time, CaregiverName)
);