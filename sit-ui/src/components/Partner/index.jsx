import { useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import Breadcrumbs from '../Shared/Breadcrumbs';
import SitSearchBox from '../Shared/SitSearchBox';
import { useI18n } from '../../i18n/useI18n';
import { useDebouncedSearch } from '../../hooks/useDebouncedSearch';
import { useDeletePartnerMutation, usePartnersQuery } from '../../api/hooks';
import { appActions } from '../../stores';

const PartnerList = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { t } = useI18n();
  const { search, setSearch, debouncedSearch, hasSearch } = useDebouncedSearch();
  const { data: partners = [] } = usePartnersQuery();
  const deletePartner = useDeletePartnerMutation();

  const filteredPartners = useMemo(() => {
    const q = debouncedSearch.trim().toLowerCase();
    if (!q) return partners;
    return partners.filter((p) => (
      [p.id, p.name, p.flow, p.tmnCode, p.ipnUrl]
        .some((value) => String(value ?? '').toLowerCase().includes(q))
    ));
  }, [partners, debouncedSearch]);

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
          <Link to="/partners/create" className="btn btn-primary btn-sm">
            <i className="ri-add-line" /> {t('partners.add')}
          </Link>
        </div>
      </div>
      <div className="card-body">
        <SitSearchBox
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={t('partners.searchPlaceholder')}
        />
        <div className="table-wrap">
          <table className="table table-striped data-table partner-table">
            <colgroup>
              <col className="col-id" />
              <col className="col-name" />
              <col className="col-flow" />
              <col className="col-tmn" />
              <col className="col-url" />
              <col className="col-status" />
              <col className="col-actions" />
            </colgroup>
            <thead>
              <tr>
                <th className="text-center">{t('common.id')}</th>
                <th className="text-start">{t('partners.terminalName')}</th>
                <th className="text-center">{t('common.flow')}</th>
                <th className="text-center">{t('partners.tmnCode')}</th>
                <th className="text-start">{t('partners.ipnUrl')}</th>
                <th className="text-center">{t('common.active')}</th>
                <th className="text-center">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {filteredPartners.map((p) => (
                <tr key={p.id}>
                  <td className="text-center">{p.id}</td>
                  <td className="text-start cell-truncate" title={p.name}>{p.name}</td>
                  <td className="text-center">
                    <span className="badge badge-info">{p.flow}</span>
                  </td>
                  <td className="text-center text-nowrap">{p.tmnCode}</td>
                  <td className="text-start cell-truncate" title={p.ipnUrl}>{p.ipnUrl || t('common.empty')}</td>
                  <td className="text-center">
                    <span className={`badge ${p.active ? 'badge-success' : 'badge-danger'}`}>
                      {p.active ? t('common.yes') : t('common.no')}
                    </span>
                  </td>
                  <td className="text-center">
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
                </tr>
              ))}
            </tbody>
          </table>
          {filteredPartners.length === 0 && (
            <p className="text-muted text-center">
              {hasSearch
                ? t('common.searchEmpty')
                : (
                  <>
                    {t('partners.empty')}
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
