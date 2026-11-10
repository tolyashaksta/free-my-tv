'use strict';
var fs = require('fs'), path = require('path'), vm = require('vm'), assert = require('assert');
var source = fs.readFileSync(path.join(__dirname, '..', 'web', 'home.js'), 'utf8');
var chosen = ['vpn', 'tv-settings', 'live', 'wifi', 'settings'], input = 'hdmi2', opened, allApps = 0, settings = 0;
var sandbox = {
  Prefs: {topButtons: function () { return chosen; }, boxInput: function () { return input; }},
  TV: {canOpenSettings: true, launch: function (id, params, cb) { opened = {id: id, params: params}; cb(true); },
    settings: function (cb) { settings++; cb(true); }},
  tr: function (s) { return s; },
  inputs: [{key: 'live', id: 'tuner'}, {key: 'hdmi2', id: 'input2', label: 'Console'}],
  ALL_APPS_ID: 'all-apps', openApps: function () { allApps++; },
  Z: {TOP: 1, SHELF: 2}, st: {zone: 1, top: 3}, topButtons: []
};
['liveTV', 'boxTV', 'boxLabel', 'topCandidates', 'topKeys', 'launcher', 'tvSettings', 'activate'].forEach(function (name) {
  var code = source.match(new RegExp('^  function ' + name + '\\([^]*?^  }', 'm'));
  assert(code, name + ' must exist');
  vm.runInNewContext(code[0], sandbox);
});
assert.deepStrictEqual(Array.from(sandbox.topKeys()), ['tv-settings', 'live', 'settings'], 'retired indicators disappear without disturbing button order');
var live = sandbox.topCandidates().filter(function (item) { return item.id === 'live'; })[0];
assert.strictEqual(live.title, 'Console', 'configured Live TV button shows HDMI name');
assert.strictEqual(live.symbol, 'tv');
sandbox.topButtons = [{id: 'live'}];
sandbox.activate();
assert.strictEqual(opened.id, 'input2', 'configured button opens HDMI after merge');
input = 'hdmi4';
sandbox.activate();
assert.strictEqual(opened.id, 'tuner', 'missing HDMI falls back to tuner');
sandbox.topButtons = [{id: 'tv-settings'}];
sandbox.activate();
assert.strictEqual(settings, 1, 'fork TV Settings button still opens TV settings');
sandbox.launcher('all-apps', 'All Apps')();
assert.strictEqual(allApps, 1, 'fork shelf shortcut opens All Apps');
var params = {contentTarget: 'nuvio://tmdb/movie/603'};
sandbox.launcher('nuvio', 'Nuvio', params)();
assert.strictEqual(opened.params, params, 'upstream Nuvio parameters survive custom launcher');
sandbox.TV.canOpenSettings = false;
assert(!sandbox.topKeys().includes('tv-settings'), 'unavailable TV Settings stays hidden');
chosen = [];
assert.strictEqual(sandbox.topKeys().length, 0, 'empty selection remains empty');
console.log('ForkControlsTest: all checks passed.');
