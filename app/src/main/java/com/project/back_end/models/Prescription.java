package com.project.back_end.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "prescriptions")
public class Prescription {

  // 1. Unique identifier for the prescription.
  // MongoDB uses this field as the document's primary key.
  @Id
  private String id;

  // 2. Name of the patient receiving the prescription.
  // Required and must contain between 3 and 100 characters.
  @NotNull(message = "Patient name cannot be null")
  @Size(min = 3, max = 100, message = "Patient name must be between 3 and 100 characters")
  private String patientName;

  // 3. ID of the appointment associated with the prescription.
  // Required when creating a prescription.
  @NotNull(message = "Appointment ID cannot be null")
  private Long appointmentId;

  // 4. Medication prescribed to the patient.
  // Required and must contain between 3 and 100 characters.
  @NotNull(message = "Medication cannot be null")
  @Size(min = 3, max = 100, message = "Medication must be between 3 and 100 characters")
  private String medication;

  // 5. Dosage information for the medication.
  // Required when creating a prescription.
  @NotNull(message = "Dosage cannot be null")
  private String dosage;

  // 6. Additional notes or instructions from the doctor.
  // Cannot exceed 200 characters.
  @Size(max = 200, message = "Doctor notes cannot exceed 200 characters")
  private String doctorNotes;

  // 7. No-argument constructor.
  public Prescription() {
  }

  // Parameterized constructor.
  public Prescription(
      String patientName,
      String medication,
      String dosage,
      String doctorNotes,
      Long appointmentId) {
    this.patientName = patientName;
    this.medication = medication;
    this.dosage = dosage;
    this.doctorNotes = doctorNotes;
    this.appointmentId = appointmentId;
  }

  // 8. Getters and setters.

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPatientName() {
    return patientName;
  }

  public void setPatientName(String patientName) {
    this.patientName = patientName;
  }

  public Long getAppointmentId() {
    return appointmentId;
  }

  public void setAppointmentId(Long appointmentId) {
    this.appointmentId = appointmentId;
  }

  public String getMedication() {
    return medication;
  }

  public void setMedication(String medication) {
    this.medication = medication;
  }

  public String getDosage() {
    return dosage;
  }

  public void setDosage(String dosage) {
    this.dosage = dosage;
  }

  public String getDoctorNotes() {
    return doctorNotes;
  }

  public void setDoctorNotes(String doctorNotes) {
    this.doctorNotes = doctorNotes;
  }
}