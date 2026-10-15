import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
import vm from 'node:vm';
const source = await readFile(new URL('../../main/resources/static/js/loggedPatient.js', import.meta.url), 'utf8');
const cards = [];
const elements = {
  searchBar: { value: '' }, filterTime: { value: '' }, filterSpecialty: { value: '' },
  doctorResultCount: { textContent: '' },
  content: { set innerHTML(value) { cards.length = 0; }, appendChild(card) { cards.push(card); } }
};
const doctors = [
  { id: 1, name: 'Morning', availableTimes: ['09:00-10:00'] },
  { id: 2, name: 'Afternoon', availableTimes: ['12:00-13:00', '16:00-17:00'] },
  { id: 3, name: 'Both', availableTimes: ['11:00-12:00', '14:00-15:00'] }
];
let pending = [];
const context = vm.createContext({ console, window: {},
  document: { addEventListener() {}, getElementById(id) { return elements[id]; } },
  createDoctorCard(doctor) { return JSON.parse(JSON.stringify(doctor)); },
  filterDoctors(name, time, specialty) {
    return new Promise(resolve => pending.push({ name, time, specialty, resolve }));
  }
});
vm.runInContext(source.replace(/^import .*;\n/gm, '').replace(/export function/g, 'function'), context);
async function filter(time, results = doctors) {
  elements.filterTime.value = time;
  const task = vm.runInContext('filterDoctorsOnChange()', context);
  const request = pending.pop();
  assert.equal(request.time, time || null);
  request.resolve({ doctors: results });
  await task;
}
await filter('AM');
assert.deepEqual(cards.map(card => card.id), [1, 3]);
assert.deepEqual(cards[1].availableTimes, ['11:00-12:00']);
assert.match(elements.doctorResultCount.textContent, /^2 doctors listed · AM/);
await filter('PM');
assert.deepEqual(cards.map(card => card.id), [2, 3]);
assert.deepEqual(cards[0].availableTimes, ['12:00-13:00', '16:00-17:00']);
assert.deepEqual(cards[1].availableTimes, ['14:00-15:00']);
assert.match(elements.doctorResultCount.textContent, /^2 doctors listed · PM/);
assert.equal(doctors[2].availableTimes.length, 2);
await filter('');
assert.equal(cards.length, 3);
assert.match(elements.doctorResultCount.textContent, /^3 doctors listed · All times/);
await filter('PM', []);
assert.equal(cards.length, 0);
assert.match(elements.doctorResultCount.textContent, /^0 doctors listed/);
elements.searchBar.value = ' Both ';
elements.filterSpecialty.value = 'general';
await filter('AM', [doctors[2]]);
assert.match(elements.doctorResultCount.textContent, /^1 doctor listed/);
elements.filterTime.value = 'AM';
const older = vm.runInContext('filterDoctorsOnChange()', context);
const oldRequest = pending.pop();
assert.equal(oldRequest.name, 'Both');
assert.equal(oldRequest.specialty, 'general');
elements.filterTime.value = 'PM';
const newer = vm.runInContext('filterDoctorsOnChange()', context);
pending.pop().resolve({ doctors: [doctors[1]] });
await newer;
oldRequest.resolve({ doctors: [doctors[0]] });
await older;
assert.deepEqual(cards.map(card => card.id), [2]);
assert.match(elements.doctorResultCount.textContent, /PM/);
console.log('Passed AM/PM membership and slots, noon boundary, counts, All Times reset, combined filters, empty results, and stale response protection');
