## MySQL Database Design

This design starts with the required core tables: `patients`, `doctors`, `appointments`, and `admin`.
It also adds `clinic_locations`, `payments`, `doctor_availability`, and `prescriptions` for a realistic clinic workflow.

---

### Table: patients
- id: INT, Primary Key, Auto Increment
- first_name: VARCHAR(100), Not Null
- last_name: VARCHAR(100), Not Null
- date_of_birth: DATE, Not Null
- gender: ENUM('M','F','OTHER'), Null
- email: VARCHAR(255), Unique, Null
- phone: VARCHAR(20), Unique, Null
- address_line1: VARCHAR(255), Null
- address_line2: VARCHAR(255), Null
- city: VARCHAR(100), Null
- state: VARCHAR(100), Null
- postal_code: VARCHAR(20), Null
- created_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP
- updated_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- deleted_at: DATETIME, Null

Notes:
- `email` and `phone` are unique to avoid duplicate patient accounts.
- Soft delete (`deleted_at`) is preferred for medical history retention.
- Email/phone format validation should primarily happen in application code.

---

### Table: doctors
- id: INT, Primary Key, Auto Increment
- first_name: VARCHAR(100), Not Null
- last_name: VARCHAR(100), Not Null
- specialization: VARCHAR(150), Not Null
- license_number: VARCHAR(50), Not Null, Unique
- email: VARCHAR(255), Not Null, Unique
- phone: VARCHAR(20), Null, Unique
- location_id: INT, Foreign Key -> clinic_locations(id), Null
- is_active: TINYINT(1), Not Null, Default 1
- created_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP
- updated_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

Foreign keys:
- `location_id` references `clinic_locations(id)` ON DELETE SET NULL ON UPDATE CASCADE

Notes:
- `license_number` is unique to prevent duplicate doctor identities.
- Doctor records are usually deactivated (`is_active = 0`) instead of hard-deleted.

---

### Table: admin
- id: INT, Primary Key, Auto Increment
- username: VARCHAR(100), Not Null, Unique
- email: VARCHAR(255), Not Null, Unique
- password_hash: VARCHAR(255), Not Null
- role: ENUM('SUPER_ADMIN','ADMIN'), Not Null, Default 'ADMIN'
- last_login_at: DATETIME, Null
- is_active: TINYINT(1), Not Null, Default 1
- created_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP
- updated_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

Notes:
- Store only `password_hash`, never plain text password.
- Keep admins as active/inactive, not deleted, for auditability.

---

### Table: clinic_locations
- id: INT, Primary Key, Auto Increment
- name: VARCHAR(150), Not Null
- address_line1: VARCHAR(255), Not Null
- address_line2: VARCHAR(255), Null
- city: VARCHAR(100), Not Null
- state: VARCHAR(100), Not Null
- postal_code: VARCHAR(20), Not Null
- phone: VARCHAR(20), Null
- created_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP
- updated_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

Notes:
- Separated into its own table so doctors and appointments can reference locations cleanly.

---

### Table: appointments
- id: INT, Primary Key, Auto Increment
- doctor_id: INT, Foreign Key -> doctors(id), Not Null
- patient_id: INT, Foreign Key -> patients(id), Not Null
- location_id: INT, Foreign Key -> clinic_locations(id), Null
- appointment_start: DATETIME, Not Null
- appointment_end: DATETIME, Not Null
- status: ENUM('SCHEDULED','COMPLETED','CANCELLED','NO_SHOW'), Not Null, Default 'SCHEDULED'
- reason: VARCHAR(500), Null
- created_by_admin_id: INT, Foreign Key -> admin(id), Null
- created_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP
- updated_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

Foreign keys:
- `doctor_id` references `doctors(id)` ON DELETE RESTRICT ON UPDATE CASCADE
- `patient_id` references `patients(id)` ON DELETE RESTRICT ON UPDATE CASCADE
- `location_id` references `clinic_locations(id)` ON DELETE SET NULL ON UPDATE CASCADE
- `created_by_admin_id` references `admin(id)` ON DELETE SET NULL ON UPDATE CASCADE

Recommended indexes and constraints:
- Index: `(doctor_id, appointment_start)`
- Index: `(patient_id, appointment_start)`
- Check: `appointment_end > appointment_start`

Notes:
- Keep appointment history forever; do not hard-delete completed/cancelled records.
- Use `RESTRICT` on doctor/patient deletes to prevent orphaning medical history.

---

### Table: doctor_availability
- id: INT, Primary Key, Auto Increment
- doctor_id: INT, Foreign Key -> doctors(id), Not Null
- weekday: TINYINT, Not Null (1=Monday ... 7=Sunday)
- start_time: TIME, Not Null
- end_time: TIME, Not Null
- slot_minutes: INT, Not Null, Default 60
- is_available: TINYINT(1), Not Null, Default 1
- effective_from: DATE, Null
- effective_to: DATE, Null
- created_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP

Foreign keys:
- `doctor_id` references `doctors(id)` ON DELETE CASCADE ON UPDATE CASCADE

Constraints:
- Check: `end_time > start_time`
- Check: `slot_minutes IN (15, 30, 45, 60)`

Notes:
- This table answers whether each doctor should have their own available slots: yes.
- Admin/doctor can mark unavailable periods by toggling availability or date ranges.

---

