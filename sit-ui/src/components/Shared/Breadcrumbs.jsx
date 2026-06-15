import { Link } from 'react-router-dom';
import { useI18n } from '../../i18n/useI18n';

const Breadcrumbs = ({ title }) => {
  const { t } = useI18n();
  return (
    <ul className="sit-breadcrumb">
      <li><Link to="/">{t('common.home')}</Link></li>
      <li className="active">{title}</li>
    </ul>
  );
};

export default Breadcrumbs;
