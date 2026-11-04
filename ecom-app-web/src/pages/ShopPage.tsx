import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api, attempt, type PageResult } from '@/api/client';
import { Empty, ProductCard } from '@/components/ui';
import { useT } from '@/store/locale';
import { useLocale } from '@/store/locale';
import type { MessageKey } from '@/i18n/messages';
import type { Category, Product } from '@/types';

const SORTS: { key: string; label: MessageKey }[] = [
  { key: 'newest', label: 'listing.sort.newest' },
  { key: 'sales', label: 'listing.sort.sales' },
  { key: 'price_asc', label: 'listing.sort.priceAsc' },
  { key: 'price_desc', label: 'listing.sort.priceDesc' },
];

const PAGE_SIZE = 12;

export default function ShopPage() {
  const { t } = useT();
  // the category tree returned by the API is already translated, so a language
  // change needs a refetch rather than a re-render
  const locale = useLocale((s) => s.locale);
  const [params, setParams] = useSearchParams();
  const categoryId = params.get('categoryId') ?? undefined;
  const keyword = params.get('keyword') ?? undefined;
  const sort = params.get('sort') ?? 'newest';

  const [tree, setTree] = useState<Category[]>([]);
  const [rows, setRows] = useState<Product[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [facetsOpen, setFacetsOpen] = useState(false);

  const [priceMin, setPriceMin] = useState(params.get('minPrice') ?? '');
  const [priceMax, setPriceMax] = useState(params.get('maxPrice') ?? '');

  useEffect(() => {
    api.get<Category[]>('/app/categories').then(setTree).catch(() => undefined);
  }, [locale]);

  const load = useCallback(
    async (nextPage: number, replace: boolean) => {
      setLoading(true);
      const result = await attempt(() =>
        api.get<PageResult<Product>>('/app/products', {
          keyword,
          categoryId,
          sort,
          minPrice: params.get('minPrice') ?? undefined,
          maxPrice: params.get('maxPrice') ?? undefined,
          page: nextPage,
          limit: PAGE_SIZE,
        }),
      );
      setLoading(false);
      if (!result) return;
      setTotal(result.total);
      setPage(nextPage);
      setRows((prev) => (replace ? result.records : [...prev, ...result.records]));
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [keyword, categoryId, sort, params.toString(), locale],
  );

  useEffect(() => {
    load(1, true);
  }, [load]);

  const patch = (next: Record<string, string | undefined>) => {
    const merged = new URLSearchParams(params);
    Object.entries(next).forEach(([key, value]) => {
      if (value) merged.set(key, value);
      else merged.delete(key);
    });
    setParams(merged);
  };

  const activeCategoryName = useMemo(() => {
    if (!categoryId) {
      return keyword ? t('listing.searchFor', { keyword }) : t('listing.all');
    }
    for (const root of tree) {
      if (String(root.id) === categoryId) return root.name;
      const child = root.children?.find((c) => String(c.id) === categoryId);
      if (child) return child.name;
    }
    return params.get('name') ?? t('listing.product');
  }, [categoryId, tree, keyword, params, t]);

  const hasFilters = Boolean(categoryId || keyword || params.get('minPrice') || params.get('maxPrice'));
  const canLoadMore = rows.length < total;

  return (
    <div className="shell">
      <div className="listing">
        {/* ------------------------------------------------------- facets */}
        <aside className={`facets ${facetsOpen ? 'open' : ''}`}>
          <div className="facet-group">
            <h4>{t('listing.categories')}</h4>
            <div className="facet-list">
              <button
                className={!categoryId ? 'on' : ''}
                onClick={() => patch({ categoryId: undefined, name: undefined })}
              >
                {t('listing.all')}
              </button>
              {tree.map((root) => (
                <div key={root.id} style={{ marginTop: 6 }}>
                  <button
                    className={String(root.id) === categoryId ? 'on' : ''}
                    onClick={() => patch({ categoryId: String(root.id), name: root.name })}
                  >
                    {root.name}
                    <span className="count">{root.productCount ?? 0}</span>
                  </button>
                  {root.children?.length ? (
                    <div style={{ paddingLeft: 14, marginTop: 2 }}>
                      {root.children.map((child) => (
                        <button
                          key={child.id}
                          className={String(child.id) === categoryId ? 'on' : ''}
                          style={{ fontSize: 12.5, padding: '5px 0' }}
                          onClick={() => patch({ categoryId: String(child.id), name: child.name })}
                        >
                          {child.name}
                          <span className="count">{child.productCount ?? 0}</span>
                        </button>
                      ))}
                    </div>
                  ) : null}
                </div>
              ))}
            </div>
          </div>

          <div className="facet-group">
            <h4>{t('listing.price')}</h4>
            <div className="price-range">
              <input
                value={priceMin}
                onChange={(e) => setPriceMin(e.target.value.replace(/\D/g, ''))}
                placeholder={t('listing.priceMin')}
                inputMode="numeric"
                aria-label={t('listing.priceMin')}
              />
              <span className="muted">—</span>
              <input
                value={priceMax}
                onChange={(e) => setPriceMax(e.target.value.replace(/\D/g, ''))}
                placeholder={t('listing.priceMax')}
                inputMode="numeric"
                aria-label={t('listing.priceMax')}
              />
            </div>
            <button
              className="btn btn-outline btn-sm btn-block"
              style={{ marginTop: 10 }}
              onClick={() =>
                patch({
                  minPrice: priceMin || undefined,
                  maxPrice: priceMax || undefined,
                })
              }
            >
              {t('listing.applyPrice')}
            </button>
          </div>

          {hasFilters && (
            <div className="facet-group">
              <button
                className="btn-link"
                onClick={() => {
                  setPriceMin('');
                  setPriceMax('');
                  setParams(new URLSearchParams());
                }}
              >
                {t('listing.clearFilters')}
              </button>
            </div>
          )}
        </aside>

        {/* ------------------------------------------------------- results */}
        <div>
          <div className="listing-head">
            <h1>{activeCategoryName}</h1>
            <div className="listing-tools">
              <span className="result-count">{t('listing.results', { count: total })}</span>
              <button
                className="btn btn-outline btn-sm facet-toggle"
                onClick={() => setFacetsOpen((v) => !v)}
              >
                {facetsOpen ? t('listing.hideFilters') : t('listing.filter')}
              </button>
              <div className="pills">
                {SORTS.map((item) => (
                  <button
                    key={item.key}
                    className={`pill ${sort === item.key ? 'on' : ''}`}
                    onClick={() => patch({ sort: item.key === 'newest' ? undefined : item.key })}
                  >
                    {t(item.label)}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {loading && rows.length === 0 ? (
            <div className="product-grid">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i}>
                  <div className="skel" style={{ aspectRatio: '4 / 5', marginBottom: 12 }} />
                  <div className="skel" style={{ height: 14, marginBottom: 6 }} />
                  <div className="skel" style={{ height: 14, width: '54%' }} />
                </div>
              ))}
            </div>
          ) : rows.length === 0 ? (
            <Empty
              glyph="◎"
              title={t('listing.empty')}
              description={t('listing.emptyHint')}
              action={
                <button className="btn btn-primary" onClick={() => setParams(new URLSearchParams())}>
                  {t('listing.viewAll')}
                </button>
              }
            />
          ) : (
            <>
              <div className="product-grid">
                {rows.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>

              {canLoadMore && (
                <div style={{ textAlign: 'center', marginTop: 48 }}>
                  <button
                    className="btn btn-outline"
                    disabled={loading}
                    onClick={() => load(page + 1, false)}
                  >
                    {loading
                      ? t('common.loading')
                      : t('listing.loadMore', { count: total - rows.length })}
                  </button>
                </div>
              )}
              {!canLoadMore && rows.length > PAGE_SIZE / 2 && (
                <div style={{ textAlign: 'center', marginTop: 44 }} className="muted">
                  {t('listing.shownAll', { count: total })}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
