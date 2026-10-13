// util.js
function setRole(role) {
  localStorage.setItem("userRole", role);
}

function getRole() {
  return localStorage.getItem("userRole");
}

function clearRole() {
  localStorage.removeItem("userRole");
}

function clearSessionAndRedirect(message = "Session expired or invalid login. Please sign in again.", redirectPath = "/") {
  localStorage.removeItem("userRole");
  localStorage.removeItem("token");
  if (message) {
    alert(message);
  }
  window.location.href = redirectPath;
}

function requireRole(expectedRole, redirectPath = "/") {
  const role = localStorage.getItem("userRole");
  const token = localStorage.getItem("token");

  if (!token || role !== expectedRole) {
    clearSessionAndRedirect("Your session is invalid or for the wrong role. Please sign in again.", redirectPath);
    return false;
  }

  return true;
}

function isNonEmpty(value) {
  return typeof value === "string" ? value.trim().length > 0 : Boolean(value);
}

function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value || "").trim());
}

