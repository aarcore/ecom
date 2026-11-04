import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '@/api/client';
import { HeroCarousel } from '@/components/HeroCarousel';
import { ProductCard } from '@/components/ui';
import type { Banner, HomeData } from '@/types';

const QUICK_LINKS = [
  { label: '整车', glyph: '🚵', to: '/search?categoryId=1&name=山地整车' },
  { label: '热销榜单', glyph: '🔥', to: '/search?sort=sales' },
  { label: '优惠券', glyph: '🎟️', to: '/coupons' },
  { label: '我的订单', glyph: '📦', to: '/orders' },
];

export default function HomePage() {
  const navigate = useNavigate();
  const [data, setData] = useState<HomeData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .get<HomeData>('/app/home')
      .then(setData)
      .catch((err: Error) => setError(err.message));
  }, []);

  const bannerTap = (banner: Banner) => {
    if (banner.linkType === 'product' && banner.linkValue) {
      navigate(`/product/${banner.linkValue}`);
    } else if (banner.linkType === 'category' && banner.linkValue) {
      navigate(`/search?categoryId=${banner.linkValue}`);
    } else if (banner.linkType === 'url' && banner.linkValue) {
      window.location.href = banner.linkValue;
    }
  };

  if (error) {
    return (
      <div className="page">
        <div className="empty">
          <div className="glyph">😵</div>
          <p>{error}</p>
          <button className="btn btn-outline btn-sm" onClick={() => window.location.reload()}>
            重新加载
          </button>
        </div>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="page">
        <div className="skel" style={{ height: 42, marginBottom: 12 }} />
        <div className="hero skel" />
        <div className="skel" style={{ height: 88, marginTop: 14 }} />
        <div className="product-grid" style={{ marginTop: 18 }}>
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="skel" style={{ height: 220 }} />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      {/* search entry */}
      <div className="search-bar" onClick={() => navigate('/search')} style={{ marginBottom: 12 }}>
        <span>🔍</span>
        <span style={{ flex: 1 }}>搜索车型、装备或配件</span>
      </div>

      <HeroCarousel images={data.banners} onTap={(i) => bannerTap(data.banners[i])} />

      {/* quick links */}
      <div className="cat-grid" style={{ marginTop: 12, gridTemplateColumns: 'repeat(4, 1fr)' }}>
        {QUICK_LINKS.map((link) => (
          <button key={link.label} className="cat-item" onClick={() => navigate(link.to)}>
            <span className="bubble">{link.glyph}</span>
            <span>{link.label}</span>
          </button>
        ))}
      </div>

      {/* notice marquee */}
      {data.notices.length > 0 && (
        <div className="notice-strip" style={{ marginTop: 12 }}>
          <span className="tag">公告</span>
          <div className="marquee">
            <span>
              {data.notices.map((n) => n.title).join('　·　')}
            </span>
          </div>
        </div>
      )}

      {/* categories */}
      <div className="section">
        <div className="section-head">
          <h3>全部分类</h3>
          <span className="more" onClick={() => navigate('/category')}>
            查看全部 ›
          </span>
        </div>
        <div className="cat-grid">
          {data.categories.slice(0, 10).map((cat) => (
            <button
              key={cat.id}
              className="cat-item"
              onClick={() => navigate(`/search?categoryId=${cat.id}&name=${encodeURIComponent(cat.name)}`)}
            >
              <span className="bubble">{cat.icon || '📦'}</span>
              <span>{cat.name}</span>
            </button>
          ))}
        </div>
      </div>

      {/* recommended */}
      <div className="section">
        <div className="section-head">
          <h3>为你推荐</h3>
          <span className="more" onClick={() => navigate('/search')}>
            更多 ›
          </span>
        </div>
        <div className="product-grid">
          {data.recommend.map((product) => (
            <ProductCard
              key={product.id}
              product={product}
              onClick={() => navigate(`/product/${product.id}`)}
            />
          ))}
        </div>
      </div>

      {/* new arrivals rail */}
      {data.newArrivals.length > 0 && (
        <div className="section">
          <div className="section-head">
            <h3>新品上架</h3>
            <span className="more" onClick={() => navigate('/search?sort=newest')}>
              更多 ›
            </span>
          </div>
          <div className="rail">
            {data.newArrivals.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                onClick={() => navigate(`/product/${product.id}`)}
              />
            ))}
          </div>
        </div>
      )}

      {/* hot */}
      {data.hot.length > 0 && (
        <div className="section">
          <div className="section-head">
            <h3>热销装备</h3>
            <span className="more" onClick={() => navigate('/search?sort=sales')}>
              更多 ›
            </span>
          </div>
          <div className="product-grid">
            {data.hot.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                onClick={() => navigate(`/product/${product.id}`)}
              />
            ))}
          </div>
        </div>
      )}

      <div style={{ textAlign: 'center', padding: '28px 0 8px' }} className="muted">
        — 山脊单车 · 支持 Web 与微信小程序 —
      </div>
    </div>
  );
}
