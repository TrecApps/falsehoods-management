/**
 * Markdown Editor — vanilla JS module
 * Ported from the Angular ng-markdown component.
 *
 * Public API:
 *   initMarkdownEditor(editorId, initialContent)
 *   window[editorId + '_content']  — kept in sync with textarea value
 *
 * Pure helper functions are exported on window._mdEditorUtils for testing.
 */

// ---------------------------------------------------------------------------
// Pure helper functions (stateless, exported for unit / property tests)
// ---------------------------------------------------------------------------

/**
 * Toggle a wrap marker (e.g. '**', '*', '~~', '`') around the selection.
 *
 * @param {string}  marker    The wrap marker string.
 * @param {boolean} isActive  Whether the marker is currently active at cursor.
 * @param {string}  content   Full textarea content.
 * @param {number}  selStart  Selection start index.
 * @param {number}  selEnd    Selection end index.
 * @returns {{ content: string, selStart: number, selEnd: number }}
 */
function toggleCodeStat(marker, isActive, content, selStart, selEnd) {
  const ml = marker.length;

  if (!isActive) {
    // Wrap selection with marker
    const pre      = content.substring(0, selStart);
    const selected = content.substring(selStart, selEnd);
    const post     = content.substring(selEnd);
    return {
      content:  `${pre}${marker}${selected}${marker}${post}`,
      selStart: selStart + ml,
      selEnd:   selEnd   + ml,
    };
  } else {
    // Remove surrounding markers
    const prevMarker = content.lastIndexOf(marker, selStart);
    const nextMarker = content.indexOf(marker, selEnd);

    if (prevMarker !== -1 && nextMarker !== -1) {
      const pre    = content.substring(0, prevMarker);
      const middle = content.substring(prevMarker + ml, nextMarker);
      const post   = content.substring(nextMarker + ml);
      return {
        content:  `${pre}${middle}${post}`,
        selStart: selStart - ml,
        selEnd:   selEnd   - ml,
      };
    }
    // Markers not found — no change
    return { content, selStart, selEnd };
  }
}

/**
 * Return the index of the start of the line containing `index`.
 * Returns 0 if on the first line.
 *
 * @param {string} content
 * @param {number} index
 * @returns {number}
 */
function getPreviousLineIndex(content, index) {
  const newLineN = content.lastIndexOf('\n', index - 1);
  const newLineR = content.lastIndexOf('\r', index - 1);
  const ret = Math.max(newLineN, newLineR);
  return ret < 0 ? 0 : ret;
}

/**
 * Return the text from the start of the current line up to `index`.
 *
 * @param {string} content
 * @param {number} index
 * @returns {string}
 */
function getPreviousTextLine(content, index) {
  const startNewLine = getPreviousLineIndex(content, index);
  return content.substring(startNewLine, index);
}

/**
 * Count the number of leading '#' characters in `input`.
 *
 * @param {string} input
 * @returns {number}  0–6
 */
function findFirstHeaderMark(input) {
  let ret = 0;
  for (let i = 0; ret < 6 && i < input.length; i++) {
    const ch = input.charAt(i);
    if (ch === '#') {
      ret++;
    } else if (ret > 0) {
      break;
    }
  }
  return ret;
}

/**
 * Insert / replace a header marker at the start of the current line.
 *
 * @param {number} n             Target header level (1–6).
 * @param {string} content       Full textarea content.
 * @param {number} cursorPos     Current cursor / selectionStart.
 * @param {number} currentHeader Existing header level at cursor line (0 = none).
 * @returns {string}  New content string.
 */
function buildHeaderLine(n, content, cursorPos, currentHeader) {
  const newHeader = '#'.repeat(n);
  let lineStart = getPreviousLineIndex(content, cursorPos);

  if (currentHeader === 0) {
    // No existing header — insert before the line
    const insertAt = lineStart > 0 ? lineStart + 1 : lineStart;
    const pre  = content.substring(0, insertAt);
    const post = content.substring(insertAt);
    return `${pre}${newHeader} ${post}`;
  } else {
    // Replace existing header
    const oldHeader    = '#'.repeat(currentHeader);
    const oldHeaderLoc = content.indexOf(oldHeader, lineStart);
    const pre  = content.substring(0, oldHeaderLoc);
    const post = content.substring(oldHeaderLoc + oldHeader.length);
    return `${pre}${newHeader}${post}`;
  }
}

