import { savePrescription, getPrescription } from './services/prescriptionServices.js';

document.addEventListener('DOMContentLoaded', async () => {
  if (!requireRole('doctor')) return;
  const form = document.getElementById('prescriptionForm');
  const saveButton = document.getElementById('savePrescription');
  const patientName = document.getElementById('patientName');
  const medication = document.getElementById('medicines');
  const dosage = document.getElementById('dosage');
  const notes = document.getElementById('notes');
  const params = new URLSearchParams(window.location.search);
  const appointmentId = Number(params.get('appointmentId'));
  const token = localStorage.getItem('token');
  if (!Number.isSafeInteger(appointmentId) || appointmentId <= 0) {
    alert('Appointment ID is missing or invalid.');
    window.location.href = `/doctorDashboard/${encodeURIComponent(token)}`;
    return;
  }
  patientName.value = params.get('patientName') || '';
  saveButton.disabled = true;
  let viewOnly = params.get('mode') === 'view';
  try {
    const response = await getPrescription(appointmentId, token);
    const existing = response.prescriptions?.[0];
    if (existing) {
      patientName.value = existing.patientName || 'You';
      medication.value = existing.medication || '';
      dosage.value = existing.dosage || '';
      notes.value = existing.doctorNotes || '';
      viewOnly = true;
    } else if (viewOnly) {
      alert('No prescription has been added for this appointment.');
    }
  } catch (error) {
    alert('Failed to load prescription: ' + error.message);
    return;
  }
  document.getElementById('heading').textContent = viewOnly ? 'View Prescription' : 'Add Prescription';
  if (viewOnly) {
    medication.disabled = dosage.disabled = notes.disabled = true;
    saveButton.style.display = 'none';
  } else {
    saveButton.disabled = false;
  }
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (viewOnly || saveButton.disabled) return;
    if (!medication.value.trim() || !dosage.value.trim()) {
      alert('Please enter medication and dosage instructions.');
      return;
    }
    saveButton.disabled = true;
    const result = await savePrescription({
      patientName: patientName.value,
      medication: medication.value.trim(), dosage: dosage.value.trim(),
      doctorNotes: notes.value.trim(), appointmentId
    }, token);
    if (result.success) {
      alert('Prescription saved successfully.');
      window.location.href = `/doctorDashboard/${encodeURIComponent(token)}`;
    } else {
      alert('Failed to save prescription: ' + result.message);
      saveButton.disabled = false;
    }
  });
});
