import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
import vm from 'node:vm';
const base = new URL('../../main/resources/static/js/', import.meta.url).pathname;
const config = await readFile(base + 'config/config.js', 'utf8');
const source = await readFile(base + 'services/prescriptionServices.js', 'utf8');
const api = await import(`data:text/javascript;base64,${Buffer.from(source.replace("import { API_BASE_URL } from '../config/config.js';", config)).toString('base64')}`);
for (const [status, body] of [[201, '{"message":"Saved"}'], [401, 'Invalid or expired token'], [409, '{"message":"Already exists"}']]) {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, 'http://localhost:8080/prescription/save/token');
    assert.equal(options.method, 'POST');
    return new Response(body, { status });
  };
  assert.equal((await api.savePrescription({ appointmentId: 7 }, 'token')).success, status === 201);
}
globalThis.fetch = async () => { throw new TypeError('fetch failed'); };
const oldError = console.error; console.error = () => {};
assert.equal((await api.savePrescription({}, 'token')).success, false);
console.error = oldError;
globalThis.fetch = async () => new Response('Unauthorized access', { status: 401 });
await assert.rejects(() => api.getPrescription(7, 'token'), /Unauthorized access/);
const dashboard = await readFile(base + 'doctorDashboard.js', 'utf8');
function element() { return { value: '', style: {}, listeners: {}, children: [], addEventListener(event, fn) { this.listeners[event] = fn; }, appendChild(child) { this.children.push(child); } }; }
const dash = vm.createContext({ console, URLSearchParams, document: { getElementById() { return null; }, addEventListener() {}, createElement: element }, localStorage: { getItem() { return 'token'; } }, window: { location: {} } });
vm.runInContext(dashboard.replace(/^import .*;\n/m, ''), dash);
for (const [status, label, mode] of [[0, 'Add Prescription', 'add'], [1, 'View Prescription', 'view']]) {
  const row = vm.runInContext(`buildPatientRow({ id: 7, status: ${status}, patient: {name: 'Test Patient'} })`, dash);
  const button = row.children[0].children[0];
  assert.equal(button.textContent, label);
  button.listeners.click();
  const params = new URL('http://localhost' + dash.window.location.href).searchParams;
  assert.equal(params.get('mode'), mode);
  assert.equal(params.get('appointmentId'), '7');
}
const formSource = await readFile(base + 'addPrescription.js', 'utf8');
for (const existing of [false, true]) {
  const fields = Object.fromEntries(['prescriptionForm', 'savePrescription', 'patientName', 'medicines', 'dosage', 'notes', 'heading'].map(id => [id, element()]));
  let init; let saved;
  const ctx = vm.createContext({ console, URLSearchParams, Number, alert() {}, requireRole(role) { assert.equal(role, 'doctor'); return true; },
    document: { getElementById(id) { return fields[id]; }, addEventListener(_event, fn) { init = fn; } },
    window: { location: { search: '?appointmentId=7&patientName=Test+Patient&mode=add' } },
    localStorage: { getItem() { return 'token'; } },
    async getPrescription() { return { prescriptions: existing ? [{ patientName: 'Actual Patient', medication: 'Medicine', dosage: 'Daily', doctorNotes: 'Notes' }] : [] }; },
    async savePrescription(value) { saved = value; return { success: true }; }
  });
  vm.runInContext(formSource.replace(/^import .*;\n/m, ''), ctx);
  await init();
  if (existing) {
    assert.equal(fields.medicines.value, 'Medicine');
    assert.equal(fields.dosage.disabled, true);
    assert.equal(fields.savePrescription.style.display, 'none');
  } else {
    fields.medicines.value = ' Medicine '; fields.dosage.value = ' Daily ';
    await fields.prescriptionForm.listeners.submit({ preventDefault() {} });
    assert.equal(saved.appointmentId, 7);
    assert.equal(saved.medication, 'Medicine');
    assert.equal(ctx.window.location.href, '/doctorDashboard/token');
  }
}
console.log('Passed prescription API responses, network failure, doctor action navigation, form submission, and existing prescription view');
