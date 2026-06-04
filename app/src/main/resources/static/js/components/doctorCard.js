import { API_BASE_URL } from "../config/config.js";
import { showBookingOverlay } from "../loggedPatient.js";
import { getPatientData } from "../services/patientServices.js";

const DOCTOR_API = API_BASE_URL + "/doctor";

export function createDoctorCard(doctor) {
  const safeDoctor = doctor || {};
  const role = localStorage.getItem("userRole");

  const card = document.createElement("div");
  card.classList.add("doctor-card");
  card.dataset.role = role || "";

  const specialtyValue = safeDoctor.specialty || safeDoctor.specialization || "General";
  const emailValue = safeDoctor.email || "N/A";
  const availableTimes = Array.isArray(safeDoctor.availableTimes) ? safeDoctor.availableTimes : [];

  const infoDiv = document.createElement("div");
  infoDiv.classList.add("doctor-info");

  const actionsDiv = document.createElement("div");
  actionsDiv.classList.add("card-actions");

  const name = document.createElement("h3");
  name.textContent = safeDoctor.name || "Unknown Doctor";

  const specialization = document.createElement("p");
  specialization.textContent = `Specialization: ${specialtyValue}`;

  const email = document.createElement("p");
  email.textContent = `Email: ${emailValue}`;

  const availability = document.createElement("p");
  availability.textContent = `Availability: ${availableTimes.length ? availableTimes.join(", ") : "Not available"}`;

  if (role === "admin") {
    const removeBtn = document.createElement("button");
    removeBtn.textContent = "Delete";

    removeBtn.addEventListener("click", async () => {
      const isConfirmed = window.confirm("Are you sure you want to delete this doctor?");
      if (!isConfirmed) {
        return;
      }

      const token = localStorage.getItem("token");
      if (!token) {
        alert("Admin session expired. Please log in again.");
        return;
      }

      if (!safeDoctor.id) {
        alert("Cannot delete doctor: missing doctor id.");
        return;
      }

      try {
        const response = await fetch(`${DOCTOR_API}/${safeDoctor.id}/${token}`, {
          method: "DELETE",
        });

        const result = await response.json().catch(() => ({}));

        if (response.ok) {
          card.remove();
          alert(result.message || "Doctor deleted successfully.");
        } else {
          alert(result.message || "Failed to delete doctor.");
        }
      } catch (error) {
        console.error("Failed to delete doctor:", error);
        alert("An error occurred while deleting doctor.");
      }
    });

    actionsDiv.appendChild(removeBtn);
  } else if (role === "patient") {
    const bookNow = document.createElement("button");
    bookNow.textContent = "Book Now";
    bookNow.addEventListener("click", () => {
      alert("Patient needs to login first.");
    });
    actionsDiv.appendChild(bookNow);
  } else if (role === "loggedPatient") {
    const bookNow = document.createElement("button");
    bookNow.textContent = "Book Now";
    bookNow.addEventListener("click", async (e) => {
      const token = localStorage.getItem("token");
      if (!token) {
        alert("Session expired. Please login again.");
        return;
      }

      const patientData = await getPatientData(token);
      if (!patientData) {
        alert("Unable to load patient data.");
        return;
      }

      showBookingOverlay(e, safeDoctor, patientData);
    });
    actionsDiv.appendChild(bookNow);
  }

  infoDiv.appendChild(name);
  infoDiv.appendChild(specialization);
  infoDiv.appendChild(email);
  infoDiv.appendChild(availability);
  card.appendChild(infoDiv);
  card.appendChild(actionsDiv);

  return card;
}
