import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { IconArrowRight } from './icons';
import { Photo } from './Photo';
import { useT } from '@/store/locale';

export interface MastheadSlide {
  id: number;
  title: string;
  /** Rendered in the accent colour on a second line. */
  accent?: string;
  kicker: string;
  sub?: string;
  image: string;
  cta?: { label: string; to: string };
  secondary?: { label: string; to: string };
}

const AUTOPLAY_MS = 6500;

/**
 * Full-bleed hero carousel.
 *
 * Deliberate choices:
 *  - the image owns the full viewport width; nothing is boxed in a card
 *  - copy rises with a stagger each time a slide becomes active, so every
 *    rotation feels like an entrance rather than a silent crossfade
 *  - autoplay pauses on hover and on keyboard focus, and never runs when the
 *    visitor prefers reduced motion
 *  - the right-edge rail is a real tablist: arrow keys move between slides
 */
export function Masthead({ slides }: { slides: MastheadSlide[] }) {
  const navigate = useNavigate();
  const { t } = useT();
  const [index, setIndex] = useState(0);
  const [paused, setPaused] = useState(false);
  const timer = useRef<number | null>(null);
  const count = slides.length;

  const go = useCallback(
    (next: number) => {
      if (count === 0) return;
      setIndex(((next % count) + count) % count);
    },
    [count],
  );

  useEffect(() => {
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (paused || reduced || count <= 1) return;
    timer.current = window.setInterval(() => setIndex((i) => (i + 1) % count), AUTOPLAY_MS);
    return () => {
      if (timer.current) window.clearInterval(timer.current);
    };
  }, [paused, count]);

  // touch swipe
  const touchX = useRef<number | null>(null);
  const onTouchStart = (e: React.TouchEvent) => {
    touchX.current = e.touches[0].clientX;
  };
  const onTouchEnd = (e: React.TouchEvent) => {
    if (touchX.current == null) return;
    const delta = e.changedTouches[0].clientX - touchX.current;
    if (Math.abs(delta) > 48) go(index + (delta < 0 ? 1 : -1));
    touchX.current = null;
  };

  if (count === 0) return null;

  return (
    <section
      className="masthead"
      aria-roledescription="carousel"
      aria-label={t('masthead.label')}
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={() => setPaused(false)}
      onTouchStart={onTouchStart}
      onTouchEnd={onTouchEnd}
    >
      {slides.map((slide, i) => (
        <div
          key={slide.id}
          className={`masthead-slide ${i === index ? 'active' : ''}`}
          aria-hidden={i !== index}
          role="group"
          aria-roledescription="slide"
          aria-label={`${i + 1} / ${count}`}
        >
          <Photo className="art" src={slide.image} alt="" draggable={false} />

          <div className="masthead-inner">
            <div className="masthead-copy">
              <div className="masthead-kicker">{slide.kicker}</div>
              <h1 className="masthead-title">
                {slide.title}
                {slide.accent && (
                  <>
                    <br />
                    <em>{slide.accent}</em>
                  </>
                )}
              </h1>
              {slide.sub && <p className="masthead-sub">{slide.sub}</p>}
              <div className="masthead-actions">
                {slide.cta && (
                  <button className="btn btn-primary" onClick={() => navigate(slide.cta!.to)}>
                    {slide.cta.label}
                    <IconArrowRight />
                  </button>
                )}
                {slide.secondary && (
                  <button className="btn btn-outline" onClick={() => navigate(slide.secondary!.to)}>
                    {slide.secondary.label}
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      ))}

      {/* right-edge tablist */}
      <div className="masthead-rail" role="tablist" aria-label={t('masthead.tabs')}>
        {slides.map((slide, i) => (
          <button
            key={slide.id}
            role="tab"
            aria-selected={i === index}
            aria-label={slide.kicker}
            className={i === index ? 'on' : ''}
            onClick={() => go(i)}
            onKeyDown={(e) => {
              if (e.key === 'ArrowDown' || e.key === 'ArrowRight') {
                e.preventDefault();
                go(index + 1);
              }
              if (e.key === 'ArrowUp' || e.key === 'ArrowLeft') {
                e.preventDefault();
                go(index - 1);
              }
            }}
          >
            <span className="bar" />
            <span className="idx">{String(i + 1).padStart(2, '0')}</span>
          </button>
        ))}
      </div>

      {/* bottom strip: counter + scroll cue */}
      <div className="masthead-foot">
        <div className="inner">
          <div className="masthead-counter">
            <strong>{String(index + 1).padStart(2, '0')}</strong>
            <span>/ {String(count).padStart(2, '0')}</span>
          </div>
          <button className="scroll-cue" onClick={() => window.scrollBy({ top: window.innerHeight * 0.8, behavior: 'smooth' })}>
            {t('masthead.scroll')}
            <span className="line" />
          </button>
        </div>
      </div>
    </section>
  );
}
