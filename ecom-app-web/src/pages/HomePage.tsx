import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '@/api/client';
import { IconArrowRight, IconRefresh, IconShield, IconTruck } from '@/components/icons';
import { Masthead, type MastheadSlide } from '@/components/Masthead';
import { Photo } from '@/components/Photo';
import { Empty, Price, ProductCard, SectionHead } from '@/components/ui';
import { useT } from '@/store/locale';
import { useLocale } from '@/store/locale';
import type { HomeData } from '@/types';

/**
 * Copy for each carousel slot.
 *
 * The banner rows in the database only carry an image and a link, so the
 * headline is storefront copy rather than merchandising data — which makes it
 * the frontend's job to translate. Slots are addressed by index, and the array
 * is keyed off the dictionary so switching language needs no remount.
 */
const SLIDE_COPY = [
  {
    kicker: 'slide.1.kicker',
    title: 'slide.1.title',
    accent: 'slide.1.accent',
    sub: 'slide.1.sub',
    cta: { label: 'slide.1.cta', to: '/shop?categoryId=1' },
    secondary: { label: 'slide.1.secondary', to: '/shop' },
  },
  {
    kicker: 'slide.2.kicker',
    title: 'slide.2.title',
    accent: 'slide.2.accent',
    sub: 'slide.2.sub',
    cta: { label: 'slide.2.cta', to: '/shop?categoryId=12' },
    secondary: { label: 'slide.2.secondary', to: '/shop?categoryId=6' },
  },
  {
    kicker: 'slide.3.kicker',
    title: 'slide.3.title',
    accent: 'slide.3.accent',
    sub: 'slide.3.sub',
    cta: { label: 'slide.3.cta', to: '/shop?categoryId=2' },
    secondary: { label: 'slide.3.secondary', to: '/coupons' },
  },
  {
    kicker: 'slide.4.kicker',
    title: 'slide.4.title',
    accent: 'slide.4.accent',
    sub: 'slide.4.sub',
    cta: { label: 'slide.4.cta', to: '/shop?categoryId=51' },
    secondary: { label: 'slide.4.secondary', to: '/shop?categoryId=52' },
  },
] as const;

