# User Story Template

**Title:**
_As a [user role], I want [feature/goal], so that [reason]._

**Acceptance Criteria:**
1. [Criteria 1]
2. [Criteria 2]
3. [Criteria 3]

**Priority:** [High/Medium/Low]
**Story Points:** [Estimated Effort in Points]
**Notes:**
- [Additional information or edge cases]

---

## Doctor User Stories

### Story 1: Doctor Login

**Title:**
_As a doctor, I want to log into the portal, so that I can manage my appointments._

**Acceptance Criteria:**
1. Doctor can enter email/username and password on the login page
2. System validates credentials and creates an authenticated session
3. On successful login, doctor is redirected to the doctor dashboard
4. Invalid credentials show a generic error message
5. Unauthenticated users cannot access doctor-only endpoints

**Priority:** High
**Story Points:** 3
**Notes:**
- Add login attempt throttling and audit logging

---

### Story 2: Doctor Logout

**Title:**
_As a doctor, I want to log out of the portal, so that I can protect my data._

**Acceptance Criteria:**
1. Doctor can click a logout action from the dashboard
2. Logout invalidates the session/token immediately
3. Doctor is redirected to a public or login page
4. Protected pages are not accessible after logout
5. Re-authentication is required for any subsequent secure actions

**Priority:** High
**Story Points:** 2
**Notes:**
- Ensure both client-side and server-side session cleanup

---

### Story 3: View Appointment Calendar

**Title:**
_As a doctor, I want to view my appointment calendar, so that I can stay organized._

**Acceptance Criteria:**
1. Doctor can open a calendar view from the dashboard
2. Calendar shows all appointments assigned to the logged-in doctor
3. Appointments display date, time, patient name, and status
4. Doctor can switch between day/week/month views
5. Calendar entries are sorted and displayed in the doctor’s timezone

**Priority:** High
**Story Points:** 5
**Notes:**
- Include color-coded status labels for quick scanning

---

### Story 4: Mark Unavailability

**Title:**
_As a doctor, I want to mark my unavailability, so that patients only see available time slots._

**Acceptance Criteria:**
1. Doctor can add unavailability blocks with start and end date/time
2. System prevents overlapping unavailability entries
3. Unavailable slots are excluded from patient booking options
4. Existing booked appointments in blocked windows are flagged for follow-up
5. Doctor can edit or remove previously created unavailability blocks

**Priority:** High
**Story Points:** 8
**Notes:**
- Use conflict checks to avoid race conditions during booking

---

### Story 5: Update Doctor Profile

**Title:**
_As a doctor, I want to update my profile with specialization and contact information, so that patients have up-to-date information._

**Acceptance Criteria:**
1. Doctor can open and edit profile fields from the portal
2. Editable fields include specialization, phone, and clinic/contact details
3. System validates required fields and format before saving
4. Updated profile data is persisted in the database
5. Public/patient-facing doctor profile reflects updates immediately or after refresh

**Priority:** Medium
**Story Points:** 4
**Notes:**
- Track profile changes with timestamps for auditing

---

### Story 6: View Patient Details for Upcoming Appointments

**Title:**
_As a doctor, I want to view patient details for upcoming appointments, so that I can be prepared._

**Acceptance Criteria:**
1. Doctor can open an upcoming appointments list
2. Each appointment includes linked patient details view
3. Patient details include core information needed for consultation prep
4. Access is limited to appointments assigned to the logged-in doctor
5. Unauthorized access attempts are blocked and logged

**Priority:** High
**Story Points:** 5
**Notes:**
- Ensure patient data visibility follows privacy and least-privilege rules
