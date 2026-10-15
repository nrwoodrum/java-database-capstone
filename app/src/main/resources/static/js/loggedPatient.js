// loggedPatient.js 
import { createDoctorCard } from './components/doctorCard.js';
import { filterDoctors } from './services/doctorServices.js';
import { bookAppointment } from './services/appointmentRecordService.js';


let doctorRequest = 0;

document.addEventListener("DOMContentLoaded", initializeLoggedPatientPage);

function initializeLoggedPatientPage() {
  const searchBar = document.getElementById("searchBar");
  const filterTime = document.getElementById("filterTime");
  const filterSpecialty = document.getElementById("filterSpecialty");

  if (searchBar) searchBar.addEventListener("input", filterDoctorsOnChange);
  if (filterTime) filterTime.addEventListener("change", filterDoctorsOnChange);
  if (filterSpecialty) filterSpecialty.addEventListener("change", filterDoctorsOnChange);

  loadDoctorCards();
}

function loadDoctorCards() {
  return filterDoctorsOnChange();
}

window.showBookingOverlay = function showBookingOverlay(e, doctor, patient) {
  const button = e.target;
  const rect = button.getBoundingClientRect();
  console.log(patient.name)
  console.log(patient)
  const ripple = document.createElement("div");
  ripple.classList.add("ripple-overlay");
  ripple.style.left = `${e.clientX}px`;
  ripple.style.top = `${e.clientY}px`;
  document.body.appendChild(ripple);

  setTimeout(() => ripple.classList.add("active"), 50);

  const modalApp = document.createElement("div");
  modalApp.classList.add("modalApp");

  modalApp.innerHTML = `
    <h2>Book Appointment</h2>
    <input class="input-field" type="text" value="${patient.name}" disabled />
    <input class="input-field" type="text" value="${doctor.name}" disabled />
    <input class="input-field" type="text" value="${doctor.specialty}" disabled/>
    <input class="input-field" type="email" value="${doctor.email}" disabled/>
    <input class="input-field" type="date" id="appointment-date" />
    <select class="input-field" id="appointment-time">
      <option value="">Select time</option>
      ${doctor.availableTimes.map(t => `<option value="${t}">${t}</option>`).join('')}
    </select>
    <button class="confirm-booking">Confirm Booking</button>
  `;

  document.body.appendChild(modalApp);

  setTimeout(() => modalApp.classList.add("active"), 600);

  modalApp.querySelector(".confirm-booking").addEventListener("click", async () => {
    const date = modalApp.querySelector("#appointment-date").value;
    const time = modalApp.querySelector("#appointment-time").value;
    const token = localStorage.getItem("token");
    if (!date || !time) {
      alert("Please select an appointment date and time.");
      return;
    }
    const startTime = time.split('-')[0];
    if (new Date(`${date}T${startTime}:00`) <= new Date()) {
      alert("Please select an appointment time in the future.");
      return;
    }
    const appointment = {
      doctor: { id: doctor.id },
      patient: { id: patient.id },
      appointmentTime: `${date}T${startTime}:00`,
      status: 0
    };


    const { success, message } = await bookAppointment(appointment, token);

    if (success) {
      alert("Appointment Booked successfully");
      ripple.remove();
      modalApp.remove();
    } else {
      alert("❌ Failed to book an appointment :: " + message);
    }
  });
}



async function filterDoctorsOnChange() {
  const search = document.getElementById("searchBar");
  const time = document.getElementById("filterTime");
  const specialty = document.getElementById("filterSpecialty");
  const count = document.getElementById("doctorResultCount");
  if (!search || !time || !specialty) return;
  const request = ++doctorRequest;
  const selectedTime = time.value;
  if (count) count.textContent = "Loading doctors…";
  try {
    const response = await filterDoctors(search.value.trim() || null, selectedTime || null, specialty.value || null);
    if (request !== doctorRequest) return;
    renderDoctorCards(response?.doctors || [], selectedTime);
  } catch (error) {
    if (request !== doctorRequest) return;
    console.error("Failed to filter doctors:", error);
    const content = document.getElementById("content");
    if (content) content.textContent = "Unable to load doctors. Please try again.";
    if (count) count.textContent = "Doctor count unavailable.";
  }
}

export function renderDoctorCards(doctors, selectedTime = "") {
  const content = document.getElementById("content");
  if (!content) return;
  content.innerHTML = "";
  let total = 0;
  for (const doctor of doctors) {
    let visibleDoctor = doctor;
    if (selectedTime) {
      const matchingTimes = (doctor.availableTimes || []).filter(slot => {
        const hour = Number(slot.trim().split(":")[0]);
        return selectedTime === "AM" ? hour < 12 : hour >= 12;
      });
      if (!matchingTimes.length) continue;
      visibleDoctor = { ...doctor, availableTimes: matchingTimes };
    }
    content.appendChild(createDoctorCard(visibleDoctor));
    total++;
  }
  if (!total) content.textContent = "No doctors found with the given filters.";
  const count = document.getElementById("doctorResultCount");
  if (count) {
    const period = selectedTime === "AM" ? " · AM (before noon)"
      : selectedTime === "PM" ? " · PM (noon onward)" : " · All times";
    count.textContent = `${total} ${total === 1 ? "doctor" : "doctors"} listed${period}`;
  }
}
