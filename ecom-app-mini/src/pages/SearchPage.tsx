import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api, attempt, type PageResult } from '@/api/client';
import { Empty, ProductCard } from '@/components/ui';
import type { Product } from '@/types';

const SORTS = [
  { key: 'newest', label: '综合' },
  { key: 'sales', label: '销量' },
  { key: 'price_asc', label: '价格↑' },
  { key: 'price_desc', label: '价格↓' },
];

export default function SearchPage() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const categoryId = params.get('categoryId') ?? undefined;
  const categoryName = params.get('name') ?? undefined;

  const [keyword, setKeyword] = useState(params.get('keyword') ?? '');
  const [sort, setSort] = useState(params.get('sort') ?? 'newest');
  const [rows, setRows] = useState<Product[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  const load = async (nextPage: number, replace: boolean) => {
    setLoading(true);
    const result = await attempt(() =>
      api.get<PageResult<Product>>('/app/products', {
        keyword: params.get('keyword') || undefined,
        categoryId,
        sort,
        page: nextPage,
        limit: 12,
      }),
    );
    setLoading(false);
    if (!result) return;
    setRows(replace ? result.records : [...rows, ...result.records]);
    setTotal(result.total);
    setPage(nextPage);
    setDone(rows.length + result.records.length >= result.total);
  };

  useEffect(() => {
    setDone(false);
    load(1, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.toString(), sort, categoryId]);

  const submit = () => {
    const next = new URLSearchParams(params);
    if (keyword.trim()) next.set('keyword', keyword.trim());
    else next.delete('keyword');
    setParams(next);
  };

  return (
    <div style={{ paddingBottom: 24 }}>
      <div className="topbar">
        <button className="icon-btn" onClick={() => navigate(-1)}>
          ‹
        </button>
        <div className="search-bar" style={{ flex: 1 }}>
          <span>🔍</span>
          <input
            value={keyword}
            placeholder="搜索商品"
            onChange={(event) => setKeyword(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') submit();
            }}
          />
        </div>
        <button className="icon-btn" style={{ fontSize: 13, width: 'auto', padding: '0 6px' }} onClick={submit}>
          搜索
        </button>
      </div>

      <div className="page">
        {categoryName && (
          <div style={{ marginBottom: 10 }}>
            <span className="muted">分类：</span>
            <span style={{ fontSize: 14, fontWeight: 600 }}>{categoryName}</span>
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div style={{ display: 'flex', gap: 6 }}>
            {SORTS.map((item) => (
              <button
                key={item.key}
                className={`chip ${sort === item.key ? 'on' : ''}`}
                style={{ padding: '4px 11px', fontSize: 12 }}
                onClick={() => {
                  setSort(item.key);
                  setRows([]);
                }}
              >
                {item.label}
              </button>
            ))}
          </div>
          <span className="muted">共 {total} 件</span>
        </div>

        {rows.length ? (
          <div className="product-grid">
            {rows.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                onClick={() => navigate(`/product/${product.id}`)}
              />
            ))}
          </div>
        ) : loading ? (
          <div className="product-grid">
            {[0, 1, 2, 3].map((i) => (
              <div key={i} className="skel" style={{ height: 210 }} />
            ))}
          </div>
        ) : (
          <Empty glyph="🔍" text="没有找到相关商品，换个关键词试试" />
        )}

        {rows.length > 0 && !done && (
          <div style={{ textAlign: 'center', marginTop: 16 }}>
            <button
              className="btn btn-outline btn-sm"
              disabled={loading}
              onClick={() => load(page + 1, false)}
            >
              {loading ? '加载中…' : '加载更多'}
            </button>
          </div>
        )}
        {done && rows.length > 0 && (
          <div style={{ textAlign: 'center', marginTop: 18 }} className="muted">
            — 已经到底啦 —
          </div>
        )}
      </div>
    </div>
  );
}
