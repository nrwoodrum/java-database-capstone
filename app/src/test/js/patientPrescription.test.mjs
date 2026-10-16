import { readFile } from 'node:fs/promises';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const base = new URL('../../main/resources/static/', import.meta.url);
const source = await readFile(new URL('js/patientPrescription.js', base), 'utf8');
for (const scenario of ['success', 'empty', 'denied', 'invalid']) {
  let init; let calls = 0;
  const fields = Object.fromEntries(['prescriptionStatus', 'prescriptionDetails', 'patientName', 'medication', 'dosage', 'doctorNotes'].map(id => [id, { textContent: '', hidden: id === 'prescriptionDetails' }]));
  const context = vm.createContext({
    URLSearchParams, requireRole(role) { assert.equal(role, 'loggedPatient'); return true; },
    window: { location: { search: scenario === 'invalid' ? '?appointmentId=bad' : '?appointmentId=7' } },
    localStorage: { getItem() { return 'patient-token'; } },
    document: { addEventListener(_event, fn) { init = fn; }, getElementById(id) { return fields[id]; } },
    async getPatientPrescription(id, token) {
      calls++; assert.equal(id, 7); assert.equal(token, 'patient-token');
      if (scenario === 'denied') throw new Error('Access denied');
      return { prescriptions: scenario === 'empty' ? [] : [{ patientName: 'Patient', medication: 'Medicine', dosage: 'Once daily', doctorNotes: '<script>literal notes</script>' }] };
    }
  });
  vm.runInContext(source.replace(/^import .*;\n/m, ''), context);
  await init();
  if (scenario === 'success') {
    assert.equal(fields.prescriptionDetails.hidden, false);
    assert.equal(fields.dosage.textContent, 'Once daily');
    assert.equal(fields.doctorNotes.textContent, '<script>literal notes</script>');
  } else {
    assert.equal(fields.prescriptionDetails.hidden, true);
    assert.match(fields.prescriptionStatus.textContent, scenario === 'empty' ? /No prescription/ : scenario === 'denied' ? /Access denied/ : /invalid/);
  }
  assert.equal(calls, scenario === 'invalid' ? 0 : 1);
}
const html = await readFile(new URL('pages/patientPrescription.html', base), 'utf8');
assert.doesNotMatch(html, /<(?:form|input|textarea)\b/);
const service = await readFile(new URL('js/services/prescriptionServices.js', base), 'utf8');
const config = await readFile(new URL('js/config/config.js', base), 'utf8');
const { getPatientPrescription } = await import(`data:text/javascript;base64,${Buffer.from(service.replace("import { API_BASE_URL } from '../config/config.js';", config)).toString('base64')}`);
globalThis.fetch = async url => {
  assert.equal(url, 'http://localhost:8080/prescription/patient/7/patient-token');
  return new Response('{"prescriptions":[]}');
};
assert.deepEqual(await getPatientPrescription(7, 'patient-token'), { prescriptions: [] });
const patientSource = await readFile(new URL('js/patientAppointment.js', base), 'utf8');
const rows = [];
const body = { set innerHTML(value) { rows.length = 0; }, appendChild(row) { rows.push(row.innerHTML); } };
const context = vm.createContext({ console, encodeURIComponent,
 localStorage: { getItem() { return 'token'; } },
 document: { addEventListener() {}, getElementById() { return body; }, querySelector() { return null; }, createElement() { return { innerHTML: '', querySelector() { return null; } }; } }
});
vm.runInContext(patientSource.replace(/^import .*;\n/m, ''), context);
vm.runInContext('renderAppointments([{id:7,status:1},{id:8,status:0}])', context);
assert.match(rows[0], /patientPrescription.html\?appointmentId=7/);
assert.doesNotMatch(rows[1], /View Prescription/);
assert.match(rows[1], /alt="Edit"/);
console.log('Passed patient prescription details, empty/error states, safe text rendering, read-only screen, endpoint URL, and appointment links');
