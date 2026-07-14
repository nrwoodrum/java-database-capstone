# Smart Clinic Management System Database Design

## Overview

The Smart Clinic Management System uses a hybrid database architecture:

- **MySQL** stores structured, relational, and transactional data such as patients, doctors, appointments, administrators, locations, and payments.
- **MongoDB** stores flexible or document-oriented data such as prescriptions, clinical notes, feedback, chat records, and uploaded document metadata.

This separation allows the system to use relational integrity where relationships are important while still supporting flexible medical and communication records that may change over time.

---

## Core Design Decisions

### 1. Each doctor owns an independent schedule

Each doctor should have their own working hours, breaks, time-off periods, and appointment duration rules. A clinic-wide schedule is not sufficient because doctors may work on different days, at different locations, or for different lengths of time.

The database should store **availability rules and exceptions**, not thousands of pre-generated future time-slot rows. The service layer can calculate bookable slots by combining:

1. The doctor's recurring working hours.
2. Doctor-specific unavailable periods, such as vacation, meetings, or breaks.
3. Existing appointments with active statuses.
4. The required appointment duration and scheduling interval.
5. The clinic location's operating hours, when applicable.

> **Design justification:** Generating slots when requested avoids storing large numbers of unused records and prevents stale slot data. A short-lived cache may be added later if slot calculation becomes expensive.

A slot should only be considered available when the entire appointment interval fits inside the doctor's working hours and does not overlap an unavailable period or an existing active appointment. The final conflict check must run again inside the booking transaction because two patients could select the same displayed slot at nearly the same time.

### 2. Appointment history is retained, but not necessarily forever

Past appointments should not be deleted merely because they have occurred or because a patient closes their account. They may be needed for continuity of care, billing, dispute resolution, operational reporting, and auditing. Appointments should therefore use statuses such as `COMPLETED`, `CANCELLED`, and `NO_SHOW` instead of being physically removed.

However, the design should not promise unlimited retention. The clinic should apply a documented retention policy based on applicable legal, contractual, and organizational requirements. When the retention period expires, records may be archived, anonymized, or securely deleted.

> **Design justification:** Retaining appointment history protects the integrity of medical and financial records, while a formal retention policy avoids keeping personal data longer than necessary.

Deleting or deactivating a patient's login account must not cascade-delete appointments, prescriptions, payments, or clinical notes. Personally identifying fields may later be anonymized when legally permitted, but referential and audit history should remain intact until the approved retention action occurs.

### 3. A prescription normally belongs to an appointment, with controlled exceptions

Most prescriptions should reference the appointment during which the doctor evaluated the patient. This provides clinical context and makes it possible to trace who issued the prescription, why it was issued, and which consultation supported it.

The `appointmentId` field in a prescription should therefore normally be required. A prescription may exist without a new appointment only for defined workflows such as an authorized refill, a correction, or a continuation of treatment. In those cases, the document should still reference the original prescription or originating appointment and include a reason.

Recommended prescription linkage fields are:

- `appointmentId`: the consultation that directly produced the prescription; nullable only for approved exceptions.
- `originatingAppointmentId`: the original clinical encounter when a refill or correction is issued later.
- `parentPrescriptionId`: the previous prescription when creating a refill, replacement, or amended version.
- `issuanceType`: `NEW`, `REFILL`, `REPLACEMENT`, or `AMENDMENT`.
- `independentReason`: required when `appointmentId` is absent.

> **Design justification:** This model preserves traceability without forcing every legitimate refill or correction to create a fake appointment. The service layer must reject an unlinked prescription unless the issuance type and reason satisfy an approved rule.

---

# MySQL Database Design

## 1. `patients`

