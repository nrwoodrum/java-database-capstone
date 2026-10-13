import { getDoctors, filterDoctors, saveDoctor } from './services/doctorServices.js';
import { createDoctorCard } from './components/doctorCard.js';
import { openModal } from './components/modals.js';

function setupAdminDashboard() {
  const addDoctorButton = document.getElementById('addDoctorBtn');
  if (addDoctorButton) {
    addDoctorButton.addEventListener('click', () => openModal('addDoctor'));
  }

  const searchBar = document.getElementById('searchBar');
  const timeFilter = document.getElementById('timeFilter');
  const specialtyFilter = document.getElementById('specialtyFilter');

  if (searchBar) searchBar.addEventListener('input', filterDoctorsOnChange);
  if (timeFilter) timeFilter.addEventListener('change', filterDoctorsOnChange);
  if (specialtyFilter) specialtyFilter.addEventListener('change', filterDoctorsOnChange);

  renderSummaryCards([]);
  loadDoctorCards();
}

function renderSummaryCards(doctors) {
  const summaryContainer = document.getElementById('summaryCards');
  if (!summaryContainer) return;

  const totalDoctors = Array.isArray(doctors) ? doctors.length : 0;
  const specialties = new Set(
    (Array.isArray(doctors) ? doctors : [])
      .map(doctor => doctor?.specialty)
      .filter(Boolean)
  );

  summaryContainer.innerHTML = `
    <div class="summary-card">
      <span>Total Doctors</span>
      <strong>${totalDoctors}</strong>
    </div>
    <div class="summary-card">
      <span>Specialties</span>
      <strong>${specialties.size}</strong>
    </div>
    <div class="summary-card">
      <span>Search Results</span>
      <strong>${totalDoctors}</strong>
    </div>
  `;
}

async function loadDoctorCards() {
  const contentDiv = document.getElementById('content');
  if (!contentDiv) return;

  contentDiv.innerHTML = '<div class="empty-state">Loading doctors…</div>';

  try {
    const doctors = await getDoctors();
    renderSummaryCards(doctors);
    contentDiv.innerHTML = '';
    doctors.forEach(doctor => {
      const card = createDoctorCard(doctor);
      contentDiv.appendChild(card);
    });

    if (!doctors.length) {
      contentDiv.innerHTML = '<div class="empty-state">No doctors are available right now.</div>';
    }
  } catch (error) {
    console.error('Error loading doctor cards:', error);
    contentDiv.innerHTML = '<div class="empty-state">Unable to load doctors. Please try again.</div>';
  }
}

async function filterDoctorsOnChange() {
  const name = document.getElementById('searchBar')?.value || null;
  const time = document.getElementById('timeFilter')?.value || null;
  const specialty = document.getElementById('specialtyFilter')?.value || null;

  try {
    const filteredDoctors = await filterDoctors(name, time, specialty);
    const doctors = Array.isArray(filteredDoctors) ? filteredDoctors : filteredDoctors?.doctors || [];

    renderSummaryCards(doctors);

    if (doctors.length > 0) {
      renderDoctorCards(doctors);
    } else {
      const content = document.getElementById('content');
      if (content) {
        content.innerHTML = '<div class="empty-state">No doctors found with the given filters.</div>';
      }
    }
  } catch (error) {
    console.error('Error filtering doctors:', error);
    const content = document.getElementById('content');
    if (content) {
      content.innerHTML = '<div class="empty-state">Unable to filter doctors. Please try again.</div>';
    }
  }
}

function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById('content');
  if (!contentDiv) return;

  contentDiv.innerHTML = '';
  doctors.forEach(doctor => {
    const card = createDoctorCard(doctor);
    contentDiv.appendChild(card);
  });
}

async function adminAddDoctor() {
  const name = document.getElementById('doctorName')?.value || '';
  const email = document.getElementById('doctorEmail')?.value || '';
  const phone = document.getElementById('doctorPhone')?.value || '';
  const password = document.getElementById('doctorPassword')?.value || '';
  const specialty = document.getElementById('specialization')?.value || '';
  const availableTimes = Array.from(document.querySelectorAll('input[name="availability"]:checked')).map(input => input.value);

  const token = localStorage.getItem('token');
  if (!token) {
    alert('Admin token not found. Please log in again.');
    return;
  }

  const doctor = { name, email, phone, password, specialty, availableTimes };

  try {
    const result = await saveDoctor(doctor, token);
    if (result.success) {
      alert('Doctor added successfully.');
      const modal = document.getElementById('modal');
      if (modal) modal.style.display = 'none';
      loadDoctorCards();
    } else {
      alert('Failed to add doctor: ' + result.message);
    }
  } catch (error) {
    console.error('Error adding doctor:', error);
    alert('An error occurred while adding the doctor. Please try again.');
  }
}

window.adminAddDoctor = adminAddDoctor;

document.addEventListener('DOMContentLoaded', () => {
  if (requireRole('admin')) {
    setupAdminDashboard();
  }
});

/*
  This script handles the admin dashboard functionality for managing doctors:
  - Loads all doctor cards
  - Filters doctors by name, time, or specialty
  - Adds a new doctor via modal form


  Attach a click listener to the "Add Doctor" button
  When clicked, it opens a modal form using openModal('addDoctor')


  When the DOM is fully loaded:
    - Call loadDoctorCards() to fetch and display all doctors


  Function: loadDoctorCards
  Purpose: Fetch all doctors and display them as cards

    Call getDoctors() from the service layer
    Clear the current content area
    For each doctor returned:
    - Create a doctor card using createDoctorCard()
    - Append it to the content div

    Handle any fetch errors by logging them


  Attach 'input' and 'change' event listeners to the search bar and filter dropdowns
  On any input change, call filterDoctorsOnChange()


  Function: filterDoctorsOnChange
  Purpose: Filter doctors based on name, available time, and specialty

    Read values from the search bar and filters
    Normalize empty values to null
    Call filterDoctors(name, time, specialty) from the service

    If doctors are found:
    - Render them using createDoctorCard()
    If no doctors match the filter:
    - Show a message: "No doctors found with the given filters."

    Catch and display any errors with an alert


  Function: renderDoctorCards
  Purpose: A helper function to render a list of doctors passed to it

    Clear the content area
    Loop through the doctors and append each card to the content area


  Function: adminAddDoctor
  Purpose: Collect form data and add a new doctor to the system

    Collect input values from the modal form
    - Includes name, email, phone, password, specialty, and available times

    Retrieve the authentication token from localStorage
    - If no token is found, show an alert and stop execution

    Build a doctor object with the form values

    Call saveDoctor(doctor, token) from the service

    If save is successful:
    - Show a success message
    - Close the modal and reload the page

    If saving fails, show an error message
*/
