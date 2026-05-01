const COLLAPSED_CLASS = 'container-slider-collapse';
const EXPANDED_CLASS = 'container-slider-expanded';

function showSlider(sliderId) {
    const el = document.getElementById(sliderId);
    if (el === null) return;
    el.classList.remove(COLLAPSED_CLASS);
    el.classList.add(EXPANDED_CLASS);
}

function hideSlider(sliderId) {
    const el = document.getElementById(sliderId);
    if (el === null) return;
    el.classList.remove(EXPANDED_CLASS);
    el.classList.add(COLLAPSED_CLASS);
}