Stores registered patient account and contact information.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `patient_id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique patient identifier |
| `first_name` | `VARCHAR(100)` | `NOT NULL` | Patient's first name |
| `last_name` | `VARCHAR(100)` | `NOT NULL` | Patient's last name |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` | Patient login email |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | Securely hashed password |
| `phone` | `VARCHAR(25)` | `NULL` | Patient phone number |
| `date_of_birth` | `DATE` | `NOT NULL` | Patient date of birth |
| `gender` | `VARCHAR(30)` | `NULL` | Optional gender value |
| `address_line_1` | `VARCHAR(255)` | `NULL` | Primary street address |
| `address_line_2` | `VARCHAR(255)` | `NULL` | Secondary address information |
| `city` | `VARCHAR(100)` | `NULL` | City |
| `state` | `VARCHAR(100)` | `NULL` | State or province |
| `postal_code` | `VARCHAR(20)` | `NULL` | ZIP or postal code |
| `account_status` | `VARCHAR(30)` | `NOT NULL`, `DEFAULT 'ACTIVE'` | Account state |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP` | Account creation time |
| `updated_at` | `TIMESTAMP` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | Last update time |

### Design Notes

- Email should be validated in application code before saving.
- Phone format should also be validated in application code.
- Passwords must never be stored as plain text.
- Instead of physically deleting a patient, the preferred approach is to set `account_status` to `INACTIVE` or `DELETED`.
- Historical appointments should normally remain in the database for auditing and reporting.

---

## 2. `doctors`

Stores doctor account, specialization, and contact information.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `doctor_id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique doctor identifier |
| `first_name` | `VARCHAR(100)` | `NOT NULL` | Doctor's first name |
| `last_name` | `VARCHAR(100)` | `NOT NULL` | Doctor's last name |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` | Doctor login email |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | Securely hashed password |
| `phone` | `VARCHAR(25)` | `NULL` | Doctor contact number |
| `specialization` | `VARCHAR(150)` | `NOT NULL` | Medical specialization |
| `license_number` | `VARCHAR(100)` | `NOT NULL`, `UNIQUE` | Professional license number |
| `years_of_experience` | `SMALLINT` | `NULL` | Number of years practicing |
| `profile_bio` | `TEXT` | `NULL` | Public doctor biography |
| `clinic_location_id` | `BIGINT` | `FOREIGN KEY`, `NULL` | Assigned clinic location |
| `account_status` | `VARCHAR(30)` | `NOT NULL`, `DEFAULT 'ACTIVE'` | Account state |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP` | Account creation time |
| `updated_at` | `TIMESTAMP` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | Last update time |

### Foreign Key

```sql
FOREIGN KEY (clinic_location_id)
REFERENCES clinic_locations(clinic_location_id)
ON DELETE SET NULL
```

### Design Notes

- Email and license number must be unique.
- A doctor should usually be deactivated instead of permanently deleted.
- Deleting a doctor should not automatically delete historical appointments.
- `clinic_location_id` may be nullable if doctors work remotely or at multiple locations.

---

## 3. `appointments`

Stores scheduled consultations between patients and doctors.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `appointment_id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique appointment identifier |
| `patient_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | Patient who booked the appointment |
| `doctor_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | Assigned doctor |
| `clinic_location_id` | `BIGINT` | `NULL`, `FOREIGN KEY` | Appointment location |
| `start_time` | `DATETIME` | `NOT NULL` | Appointment start time |
| `end_time` | `DATETIME` | `NOT NULL` | Appointment end time |
| `status` | `VARCHAR(30)` | `NOT NULL`, `DEFAULT 'SCHEDULED'` | Appointment status |
| `reason_for_visit` | `VARCHAR(500)` | `NULL` | Patient-provided reason |
| `booking_source` | `VARCHAR(30)` | `NOT NULL`, `DEFAULT 'PORTAL'` | Source of the booking |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP` | Booking creation time |
| `updated_at` | `TIMESTAMP` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | Last update time |

### Foreign Keys

```sql
FOREIGN KEY (patient_id)
REFERENCES patients(patient_id)
ON DELETE RESTRICT;

