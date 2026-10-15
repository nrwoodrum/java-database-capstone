// patientAppointment.js
import { getPatientAppointments, getPatientData } from "./services/patientServices.js";

let tableBody = null;
const token = localStorage.getItem("token");

let allAppointments = [];
let patientId = null;

document.addEventListener("DOMContentLoaded", initializePage);

async function initializePage() {
  tableBody = document.getElementById("patientTableBody");
  const searchBar = document.getElementById("searchBar");
  const appointmentFilter = document.getElementById("appointmentFilter");

  if (searchBar) searchBar.addEventListener("input", handleFilterChange);
  if (appointmentFilter) appointmentFilter.addEventListener("change", handleFilterChange);

  try {
    if (!token) throw new Error("No token found");

    const patient = await getPatientData(token);
    if (!patient) throw new Error("Failed to fetch patient details");

    patientId = Number(patient.id);

    const appointmentData = await getPatientAppointments(patientId, token, "patient") || [];
    allAppointments = appointmentData.filter(app => app.patientId === patientId);

    handleFilterChange();
  } catch (error) {
    console.error("Error loading appointments:", error);
    alert("❌ Failed to load your appointments.");
  }
}

function renderAppointments(appointments) {
  if (!tableBody) {
    tableBody = document.getElementById("patientTableBody");
  }

  if (!tableBody) return;

  tableBody.innerHTML = "";

  const actionTh = document.querySelector("#patientTable thead tr th:last-child");
  if (actionTh) {
    actionTh.style.display = "table-cell"; // Always show "Actions" column
  }

  if (!appointments.length) {
    tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center;">No Appointments Found</td></tr>`;
    return;
  }

  appointments.forEach(appointment => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${appointment.patientName || "You"}</td>
      <td>${appointment.doctorName}</td>
      <td>${appointment.appointmentDate}</td>
      <td>${appointment.appointmentTimeOnly}</td>
      <td>${appointment.status == 0 ? `<img src="../assets/images/edit/edit.png" alt="Edit" class="prescription-btn" data-id="${appointment.patientId}">` : `<a class="view-prescription" href="/pages/patientPrescription.html?appointmentId=${encodeURIComponent(appointment.id)}">View Prescription</a>`}</td>
    `;

    if (appointment.status == 0) {
      const actionBtn = tr.querySelector(".prescription-btn");
      actionBtn?.addEventListener("click", () => redirectToUpdatePage(appointment));
    }

    tableBody.appendChild(tr);
  });
}

function redirectToUpdatePage(appointment) {
  // Prepare the query parameters
  const queryString = new URLSearchParams({
    appointmentId: appointment.id,
    patientId: appointment.patientId,
    patientName: appointment.patientName || "You",
    doctorName: appointment.doctorName,
    doctorId: appointment.doctorId,
    appointmentDate: appointment.appointmentDate,
    appointmentTime: appointment.appointmentTimeOnly,
  }).toString();

  // Redirect to the update page with the query string
  setTimeout(() => {
    window.location.href = `/pages/updateAppointment.html?${queryString}`;
  }, 100);
}


function handleFilterChange() {
  const searchBar = document.getElementById("searchBar");
  const appointmentFilter = document.getElementById("appointmentFilter");

  if (!searchBar || !appointmentFilter || !token) {
    return;
  }

  const searchBarValue = searchBar.value.trim().toLowerCase();
  const filterValue = appointmentFilter.value;

  const appointments = allAppointments.filter(appointment => {
    const matchesName = (appointment.doctorName || "").toLowerCase().includes(searchBarValue);
    const matchesStatus = filterValue === "future" ? Number(appointment.status) === 0
      : filterValue === "past" ? Number(appointment.status) === 1
      : true;
    return matchesName && matchesStatus;
  });

  renderAppointments(appointments);
}

