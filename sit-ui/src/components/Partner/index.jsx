import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import Breadcrumbs from '../Shared/Breadcrumbs';
import { useI18n } from '../../i18n/useI18n';
import { useDeletePartnerMutation, usePartnersQuery } from '../../api/hooks';
import { appActions } from '../../stores';

const PartnerList = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { t } = useI18n();
  const isAdmin = useSelector((state) => state.auth.user?.role === 'ADMIN');
  const { data: partners = [] } = usePartnersQuery();
  const deletePartner = useDeletePartnerMutation();

  const handleDelete = async (id, name) => {
    if (!window.confirm(t('partners.confirmDelete', { name }))) return;
    try {
      await deletePartner.mutateAsync(id);
      dispatch(appActions.flash(t('partners.deleted')));
    } catch {
      // error surfaced by mutation cache
    }
  };

  return (
    <>
      <Breadcrumbs title={t('partners.title')} />
      <div className="card-header">
        <div className="d-flex justify-content-between align-items-center">
          <h3 className="card-title mb-0">{t('partners.listTitle')}</h3>
          {isAdmin && (
            <Link to="/partners/create" className="btn btn-primary btn-sm">
              <i className="ri-add-line" /> {t('partners.add')}
            </Link>
          )}
        </div>
      </div>
      <div className="card-body">
        <div className="table-wrap">
          <table className="table table-striped">
            <thead>
              <tr>
                <th>{t('common.id')}</th>
                <th>{t('common.name')}</th>
                <th>{t('common.flow')}</th>
                <th>{t('partners.tmnCode')}</th>
                <th>{t('partners.returnUrl')}</th>
                <th>{t('partners.ipnUrl')}</th>
                <th className="text-center">{t('common.active')}</th>
                {isAdmin && <th />}
              </tr>
            </thead>
            <tbody>
              {partners.map((p) => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>{p.name}</td>
                  <td><span className="badge badge-info">{p.flow}</span></td>
                  <td>{p.tmnCode}</td>
                  <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>{p.returnUrl}</td>
                  <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>{p.ipnUrl}</td>
                  <td className="text-center">
                    <span className={`badge ${p.active ? 'badge-success' : 'badge-danger'}`}>
                      {p.active ? t('common.yes') : t('common.no')}
                    </span>
                  </td>
                  {isAdmin && (
                    <td>
                      <button
                        type="button"
                        className="btn btn-icon"
                        title={t('common.edit')}
                        onClick={() => navigate(`/partners/${p.id}/edit`)}
                      >
                        <i className="ri-edit-line text-primary" />
                      </button>
                      <button
                        type="button"
                        className="btn btn-icon"
                        title={t('common.delete')}
                        onClick={() => handleDelete(p.id, p.name)}
                      >
                        <i className="ri-delete-bin-line text-danger" />
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
          {partners.length === 0 && (
            <p className="text-muted text-center">
              {t('partners.empty')}
              {isAdmin && (
                <>
                  {' '}
                  <Link to="/partners/create">{t('partners.emptyLink')}</Link>
                </>
              )}
            </p>
          )}
        </div>
      </div>
    </>
  );
};

export default PartnerList;
