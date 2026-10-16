// render.js
function selectRole(role) {
  const token = localStorage.getItem('token');
  const currentRole = localStorage.getItem('userRole');

  if (role === "admin") {
    if (token && currentRole === 'admin') {
      window.location.href = `/adminDashboard/${token}`;
    } else if (token && currentRole && currentRole !== 'admin') {
      clearSessionAndRedirect('You are logged in under a different role. Please sign in again.', '/');
    } else {
      window.openModal('adminLogin');
    }
    return;
  }

  if (role === "doctor") {
    if (token && currentRole === 'doctor') {
      window.location.href = `/doctorDashboard/${token}`;
    } else if (token && currentRole && currentRole !== 'doctor') {
      clearSessionAndRedirect('You are logged in under a different role. Please sign in again.', '/');
    } else {
      window.openModal('doctorLogin');
    }
    return;
  }

  if (role === "patient") {
    if (token && currentRole === 'loggedPatient') {
      window.location.href = "/pages/loggedPatientDashboard.html";
    } else if (token && currentRole && currentRole !== 'loggedPatient') {
      clearSessionAndRedirect('You are logged in under a different role. Please sign in again.', '/');
    } else {
      window.openModal('patientLogin');
    }
    return;
  }

  if (role === "loggedPatient") {
    if (token && currentRole === 'loggedPatient') {
      window.location.href = "/pages/loggedPatientDashboard.html";
    } else {
      clearSessionAndRedirect('Your patient session is invalid. Please sign in again.', '/');
    }
    return;
  }
}

window.renderContent = function renderContent() {
  if (typeof renderHeader === 'function') {
    renderHeader();
  }

  if (typeof renderFooter === 'function') {
    renderFooter();
  }
};