/**
 * Build a markdown table skeleton.
 *
 * @param {number} rows  Number of data rows (>= 1).
 * @param {number} cols  Number of columns (>= 1).
 * @returns {string}
 */
function buildTableMarkdown(rows, cols) {
  let headerRow = '|   Header 1    | ';
  let headerBrk = '| ------------- | ';
  let contentCl = '| Content cell  | ';

  for (let x = 1; x < cols; x++) {
    headerRow += `  Header ${x + 1}    | `;
    headerBrk += '------------- | ';
    contentCl += 'Content cell  | ';
  }

  let dataRows = '';
  if (rows > 0) {
    const rowLine = contentCl;
    const lines = [rowLine];
    for (let y = 1; y < rows; y++) {
      lines.push(rowLine);
    }
    dataRows = lines.join('\n');
  }

  return `\n\n${headerRow}\n${headerBrk}\n${dataRows}\n`;
}

/**
 * Build image markdown.
 *
 * @param {string} url      Image URL.
 * @param {string} caption  Caption / title text.
 * @param {string} alt      Alt text.
 * @param {string} goToLink Optional browser link URL (wraps image in an anchor).
 * @returns {string}
 */
function buildImageMarkdown(url, caption, alt, goToLink) {
  const safeCaption = (caption || '').replace('"', '');
  if (goToLink) {
    return `[![${alt}](${goToLink})](${url} "${safeCaption}" =100x)`;
  }
  return `![${alt}](${url} "${safeCaption}")`;
}

/**
 * Build link markdown.
 *
 * @param {string} url         Link URL.
 * @param {string} displayText Display text.
 * @returns {string}
 */
function buildLinkMarkdown(url, displayText) {
  return `[${displayText}](${url})`;
}

/**
 * Detect the formatting state at the current cursor position.
 *
 * @param {string} content   Full textarea content.
 * @param {number} selStart  Selection start.
 * @param {number} selEnd    Selection end.
 * @returns {{ isBold: boolean, isItalic: boolean, isCode: boolean, isStrike: boolean, isHeader: number }}
 */
function calibrateState(content, selStart, selEnd) {
  // Collect all marker positions
  function findAllMarks(marker) {
    const positions = [];
    for (let start = 0, idx = content.indexOf(marker, start); idx !== -1; idx = content.indexOf(marker, start)) {
      positions.push(idx);
      start = idx + marker.length;
    }
    return positions;
  }

  const doubleBoth = findAllMarks('******');
  const doubleB    = findAllMarks('****').filter(n => !doubleBoth.includes(n));
  const bAndI      = findAllMarks('***').filter(n => !doubleB.includes(n));
  let   b          = findAllMarks('**');
  let   i          = findAllMarks('*');
  const code       = findAllMarks('`');
  const strike     = findAllMarks('~~');

  b = b.filter(n => !bAndI.includes(n));
  i = i.filter(n => !bAndI.includes(n) && !b.includes(n) && !b.includes(n - 1));

  b = b.concat(bAndI).sort((a, b) => a - b);
  i = i.concat(bAndI).sort((a, b) => a - b);

  // Check if cursor is inside a pair of markers
  function insidePair(positions, startOffset, endOffset) {
    for (let idx = 0; idx < positions.length - 1; idx += 2) {
      const lower = positions[idx];
      const upper = positions[idx + 1];
      if (selStart > lower + startOffset && selEnd <= upper + endOffset) {
        return true;
      }
    }
    return false;
  }

  const isBold   = insidePair(b,      1, 0);
  const isItalic = insidePair(i,      0, -1);
  const isCode   = insidePair(code,   0, -1);
  const isStrike = insidePair(strike, 1, 0);

  // Header detection
  let isHeader = 0;
  let selectionCrossLine = false;
  for (let k = selStart; !selectionCrossLine && k < selEnd; k++) {
    if (content.charAt(k) === '\n') selectionCrossLine = true;
  }
  if (!selectionCrossLine) {
    const line = getPreviousTextLine(content, selStart);
    isHeader = findFirstHeaderMark(line);
  }

  return { isBold, isItalic, isCode, isStrike, isHeader };
}

