// Unit tests for computeRedirect example cases
// Feature: login-redirect-on-return
// Validates: Requirements 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3

const assert = require('assert');
const { computeRedirect } = require('./redirectLogic');

const GATEWAY = 'https://gateway.example.com';

let passed = 0;
let failed = 0;

function test(description, fn) {
    try {
        fn();
        console.log(`  ✓ ${description}`);
        passed++;
    } catch (err) {
        console.error(`  ✗ ${description}`);
        console.error(`    ${err.message}`);
        failed++;
    }
}

console.log('computeRedirect unit tests\n');

// --- Guard: empty / falsy target ---
test('empty string redirectTarget navigates to gatewayPath + "/Welcome"', () => {
    const result = computeRedirect('', GATEWAY);
    assert.strictEqual(result, GATEWAY + '/Welcome');
});

// --- Guard: absolute URL with protocol ---
test('"http://evil.com" redirectTarget navigates to gatewayPath + "/Welcome"', () => {
    const result = computeRedirect('http://evil.com', GATEWAY);
    assert.strictEqual(result, GATEWAY + '/Welcome');
});

// --- Guard: protocol-relative URL (starts with //) ---
test('"//evil.com" redirectTarget navigates to gatewayPath + "/Welcome"', () => {
    const result = computeRedirect('//evil.com', GATEWAY);
    assert.strictEqual(result, GATEWAY + '/Welcome');
});

// --- Allowlist: valid known route ---
test('"/FalsehoodSearch" redirectTarget navigates to gatewayPath + "/FalsehoodSearch"', () => {
    const result = computeRedirect('/FalsehoodSearch', GATEWAY);
    assert.strictEqual(result, GATEWAY + '/FalsehoodSearch');
});

// --- Allowlist: parameterised route with prefix match ---
test('"/Falsehood/42" redirectTarget navigates to gatewayPath + "/Falsehood/42"', () => {
    const result = computeRedirect('/Falsehood/42', GATEWAY);
    assert.strictEqual(result, GATEWAY + '/Falsehood/42');
});

// --- Allowlist: path not on allowlist ---
test('"/unknownRoute" redirectTarget navigates to gatewayPath + "/Welcome"', () => {
    const result = computeRedirect('/unknownRoute', GATEWAY);
    assert.strictEqual(result, GATEWAY + '/Welcome');
});

// --- Summary ---
console.log(`\n${passed} passed, ${failed} failed`);
if (failed > 0) {
    process.exit(1);
}
