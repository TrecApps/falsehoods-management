

function showPopup(popupId) {
    const el = document.getElementById(popupId);
    if (el === null) return;
    el.hidden = false;
}

function hidePopup(popupId) {
    const el = document.getElementById(popupId);
    if (el === null) return;
    el.hidden = true;
}