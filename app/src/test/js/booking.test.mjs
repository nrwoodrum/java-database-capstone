import { readFile } from 'node:fs/promises';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const source = await readFile(new URL('../../main/resources/static/js/loggedPatient.js', import.meta.url), 'utf8');
let confirm;
let bookings = 0;
const alerts = [];
const date = { value: '' };
const time = { value: '' };
const context = vm.createContext({
  console: { log() {} },
  setTimeout() {},
  window: {},
  localStorage: { getItem() { return 'token'; } },
  alert(message) { alerts.push(message); },
  async bookAppointment(appointment) {
    bookings++;
    assert.equal(appointment.appointmentTime, '2099-01-01T09:00:00');
    return { success: true };
  },
  document: {
    addEventListener() {}, body: { appendChild() {} },
    createElement() {
      return {
        classList: { add() {} }, style: {}, remove() {},
        querySelector(selector) {
          if (selector === '#appointment-date') return date;
          if (selector === '#appointment-time') return time;
          return { addEventListener(_event, callback) { confirm = callback; } };
        }
      };
    }
  }
});
vm.runInContext(source.replace(/^import .*;\n/gm, '').replace(/export function/g, 'function'), context);
context.window.showBookingOverlay({ target: { getBoundingClientRect() { return {}; } } },
  { id: 1, availableTimes: ['09:00-10:00'] }, { id: 2, name: 'Test Patient' });
await confirm();
assert.equal(bookings, 0);
assert.match(alerts.pop(), /select an appointment date and time/);
date.value = '2000-01-01'; time.value = '09:00-10:00';
await confirm();
assert.equal(bookings, 0);
assert.match(alerts.pop(), /in the future/);
date.value = '2099-01-01';
await confirm();
assert.equal(bookings, 1);
console.log('Passed: empty and past dates rejected, future appointment submitted');
