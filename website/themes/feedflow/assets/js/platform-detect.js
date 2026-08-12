/* Route the hero call to action to the Android store. */
(function () {
  var ctaLink = document.querySelector('.js-platform-cta');
  var ctaLabel = document.querySelector('.js-cta-label');
  if (!ctaLink || !ctaLabel) return;

  var href = ctaLink.dataset.androidHref;
  ctaLabel.textContent = 'Download for Android';
  if (href) {
    ctaLink.href = href;
    ctaLink.setAttribute('target', '_blank');
    ctaLink.setAttribute('rel', 'noopener');
  }

  var card = document.querySelector('[data-platform="android"]');
  if (card) card.classList.add('platform-card-active');
})();
