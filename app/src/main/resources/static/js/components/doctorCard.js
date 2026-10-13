import { deleteDoctor } from '../services/doctorServices.js';
import { getPatientData } from '../services/patientServices.js';

export function createDoctorCard(doctor) {
  const card = document.createElement('div');
  card.className = 'doctor-card';

  const role = localStorage.getItem("userRole");

  const infoDiv = document.createElement('div');
  infoDiv.className = 'doctor-info';

  const name = document.createElement('h3');
  name.textContent = doctor.name;

  const specialization = document.createElement('p');
  specialization.textContent = `Specialization: ${doctor.specialty || doctor.specialization || 'Not specified'}`;

  const email = document.createElement('p');
  email.textContent = `Email: ${doctor.email}`;

  const availability = document.createElement('p');
  const availableTimes = Array.isArray(doctor.availableTimes) ? doctor.availableTimes : [];
  availability.textContent = `Available Times: ${availableTimes.join(", ") || 'Not available'}`;

  infoDiv.appendChild(name);
  infoDiv.appendChild(specialization);
  infoDiv.appendChild(email);
  infoDiv.appendChild(availability);

  const actionsDiv = document.createElement('div');
  actionsDiv.className = 'card-actions';

  if (role === 'admin') {
    const removeBtn = document.createElement('button');
    removeBtn.textContent = 'Delete';
    removeBtn.className = 'delete-btn';

    removeBtn.addEventListener('click', async () => {
      const token = localStorage.getItem("token");
      if (!token) {
        alert("Admin token not found. Please log in again.");
        return;
      }

      try {
        const result = await deleteDoctor(doctor.id, token);
        if (result.success) {
          alert("Doctor deleted successfully.");
          card.remove();
        } else {
          alert("Failed to delete doctor: " + result.message);
        }
      } catch (error) {
        console.error("Error deleting doctor:", error);
        alert("An error occurred while deleting the doctor. Please try again.");
      }
    });

    actionsDiv.appendChild(removeBtn);
  } else if (role === 'loggedPatient') {
    const bookBtn = document.createElement('button');
    bookBtn.textContent = 'Book Now';
    bookBtn.className = 'book-btn';

    bookBtn.addEventListener('click', async (event) => {
      const token = localStorage.getItem("token");
      if (!token) {
        alert("Please log in to book an appointment.");
        return;
      }

      try {
        const patientData = await getPatientData(token);
        if (!patientData) {
          alert("Unable to load your patient profile. Please log in again.");
          return;
        }

        if (typeof window.showBookingOverlay === 'function') {
          window.showBookingOverlay(event, doctor, patientData);
        } else {
          alert('Booking UI is not available right now. Please refresh the page.');
        }
      } catch (error) {
        console.error("Error fetching patient details:", error);
        alert("An error occurred while fetching your details. Please try again.");
      }
    });

    actionsDiv.appendChild(bookBtn);
  } else {
    const bookBtn = document.createElement('button');
    bookBtn.textContent = 'Book Now';
    bookBtn.className = 'book-btn';

    bookBtn.addEventListener('click', () => {
      alert("Please log in to book an appointment.");
    });

    actionsDiv.appendChild(bookBtn);
  }

  card.appendChild(infoDiv);
  card.appendChild(actionsDiv);

  return card;
}

/*
Import the overlay function for booking appointments from loggedPatient.js

  Import the deleteDoctor API function to remove doctors (admin role) from docotrServices.js

  Import function to fetch patient details (used during booking) from patientServices.js

  Function to create and return a DOM element for a single doctor card
    Create the main container for the doctor card
    Retrieve the current user role from localStorage
    Create a div to hold doctor information
    Create and set the doctor’s name
    Create and set the doctor's specialization
    Create and set the doctor's email
    Create and list available appointment times
    Append all info elements to the doctor info container
    Create a container for card action buttons
    === ADMIN ROLE ACTIONS ===
      Create a delete button
      Add click handler for delete button
     Get the admin token from localStorage
        Call API to delete the doctor
        Show result and remove card if successful
      Add delete button to actions container
   
    === PATIENT (NOT LOGGED-IN) ROLE ACTIONS ===
      Create a book now button
      Alert patient to log in before booking
      Add button to actions container
  
    === LOGGED-IN PATIENT ROLE ACTIONS === 
      Create a book now button
      Handle booking logic for logged-in patient   
        Redirect if token not available
        Fetch patient data with token
        Show booking overlay UI with doctor and patient info
      Add button to actions container
   
  Append doctor info and action buttons to the car
  Return the complete doctor card element
*/
