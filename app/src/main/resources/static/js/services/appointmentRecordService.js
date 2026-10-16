// appointmentRecordService.js
import { API_BASE_URL } from "../config/config.js";
const APPOINTMENT_API = `${API_BASE_URL}/appointments`;

async function readAppointmentResponse(response) {
  const text = await response.text();
  let message = text;
  try {
    const data = text ? JSON.parse(text) : null;
    message = typeof data === "string" ? data : data?.message || data?.error || text;
  } catch {
    // The appointment endpoints return plain text for success and failure.
  }

  return {
    success: response.ok,
    message: message || (response.ok ? "Appointment saved successfully" : "Failed to save appointment")
  };
}

// This is for the doctor to fetch appointments for a specific doctor on a date.
export async function getAllAppointments(doctorId, date, patientName, token) {
  const safePatientName = patientName && patientName !== 'null' ? encodeURIComponent(patientName) : 'all';
  const response = await fetch(`${APPOINTMENT_API}/${doctorId}/${date}/${safePatientName}/${token}`);

  if (!response.ok) {
    throw new Error(`Failed to fetch appointments: ${response.status}`);
  }

  return await response.json();
}

export async function bookAppointment(appointment, token) {
  try {
    const response = await fetch(`${APPOINTMENT_API}/${token}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(appointment)
    });

    return await readAppointmentResponse(response);
  } catch (error) {
    console.error("Error while booking appointment:", error);
    return {
      success: false,
      message: "Network error. Please try again later."
    };
  }
}

export async function updateAppointment(appointment, token) {
  if (!appointment?.id) {
    return { success: false, message: "Appointment ID is missing. Please reopen the appointment from My Appointments." };
  }

  try {
    const response = await fetch(`${APPOINTMENT_API}/${encodeURIComponent(appointment.id)}/${token}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(appointment)
    });

    return await readAppointmentResponse(response);
  } catch (error) {
    console.error("Error while updating appointment:", error);
    return {
      success: false,
      message: "Network error. Please try again later."
    };
  }
}
