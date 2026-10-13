import { openModal } from '../components/modals.js';
import { API_BASE_URL } from '../config/config.js';
import { patientLogin, patientSignup } from './patientServices.js';

const ADMIN_API = `${API_BASE_URL}/admin/login`;
const DOCTOR_API = `${API_BASE_URL}/doctor/login`;
const PATIENT_API = `${API_BASE_URL}/patient/login`;

window.openModal = openModal;

window.onload = () => {
  const adminBtn = document.getElementById('adminBtn');
  const doctorBtn = document.getElementById('doctorBtn');
  const patientBtn = document.getElementById('patientBtn');

  if (adminBtn) {
    adminBtn.addEventListener('click', () => openModal('adminLogin'));
  }

  if (doctorBtn) {
    doctorBtn.addEventListener('click', () => openModal('doctorLogin'));
  }

  if (patientBtn) {
    patientBtn.addEventListener('click', () => openModal('patientLogin'));
  }
};

window.adminLoginHandler = async function() {
  const username = document.getElementById('username')?.value.trim();
  const password = document.getElementById('password')?.value.trim();

  if (!username || !password) {
    alert('Please enter both username and password.');
    return;
  }

  const admin = { username, password };

  try {
    const response = await fetch(ADMIN_API, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(admin)
    });

    const payload = await response.json().catch(() => ({}));

    if (response.ok) {
      const data = payload || {};
      localStorage.setItem('userRole', 'admin');
      localStorage.setItem('token', data.token);
      selectRole('admin');
    } else {
      alert(payload.error || 'Invalid admin credentials. Please try again.');
    }
  } catch (error) {
    console.error('Error during admin login:', error);
    alert('An error occurred during login. Please try again later.');
  }
};

window.doctorLoginHandler = async function() {
  const email = document.getElementById('email')?.value.trim();
  const password = document.getElementById('password')?.value.trim();

  if (!email || !password) {
    alert('Please enter both email and password.');
    return;
  }

  const doctor = { email, password };

  try {
    const response = await fetch(DOCTOR_API, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(doctor)
    });

    const payload = await response.json().catch(() => ({}));

    if (response.ok) {
      localStorage.setItem('userRole', 'doctor');
      localStorage.setItem('token', payload.token);
      selectRole('doctor');
    } else {
      alert(payload.error || 'Invalid doctor credentials. Please try again.');
    }
  } catch (error) {
    console.error('Error during doctor login:', error);
    alert('An error occurred during login. Please try again later.');
  }
};

window.patientLoginHandler = async function() {
  const email = document.getElementById('email')?.value.trim();
  const password = document.getElementById('password')?.value.trim();

  if (!email || !password) {
    alert('Please enter both email and password.');
    return;
  }

  try {
    const response = await patientLogin({ email, password });
    const payload = await response.json().catch(() => ({}));

    if (response.ok) {
      localStorage.setItem('userRole', 'loggedPatient');
      localStorage.setItem('token', payload.token);
      window.location.href = '/pages/loggedPatientDashboard.html';
    } else {
      alert(payload.error || '❌ Patient not found or invalid password.');
    }
  } catch (error) {
    console.error('Error during patient login:', error);
    alert('❌ Failed to login as patient.');
  }
};

window.signupPatient = async function() {
  const name = document.getElementById('name')?.value.trim();
  const email = document.getElementById('email')?.value.trim();
  const password = document.getElementById('password')?.value.trim();
  const phone = document.getElementById('phone')?.value.trim();
  const address = document.getElementById('address')?.value.trim();

  if (!name || !email || !password || !phone || !address) {
    alert('Please complete all patient fields before creating an account.');
    return;
  }

  try {
    const result = await patientSignup({ name, email, password, phone, address });
    if (result.success) {
      alert(result.message || 'Signup successful');
      document.getElementById('modal').style.display = 'none';
      openModal('patientLogin');
    } else {
      alert(result.message || 'Signup failed. Please try again.');
    }
  } catch (error) {
    console.error('Error during patient signup:', error);
    alert('❌ Failed to create patient account.');
  }
};

window.loginPatient = window.patientLoginHandler;

/*
  Import the openModal function to handle showing login popups/modals
  Import the base API URL from the config file
  Define constants for the admin and doctor login API endpoints using the base URL

  Use the window.onload event to ensure DOM elements are available after page load
  Inside this function:
    - Select the "adminLogin" and "doctorLogin" buttons using getElementById
    - If the admin login button exists:
        - Add a click event listener that calls openModal('adminLogin') to show the admin login modal
    - If the doctor login button exists:
        - Add a click event listener that calls openModal('doctorLogin') to show the doctor login modal

/*
  Import the openModal function to handle showing login popups/modals
  Import the base API URL from the config file
  Define constants for the admin and doctor login API endpoints using the base URL

  Use the window.onload event to ensure DOM elements are available after page load
  Inside this function:
    - Select the "adminLogin" and "doctorLogin" buttons using getElementById
    - If the admin login button exists:
        - Add a click event listener that calls openModal('adminLogin') to show the admin login modal
    - If the doctor login button exists:
        - Add a click event listener that calls openModal('doctorLogin') to show the doctor login modal


  Define a function named adminLoginHandler on the global window object
  This function will be triggered when the admin submits their login credentials

  Step 1: Get the entered username and password from the input fields
  Step 2: Create an admin object with these credentials

  Step 3: Use fetch() to send a POST request to the ADMIN_API endpoint
    - Set method to POST
    - Add headers with 'Content-Type: application/json'
    - Convert the admin object to JSON and send in the body

  Step 4: If the response is successful:
    - Parse the JSON response to get the token
    - Store the token in localStorage
    - Call selectRole('admin') to proceed with admin-specific behavior

  Step 5: If login fails or credentials are invalid:
    - Show an alert with an error message

  Step 6: Wrap everything in a try-catch to handle network or server errors
    - Show a generic error message if something goes wrong


  Define a function named doctorLoginHandler on the global window object
  This function will be triggered when a doctor submits their login credentials

  Step 1: Get the entered email and password from the input fields
  Step 2: Create a doctor object with these credentials

  Step 3: Use fetch() to send a POST request to the DOCTOR_API endpoint
    - Include headers and request body similar to admin login

  Step 4: If login is successful:
    - Parse the JSON response to get the token
    - Store the token in localStorage
    - Call selectRole('doctor') to proceed with doctor-specific behavior

  Step 5: If login fails:
    - Show an alert for invalid credentials

  Step 6: Wrap in a try-catch block to handle errors gracefully
    - Log the error to the console
    - Show a generic error message
*/
