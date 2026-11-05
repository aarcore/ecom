import { useEffect, useState } from 'react';

/**
 * Horizontal snap carousel with dot indicators. Pure CSS scroll-snap so it
 * feels native on touch devices and needs no gesture library.
 */
export function HeroCarousel({
  images,
  onTap,
}: {
  images: { id: number; image: string; title?: string }[];
  onTap: (index: number) => void;
}) {
  const [index, setIndex] = useState(0);

  useEffect(() => {
    if (images.length <= 1) return;
    const timer = window.setInterval(() => {
      setIndex((prev) => (prev + 1) % images.length);
    }, 4200);
    return () => window.clearInterval(timer);
  }, [images.length]);

  if (!images.length) {
    return <div className="hero skel" />;
  }

  return (
    <div style={{ position: 'relative' }}>
      <div
        style={{
          display: 'flex',
          overflowX: 'auto',
          scrollSnapType: 'x mandatory',
          scrollbarWidth: 'none',
          borderRadius: 18,
        }}
        onScroll={(event) => {
          const el = event.currentTarget;
          const next = Math.round(el.scrollLeft / el.clientWidth);
          if (next !== index) setIndex(next);
        }}
      >
        {images.map((banner, i) => (
          <div
            key={banner.id}
            className="hero"
            style={{ minWidth: '100%', scrollSnapAlign: 'start', borderRadius: 0 }}
            onClick={() => onTap(i)}
          >
            <img src={banner.image} alt={banner.title ?? ''} />
          </div>
        ))}
      </div>
      <div className="hero-dots">
        {images.map((banner, i) => (
          <i key={banner.id} className={i === index ? 'on' : ''} />
        ))}
      </div>
    </div>
  );
}