export default function HomePage() {
  const navigate = useNavigate();
  const { t } = useT();
  // product/category content comes back already translated, so a reload is
  // required when the language changes rather than just a re-render
  const locale = useLocale((s) => s.locale);
  const [data, setData] = useState<HomeData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setData(null);
    setError(null);
    api
      .get<HomeData>('/app/home')
      .then(setData)
      .catch((err: Error) => setError(err.message));
  }, [locale]);

  /** Fallback slide used until the banner payload arrives, so the masthead never flashes empty. */
  const fallback: MastheadSlide[] = useMemo(
    () => [
      {
        id: -1,
        kicker: t('slide.fallback.kicker'),
        title: t('slide.fallback.title'),
        accent: t('slide.fallback.accent'),
        sub: t('slide.fallback.sub'),
        image: '/images/hero/hero-1.jpg',
        cta: { label: t('slide.fallback.cta'), to: '/shop' },
        secondary: { label: t('slide.fallback.secondary'), to: '/shop?sort=newest' },
      },
    ],
    [t],
  );

  /** Banners become slides; product/category banners reuse the hero art. */
  const slides: MastheadSlide[] = useMemo(() => {
    if (!data?.banners?.length) return fallback;

    return data.banners.slice(0, 4).map((banner, i) => {
      const copy = SLIDE_COPY[i % SLIDE_COPY.length];
      return {
        id: banner.id,
        image: banner.image,
        kicker: t(copy.kicker),
        title: t(copy.title),
        accent: t(copy.accent),
        sub: t(copy.sub),
        cta: { label: t(copy.cta.label), to: copy.cta.to },
        secondary: { label: t(copy.secondary.label), to: copy.secondary.to },
      };
    });
  }, [data, t, fallback]);

  if (error) {
    return (
      <div className="shell" style={{ paddingTop: 80 }}>
        <Empty
          glyph="×"
          title={t('home.error')}
          description={error}
          action={
            <button className="btn btn-primary" onClick={() => window.location.reload()}>
              {t('common.retry')}
            </button>
          }
        />
      </div>
    );
  }

  if (!data) return <HomeSkeleton />;

  const lead = data.recommend[0];

  return (
    <>
      <Masthead slides={slides} />

      {/* ------------------------------------------------------- promise strip */}
      <div className="promise-strip">
        <div className="shell">
          <div className="promise-inner">
            <div className="item">
              <IconTruck />
              <div>
                <strong>{t('home.promise.shipping')}</strong>
                <span>{t('home.promise.shippingHint')}</span>
              </div>
            </div>
            <div className="item">
              <IconRefresh />
              <div>
                <strong>{t('home.promise.returns')}</strong>
                <span>{t('home.promise.returnsHint')}</span>
              </div>
            </div>
            <div className="item">
              <IconShield />
              <div>
                <strong>{t('home.promise.authorised')}</strong>
                <span>{t('home.promise.authorisedHint')}</span>
              </div>
            </div>
            <div className="item">
              <IconArrowRight />
              <div>
                <strong>{t('home.promise.assembly')}</strong>
                <span>{t('home.promise.assemblyHint')}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ---------------------------------------------------------- categories */}
      <section className="shell section">
        <SectionHead
          title={t('home.categories.title')}
          aside={t('home.categories.aside')}
          action={{ label: t('home.categories.action'), to: '/shop' }}
          numeral="01"
        />
        <div className="category-rail">
          {data.categories.slice(0, 6).map((cat, i) => (
            <button
              key={cat.id}
              className="category-tile"
              style={{ animationDelay: `${i * 60}ms` }}
              onClick={() => navigate(`/shop?categoryId=${cat.id}&name=${encodeURIComponent(cat.name)}`)}
            >
              {cat.banner ? (
                <Photo src={cat.banner} loading="lazy" />
              ) : (
                <div className="skel" style={{ position: 'absolute', inset: 0 }} />
              )}
              <span className="idx">{String(i + 1).padStart(2, '0')}</span>
              <div className="label">
                <strong>{cat.name}</strong>
                <span>{t('common.pieces', { count: cat.productCount ?? 0 })}</span>
              </div>
            </button>
          ))}
        </div>
      </section>

      {/* -------------------------------------------------------------- picks */}
      <section className="shell section">
        <SectionHead
          title={t('home.picks.title')}
          aside={t('home.picks.aside')}
          action={{ label: t('home.picks.action'), to: '/shop?sort=sales' }}
          numeral="02"
        />
        <div className="product-grid">
          {data.recommend.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </section>

      {/* ----------------------------------------------------------- spotlight */}
      {lead && (
        <section className="spotlight">
          <div className="spotlight-media">
            <Photo src={lead.imageList?.[0] ?? lead.cover} alt={lead.name} loading="lazy" />
          </div>
          <div className="spotlight-copy">
            <div className="eyebrow">{t('home.spotlight.eyebrow')}</div>
            <h2>{lead.name}</h2>
            <p>{lead.subtitle ?? lead.detail ?? t('home.spotlight.fallback')}</p>
            <div className="spotlight-price">
              <Price value={lead.price} size={44} accent />
              {lead.originalPrice && Number(lead.originalPrice) > Number(lead.price) && (
                <span className="price-was">¥{Number(lead.originalPrice).toFixed(2)}</span>
              )}
            </div>
            <button className="btn btn-primary" onClick={() => navigate(`/product/${lead.id}`)}>
              {t('home.spotlight.cta')}
              <IconArrowRight />
            </button>
          </div>
        </section>
      )}

      {/* ------------------------------------------------------------- new in */}
      <section className="shell section">
        <SectionHead
          title={t('home.new.title')}
          aside={t('home.new.aside')}
          action={{ label: t('home.new.action'), to: '/shop?sort=newest' }}
          numeral="03"
        />
        <div className="product-grid">
          {data.newArrivals.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </section>

      {/* ------------------------------------------------------------ big band */}
      {data.banners[0] && (
        <section className="wide-band">
          <Photo src={data.banners[0].image} loading="lazy" />
          <div className="wide-band-copy">
            <div className="eyebrow">{t('home.band.eyebrow')}</div>
            <h2>
              {t('home.band.title')
                .split('\n')
                .map((line, i, all) => (
                  <span key={line}>
                    {line}
                    {i < all.length - 1 && <br />}
                  </span>
                ))}
            </h2>
            <button className="btn btn-primary" onClick={() => navigate('/shop?categoryId=2')}>
              {t('home.band.cta')}
              <IconArrowRight />
            </button>
          </div>
        </section>
      )}

      {/* ---------------------------------------------------------------- hot */}
      <section className="shell section">
        <SectionHead
          title={t('home.hot.title')}
          aside={t('home.hot.aside')}
          action={{ label: t('home.hot.action'), to: '/shop?sort=sales' }}
          numeral="04"
        />
        <div className="product-grid">
          {data.hot.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </section>

      {/* -------------------------------------------------------------- join */}
      <section className="join-band">
        <div className="shell">
          <div className="join-inner">
            <div>
              <div className="eyebrow">{t('home.join.eyebrow')}</div>
              <h2>
                {t('home.join.title')
                  .split('\n')
                  .map((line, i, all) => (
                    <span key={line}>
                      {line}
                      {i < all.length - 1 && <br />}
                    </span>
                  ))}
              </h2>
            </div>
            <div>
              <p>{t('home.join.body')}</p>
              <button
                className="btn btn-primary"
                style={{ marginTop: 8 }}
                onClick={() => navigate('/login')}
              >
                {t('home.join.cta')}
                <IconArrowRight />
              </button>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}

function HomeSkeleton() {
  return (
    <div>
      <div className="masthead" style={{ background: 'var(--plate)' }}>
        <div className="masthead-inner">
          <div className="skel" style={{ height: 14, width: 180, marginBottom: 24 }} />
          <div className="skel" style={{ height: 76, width: '64%', marginBottom: 14 }} />
          <div className="skel" style={{ height: 76, width: '44%', marginBottom: 30 }} />
          <div className="skel" style={{ height: 20, width: '50%' }} />
        </div>
      </div>
      <div className="shell" style={{ marginTop: 72 }}>
        <div className="product-grid">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} style={{ background: 'var(--surface-2)' }}>
              <div className="skel" style={{ aspectRatio: '1 / 1.12' }} />
              <div style={{ padding: 16 }}>
                <div className="skel" style={{ height: 16, marginBottom: 8 }} />
                <div className="skel" style={{ height: 16, width: '60%' }} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
