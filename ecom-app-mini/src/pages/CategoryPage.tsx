import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, attempt } from '@/api/client';
import { ProductCard } from '@/components/ui';
import type { Category, Product } from '@/types';
import type { PageResult } from '@/api/client';

const SORTS = [
  { key: 'newest', label: '综合' },
  { key: 'sales', label: '销量' },
  { key: 'price_asc', label: '价格↑' },
  { key: 'price_desc', label: '价格↓' },
];

export default function CategoryPage() {
  const navigate = useNavigate();
  const [tree, setTree] = useState<Category[]>([]);
  const [activeRoot, setActiveRoot] = useState<number | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(false);
  const [sort, setSort] = useState('newest');

  useEffect(() => {
    api
      .get<Category[]>('/app/categories')
      .then((data) => {
        setTree(data);
        // preselect the largest root category so the right pane is never empty
        const first = data[0] ?? null;
        if (first) setActiveRoot(first.id);
      })
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    if (activeRoot == null) return;
    setLoading(true);
    attempt(() =>
      api
        .get<PageResult<Product>>('/app/products', { categoryId: activeRoot, sort, page: 1, limit: 20 })
        .then((result) => {
          setProducts(result.records);
          return result;
        }),
    ).finally(() => setLoading(false));
  }, [activeRoot, sort]);

  const active = useMemo(() => tree.find((c) => c.id === activeRoot) ?? null, [tree, activeRoot]);

  return (
    <div style={{ paddingBottom: 24 }}>
      <div className="topbar">
        <div className="title">商品分类</div>
      </div>

      <div style={{ display: 'flex', minHeight: 'calc(100vh - 106px)' }}>
        {/* left rail */}
        <div
          style={{
            width: 92,
            flexShrink: 0,
            background: '#f2f3f7',
            overflowY: 'auto',
            paddingBottom: 20,
          }}
        >
          {tree.map((cat) => (
            <button
              key={cat.id}
              onClick={() => setActiveRoot(cat.id)}
              style={{
                width: '100%',
                border: 'none',
                background: cat.id === activeRoot ? '#fff' : 'transparent',
                padding: '14px 8px',
                fontSize: 12.5,
                color: cat.id === activeRoot ? 'var(--brand)' : '#4a5160',
                fontWeight: cat.id === activeRoot ? 600 : 400,
                cursor: 'pointer',
                position: 'relative',
              }}
            >
              {cat.id === activeRoot && (
                <span
                  style={{
                    position: 'absolute',
                    left: 0,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    width: 3,
                    height: 18,
                    borderRadius: 3,
                    background: 'var(--brand)',
                  }}
                />
              )}
              <div style={{ fontSize: 17, marginBottom: 2 }}>{cat.icon || '📦'}</div>
              {cat.name}
            </button>
          ))}
        </div>

        {/* right pane */}
        <div style={{ flex: 1, padding: '12px 12px 24px', background: '#fff', minWidth: 0 }}>
          {active?.banner && (
            <img
              src={active.banner}
              alt={active.name}
              style={{ width: '100%', borderRadius: 12, marginBottom: 12, background: '#f2f4f8' }}
            />
          )}

          <div
            style={{
              display: 'flex',
              gap: 8,
              overflowX: 'auto',
              paddingBottom: 10,
              scrollbarWidth: 'none',
            }}
          >
            {active?.children?.map((child) => (
              <button
                key={child.id}
                className="chip"
                style={{ flexShrink: 0 }}
                onClick={() =>
                  navigate(`/search?categoryId=${child.id}&name=${encodeURIComponent(child.name)}`)
                }
              >
                {child.name}
              </button>
            ))}
          </div>

          <div style={{ display: 'flex', gap: 6, marginBottom: 12 }}>
            {SORTS.map((item) => (
              <button
                key={item.key}
                className={`chip ${sort === item.key ? 'on' : ''}`}
                style={{ padding: '4px 10px', fontSize: 11.5 }}
                onClick={() => setSort(item.key)}
              >
                {item.label}
              </button>
            ))}
          </div>

          {loading ? (
            <div className="product-grid">
              {[0, 1, 2, 3].map((i) => (
                <div key={i} className="skel" style={{ height: 210 }} />
              ))}
            </div>
          ) : products.length ? (
            <div className="product-grid">
              {products.map((product) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  onClick={() => navigate(`/product/${product.id}`)}
                />
              ))}
            </div>
          ) : (
            <div className="empty" style={{ padding: '40px 0' }}>
              <div className="glyph">📭</div>
              <p>该分类暂无商品</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
