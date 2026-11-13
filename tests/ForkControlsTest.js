'use strict';
var fs = require('fs'), path = require('path'), vm = require('vm'), assert = require('assert');
var source = fs.readFileSync(path.join(__dirname, '..', 'web', 'home.js'), 'utf8');
var chosen = ['vpn', 'tv-settings', 'live', 'wifi', 'settings'], input = 'hdmi2', opened, allApps = 0, settings = 0;
var sandbox = {
  Prefs: {topButtons: function () { return chosen; }, boxInput: function () { return input; }},
  TV: {canOpenSettings: true, launch: function (id, params, cb) { opened = {id: id, params: params}; cb(true); },
    settings: function (cb) { settings++; cb(true); }},
  I18N: {name: function (s) { return s; }},
  tr: function (s) { return s; },
  inputs: [{key: 'live', id: 'tuner'}, {key: 'hdmi2', id: 'input2', label: 'Console'}],
  ALL_APPS_ID: 'all-apps', openApps: function () { allApps++; },
  Z: {TOP: 1, SHELF: 2}, st: {zone: 1, top: 3}, topButtons: []
};
['inputByKey', 'liveTV', 'boxTV', 'boxLabel', 'topCandidates', 'topKeys', 'launcher', 'tvSettings', 'activate'].forEach(function (name) {
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
// Exercise the shared editor after combining upstream inputs with the fork's top buttons and shelf shortcut.
var saved = {};
sandbox.editingTop = false;
sandbox.editKind = 'shelf';
sandbox.shelf = [{id: 'app', title: 'App'}];
sandbox.apps = sandbox.shelf.slice();
sandbox.Prefs.set = function (key, value) { saved[key] = value; if (key === 'topButtons') chosen = value; };
sandbox.Prefs.inputsOrder = function () { return saved.inputsOrder || null; };
sandbox.Prefs.inputsHidden = function () { return saved.inputsHidden || []; };
sandbox.renderEdit = sandbox.renderDrawer = sandbox.renderTop = sandbox.renderShelf = function () {};
sandbox.pickShelf = function () {};
sandbox.openEdit = function (kind) { sandbox.editKind = kind; sandbox.st.edit = 0; };
['shelfApps', 'editList', 'inputsEditList', 'orderedInputs', 'firstPort', 'openShelfEdit', 'openInputsEdit', 'editShelf', 'editInputs'].forEach(function (name) {
  var code = source.match(new RegExp('^  function ' + name + '\\([^]*?^  }', 'm'));
  assert(code, name + ' must exist');
  vm.runInNewContext(code[0], sandbox);
});
chosen = ['live', 'settings'];
sandbox.openShelfEdit(true);
sandbox.editShelf(0);
assert.deepStrictEqual(Array.from(saved.topButtons), ['settings'], 'top editor changes only top buttons');
assert.strictEqual(saved.inputsOrder, undefined);
sandbox.openInputsEdit();
assert.strictEqual(sandbox.editingTop, false, 'opening inputs clears the top editor mode');
sandbox.editInputs(0);
assert.deepStrictEqual(Array.from(saved.inputsHidden), ['live'], 'input can be hidden');
assert.deepStrictEqual(Array.from(saved.topButtons), ['settings'], 'input editor preserves top buttons');
sandbox.openShelfEdit();
sandbox.st.edit = sandbox.editList().rows.findIndex(function (item) { return item.id === 'all-apps'; });
sandbox.editShelf(0);
assert.deepStrictEqual(Array.from(saved.shelf), ['app', 'all-apps'], 'All Apps remains selectable after visiting inputs');
saved.inputsOrder = ['live', 'hdmi2']; saved.inputsHidden = [];
assert.strictEqual(sandbox.firstPort(), 1, 'HDMI button finds first visible port');
saved.inputsHidden = ['live'];
assert.strictEqual(sandbox.firstPort(), 0, 'HDMI button follows hidden Live TV');
console.log('ForkControlsTest: all checks passed.');
