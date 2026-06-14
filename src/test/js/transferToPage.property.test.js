// Feature: login-redirect-on-return, Property 4: transferToPage redirects correctly based on redirectTarget
// Standalone property-based test using fast-check.
// Run with: node -e "require('fast-check')" 2>/dev/null || npm install fast-check
//           node src/test/js/transferToPage.property.test.js
//
// To set up a minimal Node.js test environment:
//   cd src/test/js && npm init -y && npm install fast-check

const fc = require('fast-check');
const { computeRedirect, ALLOWLIST } = require('./redirectLogic');

// Feature: login-redirect-on-return, Property 4: transferToPage redirects correctly based on redirectTarget
fc.assert(
    fc.property(fc.string(), (redirectTarget) => {
        const gatewayPath = 'https://gateway';
        const result = computeRedirect(redirectTarget, gatewayPath);

        const isValid = redirectTarget
            && !redirectTarget.includes('://')
            && redirectTarget.startsWith('/')
            && ALLOWLIST.some(p => redirectTarget.startsWith(p));

        const expected = isValid
            ? `${gatewayPath}${redirectTarget}`
            : `${gatewayPath}/Welcome`;

        return result === expected;
    }),
    { numRuns: 100 }
);

console.log('Property 4 passed: transferToPage redirects correctly based on redirectTarget');
