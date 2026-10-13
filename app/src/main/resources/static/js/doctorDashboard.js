import { getAllAppointments } from './services/appointmentRecordService.js';

const tableBody = document.getElementById('patientTableBody');
const searchBar = document.getElementById('searchBar');
const datePicker = document.getElementById('datePicker');
const todayButton = document.getElementById('todayButton');

let selectedDate = new Date().toISOString().split('T')[0];
let patientName = null;
const token = localStorage.getItem('token');

function renderDoctorSummary(appointments) {
  const summaryContainer = document.getElementById('doctorSummaryCards');
  if (!summaryContainer) return;

  const rows = Array.isArray(appointments) ? appointments : [];
  const total = rows.length;
  const completed = rows.filter(appointment => {
    const status = Number(appointment?.status ?? appointment?.appointmentStatus ?? 0);
    return status === 1 || status === 2 || appointment?.status === 'Completed';
  }).length;
  const pending = total - completed;

  summaryContainer.innerHTML = `
    <div class="summary-card">
      <span>Total</span>
      <strong>${total}</strong>
    </div>
    <div class="summary-card">
      <span>Completed</span>
      <strong>${completed}</strong>
    </div>
    <div class="summary-card">
      <span>Pending</span>
      <strong>${pending}</strong>
    </div>
  `;
}

function normalizePatientName(value) {
  if (!value || value === 'null' || value === 'all') {
    return null;
  }
  return value.trim();
}

function getDoctorIdFromToken() {
  try {
    const rawToken = token?.split('.')[1];
    if (!rawToken) return null;
    const payload = JSON.parse(atob(rawToken.replace(/-/g, '+').replace(/_/g, '/')));
    return payload.userId || payload.userID || null;
  } catch (error) {
    console.error('Failed to decode doctor token:', error);
    return null;
  }
}

function statusBadge(status) {
  const normalized = Number(status ?? 0);
  const label = normalized === 1 || normalized === 2 || status === 'Completed' ? 'Completed' : 'Pending';
  const className = label === 'Completed' ? 'status-badge completed' : 'status-badge pending';
  return `<span class="${className}">${label}</span>`;
}

function buildPatientRow(appointment) {
  const patient = appointment.patient || {};
  const row = document.createElement('tr');

  row.innerHTML = `
    <td>${patient.id ?? appointment.patientId ?? ''}</td>
    <td>${patient.name ?? appointment.patientName ?? ''}</td>
    <td>${patient.phone ?? appointment.patientPhone ?? ''}</td>
    <td>${patient.email ?? appointment.patientEmail ?? ''}</td>
    <td>${statusBadge(appointment.status)}</td>
  `;

  return row;
}

async function loadAppointments() {
  if (!tableBody) return;

  const doctorId = getDoctorIdFromToken();
  if (!doctorId || !token) {
    renderDoctorSummary([]);
    tableBody.innerHTML = '<tr><td colspan="5" class="empty-state">Doctor session is invalid. Please log in again.</td></tr>';
    return;
  }

  try {
    tableBody.innerHTML = '<tr><td colspan="5" class="empty-state">Loading appointments…</td></tr>';
    const appointments = await getAllAppointments(doctorId, selectedDate, patientName, token);
    renderDoctorSummary(appointments);
    tableBody.innerHTML = '';

    if (!appointments || appointments.length === 0) {
      tableBody.innerHTML = '<tr><td colspan="5" class="empty-state">No appointments found for this date.</td></tr>';
      return;
    }

    appointments.forEach(appointment => {
      tableBody.appendChild(buildPatientRow(appointment));
    });
  } catch (error) {
    console.error('Error loading appointments:', error);
    renderDoctorSummary([]);
    tableBody.innerHTML = '<tr><td colspan="5" class="empty-state">Error loading appointments. Try again later.</td></tr>';
  }
}

if (searchBar) {
  searchBar.addEventListener('input', (event) => {
    patientName = normalizePatientName(event.target.value);
    loadAppointments();
  });
}

if (todayButton) {
  todayButton.addEventListener('click', () => {
    selectedDate = new Date().toISOString().split('T')[0];
    if (datePicker) datePicker.value = selectedDate;
    loadAppointments();
  });
}

if (datePicker) {
  datePicker.addEventListener('change', (event) => {
    selectedDate = event.target.value;
    loadAppointments();
  });
}

document.addEventListener('DOMContentLoaded', () => {
  if (datePicker) datePicker.value = selectedDate;
  loadAppointments();
});

/*
  Import getAllAppointments to fetch appointments from the backend
  Import createPatientRow to generate a table row for each patient appointment


  Get the table body where patient rows will be added
  Initialize selectedDate with today's date in 'YYYY-MM-DD' format
  Get the saved token from localStorage (used for authenticated API calls)
  Initialize patientName to null (used for filtering by name)


  Add an 'input' event listener to the search bar
  On each keystroke:
    - Trim and check the input value
    - If not empty, use it as the patientName for filtering
    - Else, reset patientName to "null" (as expected by backend)
    - Reload the appointments list with the updated filter


  Add a click listener to the "Today" button
  When clicked:
    - Set selectedDate to today's date
    - Update the date picker UI to match
    - Reload the appointments for today


  Add a change event listener to the date picker
  When the date changes:
    - Update selectedDate with the new value
    - Reload the appointments for that specific date


  Function: loadAppointments
  Purpose: Fetch and display appointments based on selected date and optional patient name

  Step 1: Call getAllAppointments with selectedDate, patientName, and token
  Step 2: Clear the table body content before rendering new rows

  Step 3: If no appointments are returned:
    - Display a message row: "No Appointments found for today."

  Step 4: If appointments exist:
    - Loop through each appointment and construct a 'patient' object with id, name, phone, and email
    - Call createPatientRow to generate a table row for the appointment
    - Append each row to the table body

  Step 5: Catch and handle any errors during fetch:
    - Show a message row: "Error loading appointments. Try again later."


  When the page is fully loaded (DOMContentLoaded):
    - Call renderContent() (assumes it sets up the UI layout)
    - Call loadAppointments() to display today's appointments by default
*/
