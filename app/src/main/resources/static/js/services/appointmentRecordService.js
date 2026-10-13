// appointmentRecordService.js
import { API_BASE_URL } from "../config/config.js";
const APPOINTMENT_API = `${API_BASE_URL}/appointments`;

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

    const data = await response.json();
    return {
      success: response.ok,
      message: data.message || "Something went wrong"
    };
  } catch (error) {
    console.error("Error while booking appointment:", error);
    return {
      success: false,
      message: "Network error. Please try again later."
    };
  }
}

export async function updateAppointment(appointment, token) {
  try {
    const response = await fetch(`${APPOINTMENT_API}/${token}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(appointment)
    });

    const data = await response.json();
    return {
      success: response.ok,
      message: data.message || "Something went wrong"
    };
  } catch (error) {
    console.error("Error while booking appointment:", error);
    return {
      success: false,
      message: "Network error. Please try again later."
    };
  }
}