/**
 * Compute the panel width for a given container width.
 *
 * @param {number}  w           Container width in pixels.
 * @param {boolean} showPreview Whether the preview panel is visible.
 * @returns {number}  Panel width in pixels.
 */
function setWidth(w, showPreview) {
  if (showPreview && w >= 640) {
    return (w / 2) - 30;
  }
  return w - 30;
}

// Export pure helpers for testing
window._mdEditorUtils = {
  toggleCodeStat,
  buildHeaderLine,
  buildTableMarkdown,
  buildImageMarkdown,
  buildLinkMarkdown,
  calibrateState,
  setWidth,
  getPreviousLineIndex,
  getPreviousTextLine,
  findFirstHeaderMark,
};

// ---------------------------------------------------------------------------
// Editor initialisation
// ---------------------------------------------------------------------------

/**
 * Initialise a markdown editor instance.
 *
 * @param {string} editorId       Scopes all element IDs and the window binding variable.
 * @param {string} initialContent Pre-populated markdown content (may be overridden by
 *                                window[editorId + '_content'] if already set).
 */
function initMarkdownEditor(editorId, initialContent) {

  // ---- Element references ------------------------------------------------
  const textarea    = document.getElementById(editorId + '-textarea');
  const preview     = document.getElementById(editorId + '-preview');
  const insertPanel = document.getElementById(editorId + '-insert-panel');
  const container   = document.getElementById(editorId + '-editor');

  if (!textarea) {
    console.error('initMarkdownEditor: textarea element not found for id', editorId);
    return;
  }

  // ---- Closure-scoped state ----------------------------------------------
  let isBold        = false;
  let isItalic      = false;
  let isCode        = false;
  let isStrike      = false;
  let isHeader      = 0;       // 0 = none, 1–6 = H1–H6
  let insertMode    = 0;       // 0 = closed, 1 = picture, 2 = link, 3 = table
  let prevInsertMode = 0;
  let resourceLink  = '';
  let displayText   = '';
  let backupText    = '';
  let goToLink      = '';
  let rowAdd        = 1;
  let colAdd        = 1;

  // ---- Initialise content ------------------------------------------------
  const windowKey = editorId + '_content';
  const startContent = (window[windowKey] !== undefined && window[windowKey] !== null)
    ? window[windowKey]
    : (initialContent || '');

  textarea.value    = startContent;
  window[windowKey] = startContent;

  // ---- Preview -----------------------------------------------------------

  function updatePreview() {
    if (!preview) return;
    if (typeof marked !== 'undefined') {
      preview.innerHTML = marked.parse(textarea.value);
    } else {
      preview.textContent = textarea.value;
    }
  }

  // ---- Responsive layout -------------------------------------------------

  function applyWidth() {
    if (!container) return;
    const w = container.offsetWidth;
    const panelW = setWidth(w, true);
    const editorSide  = document.getElementById(editorId + '-editor-side');
    const previewSide = document.getElementById(editorId + '-preview-side');
    if (editorSide)  editorSide.style.width  = panelW + 'px';
    if (previewSide) previewSide.style.width = panelW + 'px';
  }

  window.addEventListener('resize', applyWidth);

  // ---- Toolbar image helpers ---------------------------------------------

  function imgEl(name) {
    return document.getElementById(editorId + '-btn-' + name);
  }

  function updateButtonImages() {
    const map = {
      bold:   isBold,
      italic: isItalic,
      code:   isCode,
      strike: isStrike,
    };
    for (const [name, active] of Object.entries(map)) {
      const el = imgEl(name);
      if (el) {
        const capName = name.charAt(0).toUpperCase() + name.slice(1);
        el.src = active
          ? `md-images/sel-${capName}.png`
          : `md-images/reg-${capName}.png`;
      }
    }
    // Header buttons H1–H6
    for (let h = 1; h <= 6; h++) {
      const el = imgEl('h' + h);
      if (el) {
        el.src = (isHeader === h)
          ? `md-images/sel-H${h}.png`
          : `md-images/reg-H${h}.png`;
      }
    }
  }

  // ---- Calibrate state ---------------------------------------------------

  function runCalibrateState() {
    const state = calibrateState(textarea.value, textarea.selectionStart, textarea.selectionEnd);
    isBold   = state.isBold;
    isItalic = state.isItalic;
    isCode   = state.isCode;
    isStrike = state.isStrike;
    isHeader = state.isHeader;
    updateButtonImages();
  }

  textarea.addEventListener('focus',    runCalibrateState);
  textarea.addEventListener('click',    runCalibrateState);
  textarea.addEventListener('keyup',    runCalibrateState);

  // ---- Textarea input event ----------------------------------------------

  textarea.addEventListener('input', () => {
    window[windowKey] = textarea.value;
    updatePreview();
  });

  // ---- Insert panel helpers ----------------------------------------------

  function showInsertSection(mode) {
    if (!insertPanel) return;
    insertPanel.style.display = 'block';
    // Show only the relevant sub-section
    const sections = { 1: 'picture', 2: 'link', 3: 'table' };
    for (const [m, name] of Object.entries(sections)) {
      const el = document.getElementById(editorId + '-insert-' + name);
      if (el) el.style.display = (parseInt(m) === mode) ? 'block' : 'none';
    }
  }

  function hideInsertPanel() {
    if (insertPanel) insertPanel.style.display = 'none';
    insertMode = 0;
  }

  function insertAtCursor(text) {
    const start = textarea.selectionStart;
    const end   = textarea.selectionEnd;
    const before = textarea.value.substring(0, start);
    const after  = textarea.value.substring(end);
    textarea.value    = before + text + after;
    window[windowKey] = textarea.value;
    const newPos = start + text.length;
    textarea.setSelectionRange(newPos, newPos);
    textarea.focus();
    updatePreview();
  }

  // ---- Toolbar click handlers --------------------------------------------

  function onClickBold() {
    textarea.focus();
    const result = toggleCodeStat('**', isBold, textarea.value, textarea.selectionStart, textarea.selectionEnd);
    textarea.value    = result.content;
    window[windowKey] = result.content;
    textarea.setSelectionRange(result.selStart, result.selEnd);
    isBold = !isBold;
    updateButtonImages();
    updatePreview();
  }

  function onClickItalic() {
    textarea.focus();
    const result = toggleCodeStat('*', isItalic, textarea.value, textarea.selectionStart, textarea.selectionEnd);
    textarea.value    = result.content;
    window[windowKey] = result.content;
    textarea.setSelectionRange(result.selStart, result.selEnd);
    isItalic = !isItalic;
    updateButtonImages();
    updatePreview();
  }

  function onClickStrike() {
    textarea.focus();
    const result = toggleCodeStat('~~', isStrike, textarea.value, textarea.selectionStart, textarea.selectionEnd);
    textarea.value    = result.content;
    window[windowKey] = result.content;
    textarea.setSelectionRange(result.selStart, result.selEnd);
    isStrike = !isStrike;
    updateButtonImages();
    updatePreview();
  }

  function onClickCode() {
    textarea.focus();
    const result = toggleCodeStat('`', isCode, textarea.value, textarea.selectionStart, textarea.selectionEnd);
    textarea.value    = result.content;
    window[windowKey] = result.content;
    textarea.setSelectionRange(result.selStart, result.selEnd);
    isCode = !isCode;
    updateButtonImages();
    updatePreview();
  }

  function onClickHeader(n) {
    textarea.focus();
    const newContent = buildHeaderLine(n, textarea.value, textarea.selectionStart, isHeader);
    textarea.value    = newContent;
    window[windowKey] = newContent;
    isHeader = n;
    updateButtonImages();
    updatePreview();
  }

  function onClickPicture() {
    if (prevInsertMode !== 1) {
      displayText  = '';
      resourceLink = 'https://';
      goToLink     = '';
      backupText   = '';
      // Sync input fields
      _syncInsertFields();
    }
    insertMode     = 1;
    prevInsertMode = 1;
    showInsertSection(1);
  }

  function onClickLink() {
    if (prevInsertMode !== 2) {
      displayText  = '';
      resourceLink = 'https://';
      goToLink     = '';
      backupText   = '';
      _syncInsertFields();
    }
    insertMode     = 2;
    prevInsertMode = 2;
    showInsertSection(2);
  }

  function onClickTable() {
    insertMode     = 3;
    prevInsertMode = 3;
    showInsertSection(3);
  }

  function onCancel() {
    hideInsertPanel();
  }

  // ---- Insert panel submit handlers --------------------------------------

  function onInsertImage() {
    _readInsertFields();
    try {
      new URL(resourceLink);
    } catch (e) {
      console.error('Image URL error:', e);
      alert('Invalid Image URL Provided');
      return;
    }

    try {
      if (goToLink) new URL(goToLink);
    } catch (_) {
      alert('Invalid Browser Link provided! Will be empty!');
      goToLink = '';
    }

    const text = buildImageMarkdown(resourceLink, displayText, backupText, goToLink);
    hideInsertPanel();
    insertAtCursor(text);
  }

  function onInsertLink() {
    _readInsertFields();
    try {
      new URL(resourceLink);
    } catch (e) {
      console.error('Link URL error:', e);
      alert('Invalid Link URL Provided');
      return;
    }

    const text = buildLinkMarkdown(resourceLink, displayText);
    hideInsertPanel();
    insertAtCursor(text);
  }

  function onInsertTable() {
    _readInsertFields();
    const text = buildTableMarkdown(rowAdd, colAdd);
    hideInsertPanel();
    insertAtCursor(text);
  }

  // ---- Sync insert panel form fields to/from state -----------------------

  function _syncInsertFields() {
    const ids = [
      editorId + '-input-resource-link',
      editorId + '-input-display-text',
      editorId + '-input-backup-text',
      editorId + '-input-go-to-link',
    ];
    for (const id of ids) {
      const el = document.getElementById(id);
      if (el) el.value = '';
    }
    const rl = document.getElementById(editorId + '-input-resource-link');
    if (rl) rl.value = resourceLink;
  }

  function _readInsertFields() {
    const rl = document.getElementById(editorId + '-input-resource-link');
    const dt = document.getElementById(editorId + '-input-display-text');
    const bt = document.getElementById(editorId + '-input-backup-text');
    const gl = document.getElementById(editorId + '-input-go-to-link');
    const rw = document.getElementById(editorId + '-input-rows');
    const cl = document.getElementById(editorId + '-input-cols');

    if (rl) resourceLink = rl.value;
    if (dt) displayText  = dt.value;
    if (bt) backupText   = bt.value;
    if (gl) goToLink     = gl.value;
    if (rw) rowAdd       = parseInt(rw.value, 10) || 1;
    if (cl) colAdd       = parseInt(cl.value, 10) || 1;
  }

  // ---- Wire up toolbar buttons -------------------------------------------

  function wire(suffix, handler) {
    const el = document.getElementById(editorId + '-btn-' + suffix);
    if (el) el.addEventListener('click', handler);
  }

  wire('bold',   onClickBold);
  wire('italic', onClickItalic);
  wire('strike', onClickStrike);
  wire('code',   onClickCode);
  wire('picture', onClickPicture);
  wire('link',   onClickLink);
  wire('table',  onClickTable);
  wire('cancel', onCancel);

  for (let h = 1; h <= 6; h++) {
    (function(n) { wire('h' + n, () => onClickHeader(n)); })(h);
  }

  // ---- Wire up insert panel submit buttons --------------------------------

  function wireInsert(suffix, handler) {
    const el = document.getElementById(editorId + '-submit-' + suffix);
    if (el) el.addEventListener('click', handler);
  }

  wireInsert('image', onInsertImage);
  wireInsert('link',  onInsertLink);
  wireInsert('table', onInsertTable);

  // ---- Initial render ----------------------------------------------------

  updatePreview();
  setTimeout(applyWidth, 0);
}
