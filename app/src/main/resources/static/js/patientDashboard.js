// patientDashboard.js
import { getDoctors } from './services/doctorServices.js';
import { openModal } from './components/modals.js';
import { createDoctorCard } from './components/doctorCard.js';
import { filterDoctors } from './services/doctorServices.js';//call the same function to avoid duplication coz the functionality was same
import { patientSignup, patientLogin } from './services/patientServices.js';



document.addEventListener("DOMContentLoaded", () => {
  if (!requireRole("loggedPatient")) {
    return;
  }

  const btn = document.getElementById("patientSignup");
  const loginBtn = document.getElementById("patientLogin");
  const searchBar = document.getElementById("searchBar");
  const filterTime = document.getElementById("filterTime");
  const filterSpecialty = document.getElementById("filterSpecialty");

  if (btn) {
    btn.addEventListener("click", () => openModal("patientSignup"));
  }

  if (loginBtn) {
    loginBtn.addEventListener("click", () => {
      openModal("patientLogin");
    });
  }

  if (searchBar) searchBar.addEventListener("input", filterDoctorsOnChange);
  if (filterTime) filterTime.addEventListener("change", filterDoctorsOnChange);
  if (filterSpecialty) filterSpecialty.addEventListener("change", filterDoctorsOnChange);

  loadDoctorCards();
});

function loadDoctorCards() {
  const contentDiv = document.getElementById("content");
  if (!contentDiv) return;

  getDoctors()
    .then(doctors => {
      contentDiv.innerHTML = "";

      doctors.forEach(doctor => {
        const card = createDoctorCard(doctor);
        contentDiv.appendChild(card);
      });
    })
    .catch(error => {
      console.error("Failed to load doctors:", error);
    });
}

function filterDoctorsOnChange() {
  const searchBar = document.getElementById("searchBar");
  const filterTime = document.getElementById("filterTime");
  const filterSpecialty = document.getElementById("filterSpecialty");
  const contentDiv = document.getElementById("content");

  if (!searchBar || !filterTime || !filterSpecialty || !contentDiv) {
    return;
  }

  const searchBarValue = searchBar.value.trim();
  const filterTimeValue = filterTime.value;
  const filterSpecialtyValue = filterSpecialty.value;

  const name = searchBarValue.length > 0 ? searchBarValue : null;
  const time = filterTimeValue.length > 0 ? filterTimeValue : null;
  const specialty = filterSpecialtyValue.length > 0 ? filterSpecialtyValue : null;

  filterDoctors(name, time, specialty)
    .then(response => {
      const doctors = response?.doctors || [];
      contentDiv.innerHTML = "";

      if (doctors.length > 0) {
        doctors.forEach(doctor => {
          const card = createDoctorCard(doctor);
          contentDiv.appendChild(card);
        });
      } else {
        contentDiv.innerHTML = "<p>No doctors found with the given filters.</p>";
      }
    })
    .catch(error => {
      console.error("Failed to filter doctors:", error);
      alert("❌ An error occurred while filtering doctors.");
    });
}

window.signupPatient = async function () {
  try {
    const name = document.getElementById("name")?.value?.trim() || "";
    const email = document.getElementById("email")?.value?.trim() || "";
    const password = document.getElementById("password")?.value || "";
    const phone = document.getElementById("phone")?.value?.trim() || "";
    const address = document.getElementById("address")?.value?.trim() || "";

    if (!isNonEmpty(name) || !isNonEmpty(email) || !isNonEmpty(password) || !isNonEmpty(phone) || !isNonEmpty(address)) {
      alert("Please complete all patient fields before creating an account.");
      return;
    }

    if (!isValidEmail(email)) {
      alert("Please enter a valid email address.");
      return;
    }

    if (password.length < 6) {
      alert("Password must be at least 6 characters long.");
      return;
    }

    const data = { name, email, password, phone, address };
    const { success, message } = await patientSignup(data);
    if (success) {
      alert(message);
      const modal = document.getElementById("modal");
      if (modal) modal.style.display = "none";
      window.location.reload();
    }
    else alert(message);
  } catch (error) {
    console.error("Signup failed:", error);
    alert("❌ An error occurred while signing up.");
  }
};

window.loginPatient = async function () {
  try {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const data = {
      email,
      password
    }
    console.log("loginPatient :: ", data)
    const response = await patientLogin(data);
    console.log("Status Code:", response.status);
    console.log("Response OK:", response.ok);
    if (response.ok) {
      const result = await response.json();
      console.log(result);
      localStorage.setItem('userRole', 'loggedPatient');
      localStorage.setItem('token', result.token)
      window.location.href = '/pages/loggedPatientDashboard.html';
    } else {
      alert('❌ Invalid credentials!');
    }
  }
  catch (error) {
    alert("❌ Failed to Login : ", error);
    console.log("Error :: loginPatient :: ", error)
  }


}
