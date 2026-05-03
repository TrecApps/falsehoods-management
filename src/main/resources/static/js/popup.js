

function showPopup(popupId) {
    const el = document.getElementById(sliderId);
    if (el === null) return;
    el.hidden = false;
}

function hidePopup(popupId) {
    const el = document.getElementById(sliderId);
    if (el === null) return;
    el.hidden = true;
}