FOREIGN KEY (doctor_id)
REFERENCES doctors(doctor_id)
ON DELETE RESTRICT;

FOREIGN KEY (clinic_location_id)
REFERENCES clinic_locations(clinic_location_id)
ON DELETE SET NULL;
```

### Constraints and Rules

- `end_time` must be later than `start_time`.
- Standard appointments may be required to last exactly one hour.
- A doctor should not be allowed to have overlapping active appointments.
- A patient should also not be allowed to book overlapping appointments.
- Appointment conflict validation should be enforced in the service layer within a database transaction.
- A composite index should be created on `doctor_id`, `start_time`, and `end_time`.

Example conflict check:

```sql
SELECT COUNT(*)
FROM appointments
WHERE doctor_id = ?
  AND status IN ('SCHEDULED', 'CONFIRMED')
  AND start_time < ?
  AND end_time > ?;
```

The two parameter values should represent the proposed appointment end and start times.

---

## 4. `admins`

Stores administrative users who manage the portal.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `admin_id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique admin identifier |
| `username` | `VARCHAR(100)` | `NOT NULL`, `UNIQUE` | Admin login username |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` | Admin email |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | Securely hashed password |
| `first_name` | `VARCHAR(100)` | `NOT NULL` | Admin's first name |
| `last_name` | `VARCHAR(100)` | `NOT NULL` | Admin's last name |
| `role` | `VARCHAR(50)` | `NOT NULL`, `DEFAULT 'ADMIN'` | Administrative role |
| `account_status` | `VARCHAR(30)` | `NOT NULL`, `DEFAULT 'ACTIVE'` | Account state |
| `last_login_at` | `TIMESTAMP` | `NULL` | Most recent successful login |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP` | Account creation time |

### Design Notes

- Both `username` and `email` should be unique.
- Passwords should be stored using a strong one-way hashing algorithm.
- Role checks should be enforced through application security rules.

---

## 5. `doctor_availability`

Stores doctor working hours and unavailable periods.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `availability_id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique availability record |
| `doctor_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | Related doctor |
| `day_of_week` | `TINYINT` | `NULL` | Day from 1 through 7 for recurring hours |
| `available_from` | `TIME` | `NULL` | Start of recurring availability |
| `available_to` | `TIME` | `NULL` | End of recurring availability |
| `unavailable_start` | `DATETIME` | `NULL` | Start of a blocked period |
| `unavailable_end` | `DATETIME` | `NULL` | End of a blocked period |
| `availability_type` | `VARCHAR(30)` | `NOT NULL` | `WORKING_HOURS` or `UNAVAILABLE` |
| `reason` | `VARCHAR(255)` | `NULL` | Optional reason for unavailability |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP` | Record creation time |

### Foreign Key

```sql
FOREIGN KEY (doctor_id)
REFERENCES doctors(doctor_id)
ON DELETE CASCADE
```

### Design Notes

- Every availability record belongs to one doctor; doctors do not share a single generic schedule.
- Recurring working hours and one-time exceptions should be stored separately in production, even if this initial design uses one table. This prevents many nullable columns and makes validation clearer.
- The system should calculate available slots from working hours, unavailable periods, existing appointments, appointment duration, and clinic hours. It should not permanently store every possible future slot.
- Availability records can be deleted when a doctor is deleted because they are scheduling rules rather than permanent clinical history. In practice, doctor accounts should normally be deactivated instead of deleted.
- The application should validate that an unavailable period ends after it starts.
- Appointment booking must verify both availability and appointment conflicts again inside the booking transaction.
- Existing appointments should not be silently invalidated when availability changes. The system should reject the change or require an explicit rescheduling workflow.

---

## 6. `clinic_locations`

Stores physical clinic locations.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `clinic_location_id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique location identifier |
| `name` | `VARCHAR(150)` | `NOT NULL` | Clinic location name |
| `phone` | `VARCHAR(25)` | `NULL` | Location phone number |
| `email` | `VARCHAR(255)` | `NULL` | Location email |
| `address_line_1` | `VARCHAR(255)` | `NOT NULL` | Street address |
| `address_line_2` | `VARCHAR(255)` | `NULL` | Additional address information |
| `city` | `VARCHAR(100)` | `NOT NULL` | City |
| `state` | `VARCHAR(100)` | `NOT NULL` | State or province |
| `postal_code` | `VARCHAR(20)` | `NOT NULL` | ZIP or postal code |
| `active` | `BOOLEAN` | `NOT NULL`, `DEFAULT TRUE` | Whether the location is active |

