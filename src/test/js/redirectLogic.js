// Feature: login-redirect-on-return, Property 4: transferToPage redirects correctly based on redirectTarget
// Pure helper extracted from transferToPage() in Login.html for unit testing without a DOM.

const ALLOWLIST = [
    '/Welcome',
    '/FalsehoodSearch',
    '/Falsehood/',
    '/Article/',
    '/ArticleEdit',
    '/FalsehoodSubmit',
    '/FalsehoodByBrand/'
];

/**
 * Pure redirect-decision function mirroring the logic in transferToPage().
 * Returns the URL that transferToPage() would assign to document.location.href.
 *
 * @param {string} redirectTarget - The server-injected redirect target path.
 * @param {string} gatewayPath    - The application gateway base URL.
 * @returns {string} The resolved destination URL.
 */
function computeRedirect(redirectTarget, gatewayPath) {
    if (!redirectTarget || redirectTarget.indexOf('://') !== -1 || !redirectTarget.startsWith('/')) {
        return gatewayPath + '/Welcome';
    }
    const isAllowed = ALLOWLIST.some(prefix => redirectTarget.startsWith(prefix));
    if (!isAllowed) {
        return gatewayPath + '/Welcome';
    }
    return gatewayPath + redirectTarget;
}

module.exports = { computeRedirect, ALLOWLIST };
