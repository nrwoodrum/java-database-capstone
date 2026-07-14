package com.project.back_end.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
public class Appointment {

  // 1. Unique identifier and primary key.
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 2. Many appointments can belong to one doctor.
  @ManyToOne
  @NotNull(message = "Doctor cannot be null")
  private Doctor doctor;

  // 3. Many appointments can belong to one patient.
  @ManyToOne
  @NotNull(message = "Patient cannot be null")
  private Patient patient;

  // 4. Date and time when the appointment is scheduled.
  @NotNull(message = "Appointment time cannot be null")
  @Future(message = "Appointment time must be in the future")
  private LocalDateTime appointmentTime;

  // 5. Appointment status:
  // 0 = scheduled
  // 1 = completed
  @Min(value = 0, message = "Appointment status must be 0 or 1")
  @Max(value = 1, message = "Appointment status must be 0 or 1")
  private int status;

  // 9. No-argument constructor required by JPA.
  public Appointment() {
  }

  // Parameterized constructor.
  public Appointment(
      Doctor doctor,
      Patient patient,
      LocalDateTime appointmentTime,
      int status) {
    this.doctor = doctor;
    this.patient = patient;
    this.appointmentTime = appointmentTime;
    this.status = status;
  }

  // 6. Returns the estimated end time, one hour after the start time.
  // This value is calculated and is not stored in the database.
  @Transient
  public LocalDateTime getEndTime() {
    if (appointmentTime == null) {
      return null;
    }

    return appointmentTime.plusHours(1);
  }

  // 7. Returns only the date portion of the appointment time.
  @Transient
  public LocalDate getAppointmentDate() {
    if (appointmentTime == null) {
      return null;
    }

    return appointmentTime.toLocalDate();
  }

  // 8. Returns only the time portion of the appointment time.
  @Transient
  public LocalTime getAppointmentTimeOnly() {
    if (appointmentTime == null) {
      return null;
    }

    return appointmentTime.toLocalTime();
  }

  // 10. Getters and setters.

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Doctor getDoctor() {
    return doctor;
  }

  public void setDoctor(Doctor doctor) {
    this.doctor = doctor;
  }

  public Patient getPatient() {
    return patient;
  }

  public void setPatient(Patient patient) {
    this.patient = patient;
  }

  public LocalDateTime getAppointmentTime() {
    return appointmentTime;
  }

  public void setAppointmentTime(LocalDateTime appointmentTime) {
    this.appointmentTime = appointmentTime;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }
}