---

## 7. `payments`

Stores payment transactions related to appointments.

| Column | Data Type | Constraints | Description |
|---|---|---|---|
| `payment_id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | Unique payment identifier |
| `appointment_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | Related appointment |
| `patient_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | Paying patient |
| `amount` | `DECIMAL(10,2)` | `NOT NULL` | Payment amount |
| `currency` | `CHAR(3)` | `NOT NULL`, `DEFAULT 'USD'` | ISO currency code |
| `payment_status` | `VARCHAR(30)` | `NOT NULL` | Payment state |
| `payment_method` | `VARCHAR(50)` | `NULL` | Card, cash, insurance, or other method |
| `transaction_reference` | `VARCHAR(255)` | `NULL`, `UNIQUE` | External payment reference |
| `paid_at` | `TIMESTAMP` | `NULL` | Payment completion time |
| `created_at` | `TIMESTAMP` | `NOT NULL`, `DEFAULT CURRENT_TIMESTAMP` | Record creation time |

### Foreign Keys

```sql
FOREIGN KEY (appointment_id)
REFERENCES appointments(appointment_id)
ON DELETE RESTRICT;

FOREIGN KEY (patient_id)
REFERENCES patients(patient_id)
ON DELETE RESTRICT;
```

### Design Notes

- Sensitive card data should not be stored directly.
- Payment provider tokens or transaction references may be stored instead.
- Historical payment data should be retained for financial reporting.

---

## MySQL Relationship Summary

- One patient can have many appointments.
- One doctor can have many appointments.
- One clinic location can have many doctors and appointments.
- One doctor can have many availability records.
- One appointment may have one or more payment records.
- Admin users manage doctors and platform operations but do not need a direct foreign-key relationship to every record.

---

## MySQL Deletion Strategy

Hard deletion should be used carefully.

| Entity | Recommended Behavior |
|---|---|
| Patient | Soft delete or deactivate; retain clinical, appointment, and payment history according to the retention policy |
| Doctor | Soft delete or deactivate; retain historical appointments |
| Appointment | Retain past records; use statuses such as `COMPLETED`, `CANCELLED`, or `NO_SHOW`; archive/anonymize only under the retention policy |
| Doctor availability | May be deleted with the doctor |
| Clinic location | Deactivate rather than delete if referenced |
| Payment | Never delete under normal operations |
| Admin | Deactivate rather than delete |

---

# MongoDB Collection Design

MongoDB is useful for data that is flexible, nested, frequently expanded, or not strongly relational. MySQL identifiers can be stored inside MongoDB documents to connect records across the two databases.

## 1. `prescriptions`

Stores prescription records created by doctors for patients.

```json
{
  "_id": "ObjectId",
  "prescriptionId": "string",
  "patientId": 1001,
  "doctorId": 501,
  "appointmentId": 9001,
  "originatingAppointmentId": 9001,
  "parentPrescriptionId": null,
  "issuanceType": "NEW",
  "independentReason": null,
  "diagnosis": "Upper respiratory infection",
  "medications": [
    {
      "name": "Amoxicillin",
      "dosage": "500 mg",
      "frequency": "Three times daily",
      "duration": "7 days",
      "instructions": "Take with food"
    }
  ],
  "generalInstructions": "Rest and drink fluids",
  "status": "ACTIVE",
  "issuedAt": "ISODate",
  "expiresAt": "ISODate",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Design Notes

- `patientId`, `doctorId`, and appointment identifiers reference MySQL records and must be validated by the service layer.
- A new prescription should normally require `appointmentId` so it can be traced to the consultation that produced it.
- A refill, replacement, or amendment may omit a new `appointmentId` only when it references `originatingAppointmentId` or `parentPrescriptionId` and provides an approved reason.
- Medications are embedded because they belong to one prescription and are normally read together.
- A prescription may contain multiple medications with different instructions.
- Prescriptions should be versioned, revoked, expired, or marked inactive rather than overwritten or deleted. This preserves the medication history.
- The application should record who issued each version and when it was issued.

---

## 2. `clinical_notes`

Stores flexible notes written during or after appointments.

```json
{
  "_id": "ObjectId",
  "appointmentId": 9001,
  "patientId": 1001,
  "doctorId": 501,
  "noteType": "CONSULTATION",
  "subjective": "Patient reports headache and fatigue.",
  "objective": {
    "temperature": 99.1,
    "bloodPressure": "120/80",
    "heartRate": 74
  },
  "assessment": "Possible viral infection",
  "plan": "Rest, hydration, and follow-up if symptoms worsen",
  "tags": ["headache", "fatigue"],
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Design Notes

- Clinical note structures may vary by specialization.
- Nested fields make it easier to store measurements and observations.
- Access must be restricted to authorized doctors and administrators.
- Changes should be auditable because medical notes are sensitive.

---

## 3. `patient_feedback`

Stores reviews or feedback related to completed appointments.

```json
{
  "_id": "ObjectId",
  "patientId": 1001,
  "doctorId": 501,
  "appointmentId": 9001,
  "rating": 5,
  "comments": "The doctor was helpful and explained everything clearly.",
  "anonymous": false,
  "status": "PUBLISHED",
  "createdAt": "ISODate"
}
```

### Design Notes

- The application should validate that ratings fall within an allowed range, such as 1 through 5.
- Feedback should usually only be allowed after an appointment is completed.
- Moderation status may be used before displaying comments publicly.

---

## 4. `chat_messages`

Stores communication between patients, doctors, and support staff.

```json
{
  "_id": "ObjectId",
  "conversationId": "conv-12345",
  "senderId": 1001,
  "senderType": "PATIENT",
  "recipientId": 501,
  "recipientType": "DOCTOR",
  "message": "Should I stop eating before my appointment?",
  "attachments": [],
  "sentAt": "ISODate",
  "readAt": null,
  "status": "SENT"
}
```

### Design Notes

- Messages can be grouped by `conversationId`.
- An index should be created on `conversationId` and `sentAt`.
- Large attachments should not be stored directly in the document.
- Attachment metadata and secure storage references may be stored instead.

---

## 5. `uploaded_documents`

Stores metadata for patient-uploaded or clinic-generated files.

```json
{
  "_id": "ObjectId",
  "patientId": 1001,
  "appointmentId": 9001,
  "uploadedById": 1001,
  "uploadedByType": "PATIENT",
  "documentType": "LAB_RESULT",
  "fileName": "blood-test-results.pdf",
  "contentType": "application/pdf",
  "storageKey": "patients/1001/documents/file-abc123",
  "fileSizeBytes": 483920,
  "description": "Recent blood test results",
  "uploadedAt": "ISODate"
}
```

### Design Notes

- The actual binary file should preferably be stored in secure object storage.
- MongoDB stores metadata and the storage location.
- Access controls should ensure that users only access authorized documents.
- File types and file sizes should be validated in application code.

---

## 6. `audit_events`

Stores important activity records for security and accountability.

```json
{
  "_id": "ObjectId",
  "actorId": 1,
  "actorType": "ADMIN",
  "action": "DOCTOR_PROFILE_UPDATED",
  "entityType": "DOCTOR",
  "entityId": 501,
  "details": {
    "changedFields": ["phone", "specialization"]
  },
  "ipAddress": "192.0.2.10",
  "occurredAt": "ISODate"
}
```

### Design Notes

- Audit events should be append-only.
- They can record logins, profile changes, appointment updates, and prescription changes.
- Audit records should have a retention policy based on security and legal requirements.

---

## MongoDB Index Suggestions

```javascript
db.prescriptions.createIndex({ patientId: 1, issuedAt: -1 });
db.prescriptions.createIndex({ doctorId: 1, issuedAt: -1 });
db.prescriptions.createIndex({ appointmentId: 1 }, { unique: true });

db.clinical_notes.createIndex({ appointmentId: 1 });
db.clinical_notes.createIndex({ patientId: 1, createdAt: -1 });

db.patient_feedback.createIndex({ doctorId: 1, createdAt: -1 });
db.patient_feedback.createIndex({ appointmentId: 1 }, { unique: true });

db.chat_messages.createIndex({ conversationId: 1, sentAt: 1 });

db.uploaded_documents.createIndex({ patientId: 1, uploadedAt: -1 });

db.audit_events.createIndex({ actorId: 1, occurredAt: -1 });
db.audit_events.createIndex({ entityType: 1, entityId: 1, occurredAt: -1 });
```

---

# Data Placement Summary

| Data Type | Database | Reason |
|---|---|---|
| Patients | MySQL | Structured account data with unique constraints |
| Doctors | MySQL | Structured profile data and relational connections |
| Appointments | MySQL | Transactional data requiring conflict checks and foreign keys |
| Admins | MySQL | Structured authentication and authorization data |
| Doctor availability | MySQL | Relational scheduling rules |
| Clinic locations | MySQL | Structured reusable location records |
| Payments | MySQL | Transactional and financial integrity |
| Prescriptions | MongoDB | Flexible nested medication structures |
| Clinical notes | MongoDB | Variable medical note formats |
| Feedback | MongoDB | Flexible comments and moderation fields |
| Chat messages | MongoDB | High-volume document-oriented communication |
| Uploaded document metadata | MongoDB | Flexible metadata and storage references |
| Audit events | MongoDB | Append-only event records with variable details |

---

# Key Validation and Business Rules

1. Email addresses must be unique for each account type and validated in application code.
2. Passwords must be hashed before storage.
3. Phone numbers should be validated in application code because formats vary by country.
4. Doctors must not have overlapping confirmed or scheduled appointments.
5. Patients must not have overlapping appointments.
6. Appointment end times must be later than start times.
7. Standard consultation appointments should be one hour unless the system supports configurable durations.
8. Appointments should not be physically deleted when cancelled or completed; they remain until the approved retention action occurs.
9. Patients and doctors should normally be deactivated rather than permanently deleted.
10. Each doctor must have independent working hours and unavailability rules. Available slots should be calculated rather than permanently pre-generated.
11. A new prescription normally requires an appointment reference; approved refills or amendments must retain a link to the originating appointment or prescription.
12. Prescription and clinical note access must be restricted to authorized users.
13. Payments and audit records should be retained for reporting and accountability.
14. Cross-database references must be validated by the service layer because MongoDB cannot enforce MySQL foreign keys.

---

# Final Design Decision

MySQL is the system of record for accounts, scheduling, availability, locations, and payments because these records require strong relationships, consistency, and transactional behavior. MongoDB is used for prescriptions, notes, messages, feedback, documents, and audit events because these records benefit from flexible schemas and nested data.

The Spring Boot service layer should coordinate operations between both databases. For example, when a doctor creates a prescription, the service should first confirm that the related patient, doctor, and appointment exist in MySQL before saving the prescription document in MongoDB.
