import { API_BASE_URL } from '../config/config.js';
const PRESCRIPTION_API = `${API_BASE_URL}/prescription`;

async function readResponse(response) {
  const text = await response.text();
  try { return text ? JSON.parse(text) : {}; }
  catch { return { message: text }; }
}

export async function savePrescription(prescription, token) {
  try {
    const response = await fetch(`${PRESCRIPTION_API}/save/${token}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(prescription)
    });
    const result = await readResponse(response);
    return { success: response.ok, message: result.message || result.error || 'Unable to save prescription' };
  } catch (error) {
    console.error('Error saving prescription:', error);
    return { success: false, message: 'Unable to connect. Please try again.' };
  }
}

export async function getPrescription(appointmentId, token) {
  const response = await fetch(`${PRESCRIPTION_API}/${appointmentId}/${token}`);
  const result = await readResponse(response);
  if (!response.ok) throw new Error(result.message || result.error || 'Unable to fetch prescription');
  return result;
}

export async function getPatientPrescription(appointmentId, token) {
  const response = await fetch(`${PRESCRIPTION_API}/patient/${encodeURIComponent(appointmentId)}/${token}`);
  const result = await readResponse(response);
  if (!response.ok) throw new Error(result.message || result.error || 'Unable to fetch prescription');
  return result;
}
