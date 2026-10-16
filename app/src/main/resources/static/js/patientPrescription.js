import { getPatientPrescription } from './services/prescriptionServices.js';

document.addEventListener('DOMContentLoaded', async () => {
  if (!requireRole('loggedPatient')) return;
  const status = document.getElementById('prescriptionStatus');
  const details = document.getElementById('prescriptionDetails');
  const appointmentId = Number(new URLSearchParams(window.location.search).get('appointmentId'));
  if (!Number.isSafeInteger(appointmentId) || appointmentId <= 0) {
    status.textContent = 'Appointment ID is missing or invalid. Please return to My Appointments.';
    return;
  }
  try {
    const response = await getPatientPrescription(appointmentId, localStorage.getItem('token'));
    const prescription = response.prescriptions?.[0];
    if (!prescription) {
      status.textContent = 'No prescription has been added for this appointment yet.';
      return;
    }
    for (const field of ['patientName', 'medication', 'dosage', 'doctorNotes']) {
      document.getElementById(field).textContent = prescription[field] || (field === 'doctorNotes' ? 'No additional notes.' : 'Not provided');
    }
    details.hidden = false;
    status.textContent = '';
  } catch (error) {
    status.textContent = 'Unable to load your prescription: ' + error.message;
  }
});