### Table: prescriptions
- id: INT, Primary Key, Auto Increment
- appointment_id: INT, Foreign Key -> appointments(id), Not Null
- patient_id: INT, Foreign Key -> patients(id), Not Null
- doctor_id: INT, Foreign Key -> doctors(id), Not Null
- medication_name: VARCHAR(150), Not Null
- dosage: VARCHAR(100), Not Null
- frequency: VARCHAR(100), Not Null
- duration_days: INT, Null
- instructions: VARCHAR(1000), Null
- created_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP

Foreign keys:
- `appointment_id` references `appointments(id)` ON DELETE RESTRICT ON UPDATE CASCADE
- `patient_id` references `patients(id)` ON DELETE RESTRICT ON UPDATE CASCADE
- `doctor_id` references `doctors(id)` ON DELETE RESTRICT ON UPDATE CASCADE

Notes:
- Prescription should be tied to a specific appointment for clinical traceability.
- If standalone prescriptions are needed later, make `appointment_id` nullable and add reason metadata.

---

### Table: payments
- id: INT, Primary Key, Auto Increment
- appointment_id: INT, Foreign Key -> appointments(id), Not Null
- patient_id: INT, Foreign Key -> patients(id), Not Null
- amount: DECIMAL(10,2), Not Null
- currency: CHAR(3), Not Null, Default 'USD'
- payment_method: ENUM('CARD','CASH','INSURANCE','ONLINE'), Not Null
- payment_status: ENUM('PENDING','PAID','FAILED','REFUNDED'), Not Null, Default 'PENDING'
- transaction_ref: VARCHAR(100), Unique, Null
- paid_at: DATETIME, Null
- created_at: DATETIME, Not Null, Default CURRENT_TIMESTAMP

Foreign keys:
- `appointment_id` references `appointments(id)` ON DELETE RESTRICT ON UPDATE CASCADE
- `patient_id` references `patients(id)` ON DELETE RESTRICT ON UPDATE CASCADE

Notes:
- Payment records should remain even if clinical records are archived.

---

## Constraint and Behavior Decisions

1. **NOT NULL / UNIQUE / AUTO_INCREMENT**
	- All PKs use `INT AUTO_INCREMENT`.
	- Identity/contact fields use `UNIQUE` where duplication should be blocked (`patients.email`, `doctors.license_number`, `admin.username`).
	- Required business fields are `NOT NULL` (names, appointment times, statuses).

2. **Email/phone validation**
	- Basic DB constraints can limit length and uniqueness.
	- Format validation (regex, country rules, normalization) should be implemented in service-layer code.

3. **If a patient is deleted, what happens to appointments?**
	- Recommended: do not hard delete patient rows in production.
	- Use soft delete (`patients.deleted_at`) and keep appointment history intact.
	- FK behavior uses `ON DELETE RESTRICT` to prevent accidental loss of medical history.

4. **Should a doctor have overlapping appointments?**
	- No, overlapping appointments should be blocked.
	- Enforce in application logic during booking with transaction-level checks.
	- Optionally, add DB-level enforcement via trigger (MySQL lacks native exclusion constraints).

5. **Should patient history be retained forever?**
	- Yes, retain appointment and prescription history for continuity of care and audit.
	- Use status transitions and soft deletes instead of hard deletes.

6. **Should prescriptions be tied to appointments?**
	- Default design ties prescriptions to appointments (`appointment_id NOT NULL`).
	- This provides clear provenance of why/when medication was prescribed.

## MongoDB Collection Design

### Collection: appointment_feedback

Purpose:
- Complements MySQL transactional tables by storing flexible, user-generated feedback after appointments.
- Keeps structured records (patients/doctors/appointments/payments) in MySQL, while semi-structured feedback content lives in MongoDB.

Example document:

```json
{
	"_id": "ObjectId('665b1f3f7c9d8a0012ab4501')",
	"appointmentId": 51,
	"patientId": 104,
	"doctorId": 12,
	"submittedAt": "2026-06-02T14:35:10Z",
	"ratings": {
		"overall": 4,
		"communication": 5,
		"waitTime": 3,
		"facilityCleanliness": 4
	},
	"comment": "Doctor explained the treatment clearly. Wait time was a bit long.",
	"tags": ["clear-explanation", "friendly-staff", "long-wait"],
	"followUpRequested": true,
	"followUpPreferences": {
		"preferredContact": "EMAIL",
		"preferredTimeWindows": [
			{ "day": "MONDAY", "from": "09:00", "to": "11:00" },
			{ "day": "WEDNESDAY", "from": "14:00", "to": "16:00" }
		]
	},
	"metadata": {
		"language": "en-US",
		"source": "patient-portal-web",
		"schemaVersion": 1,
		"client": {
			"ipHash": "sha256:ab3f...",
			"deviceType": "mobile"
		}
	},
	"audit": {
		"isEdited": false,
		"editedAt": null,
		"flags": [
			{ "type": "NONE", "createdAt": "2026-06-02T14:35:10Z" }
		]
	}
}
```

Design decisions:
- Store references (`appointmentId`, `patientId`, `doctorId`) instead of the full patient/doctor object.
	- Reason: MySQL is the source of truth for core identity/profile data.
	- This avoids stale duplicates when patient or doctor details change.
- Embed nested feedback-specific structures (`ratings`, `followUpPreferences`, `metadata`, `audit`) because they are naturally document-shaped and evolve frequently.
- Support schema evolution by keeping `metadata.schemaVersion` and optional fields.
	- New fields can be added without table migrations, and old documents remain readable.
- Useful indexes for this collection:
	- `{ "appointmentId": 1 }` (fast lookup by appointment)
	- `{ "doctorId": 1, "submittedAt": -1 }` (doctor-specific analytics)
	- `{ "tags": 1 }` (tag-based querying)
