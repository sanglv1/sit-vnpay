import { useI18n } from '../../i18n/useI18n';

const CreditSection = () => {
  const { t } = useI18n();
  const year = new Date().getFullYear();

  return (
    <footer className="sit-footer">
      <div className="sit-footer-inner">
        <span className="sit-footer-copy">
          &copy; {year}{' '}
          <strong className="sit-footer-brand">VNPAY</strong>
        </span>
        <span className="sit-footer-sep" aria-hidden="true" />
        <span className="sit-footer-product">{t('footer.product')}</span>
        <span className="sit-footer-env">{t('footer.sandbox')}</span>
      </div>
    </footer>
  );
};

export default CreditSection;
