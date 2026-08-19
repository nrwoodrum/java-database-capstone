CREATE TABLE IF NOT EXISTS admin (
    id BIGINT NOT NULL AUTO_INCREMENT,
    password VARCHAR(255),
    username VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS appointment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_time DATETIME(6),
    status INTEGER NOT NULL,
    doctor_id BIGINT,
    patient_id BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS doctor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255),
    name VARCHAR(255),
    password VARCHAR(255),
    phone VARCHAR(255),
    specialty VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS doctor_available_times (
    doctor_id BIGINT NOT NULL,
    available_times VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS patient (
    id BIGINT NOT NULL AUTO_INCREMENT,
    address VARCHAR(255),
    email VARCHAR(255),
    name VARCHAR(255),
    password VARCHAR(255),
    phone VARCHAR(255),
    PRIMARY KEY (id)
);

ALTER TABLE appointment
    ADD CONSTRAINT FK_APPOINTMENT_DOCTOR FOREIGN KEY (doctor_id) REFERENCES doctor (id);

ALTER TABLE appointment
    ADD CONSTRAINT FK_APPOINTMENT_PATIENT FOREIGN KEY (patient_id) REFERENCES patient (id);

ALTER TABLE doctor_available_times
    ADD CONSTRAINT FK_DOCTOR_AVAILABLE_TIMES_DOCTOR FOREIGN KEY (doctor_id) REFERENCES doctor (id);
