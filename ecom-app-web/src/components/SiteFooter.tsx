import { Link } from 'react-router-dom';
import { useT } from '@/store/locale';

export default function SiteFooter() {
  const { t } = useT();

  return (
    <footer className="site-footer">
      <div className="shell">
        <div className="footer-grid">
          <div>
            <div className="brand" style={{ marginBottom: 2 }}>
              <span className="mark">
                <span>山</span>
              </span>
              <span>{t('brand.name')}</span>
            </div>
            <p className="footer-brand-blurb">{t('brand.tagline')}</p>
          </div>

          <div>
            <h4>{t('footer.shop')}</h4>
            <ul>
              <li><Link to="/shop">{t('footer.allProducts')}</Link></li>
              <li><Link to="/shop?categoryId=1">{t('footer.bikes')}</Link></li>
              <li><Link to="/shop?sort=newest">{t('footer.newArrivals')}</Link></li>
              <li><Link to="/shop?sort=sales">{t('footer.bestSellers')}</Link></li>
            </ul>
          </div>

          <div>
            <h4>{t('footer.service')}</h4>
            <ul>
              <li><Link to="/orders">{t('footer.orderLookup')}</Link></li>
              <li><Link to="/account/addresses">{t('footer.addressBook')}</Link></li>
              <li><Link to="/coupons">{t('footer.couponCentre')}</Link></li>
              <li><Link to="/account">{t('footer.memberCentre')}</Link></li>
            </ul>
          </div>

          <div>
            <h4>{t('footer.guarantee')}</h4>
            <ul>
              <li>{t('footer.returns')}</li>
              <li>{t('footer.authorised')}</li>
              <li>{t('footer.freeShipping')}</li>
              <li>{t('footer.freeAssembly')}</li>
            </ul>
          </div>
        </div>

        <div className="footer-mark" aria-hidden>
          RIDE HARD
        </div>

        <div className="footer-note">
          <span>{t('footer.copyright', { year: new Date().getFullYear() })}</span>
          <span>{t('footer.platformNote')}</span>
        </div>
      </div>
    </footer>
  );
}
