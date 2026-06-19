import { useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import Breadcrumbs from '../Shared/Breadcrumbs';
import SitSearchBox from '../Shared/SitSearchBox';
import { useI18n } from '../../i18n/useI18n';
import { getFlowLabel } from '../../i18n/flowLabels';
import { useDebouncedSearch } from '../../hooks/useDebouncedSearch';
import { useDeletePartnerMutation, usePartnersQuery } from '../../api/hooks';
import { appActions } from '../../stores';

const PartnerActiveBadge = ({ active, t }) => (
  <span className={`badge sit-status-badge ${active ? 'badge-success' : 'badge-danger'}`}>
    <i className={active ? 'ri-checkbox-circle-fill' : 'ri-close-circle-fill'} aria-hidden="true" />
    {active ? t('common.yes') : t('common.no')}
  </span>
);

const PartnerList = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const user = useSelector((state) => state.auth.user);
  const isAdmin = user?.role === 'ADMIN';
  const { t } = useI18n();
  const { search, setSearch, debouncedSearch, hasSearch } = useDebouncedSearch();
  const { data: partners = [], isLoading } = usePartnersQuery();
  const deletePartner = useDeletePartnerMutation();

  const filteredPartners = useMemo(() => {
    const q = debouncedSearch.trim().toLowerCase();
    if (!q) return partners;
    return partners.filter((p) => (
      [p.id, p.name, p.flow, p.flowLabel, p.tmnCode, p.ipnUrl, p.createdByEmail]
        .some((value) => String(value ?? '').toLowerCase().includes(q))
    ));
  }, [partners, debouncedSearch]);

  const handleDelete = async (id, name, e) => {
    e?.stopPropagation();
    if (!window.confirm(t('partners.confirmDelete', { name }))) return;
    try {
      await deletePartner.mutateAsync(id);
      dispatch(appActions.flash(t('partners.deleted')));
    } catch {
      // error surfaced by mutation cache
    }
  };

  const openEdit = (id) => navigate(`/partners/${id}/edit`);

  return (
    <>
      <Breadcrumbs title={t('partners.title')} />
      <div className="card-header sit-page-header">
        <div className="d-flex justify-content-between align-items-center">
          <div>
            <h3 className="card-title mb-0">{t('partners.listTitle')}</h3>
            <p className="sit-page-subtitle mb-0">{t('partners.subtitle')}</p>
          </div>
          <Link to="/partners/create" className="btn btn-primary btn-sm">
            <i className="ri-add-line" /> {t('partners.add')}
          </Link>
        </div>
      </div>
      <div className="card-body">
        <div className="sit-list-toolbar">
          <div className="sit-list-toolbar-search">
            <SitSearchBox
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={t('partners.searchPlaceholder')}
            />
          </div>
        </div>

        {partners.length > 0 && (
          <p className="sit-list-count">
            {t('partners.terminalCount', { n: filteredPartners.length })}
            {hasSearch && filteredPartners.length !== partners.length && (
              <span className="sit-list-count-filtered">
                {' '}
                / {partners.length}
              </span>
            )}
          </p>
        )}

        {isLoading && (
          <div className="sit-list-loading">
            <i className="ri-loader-4-line" aria-hidden="true" />
            <span>{t('common.loading')}</span>
          </div>
        )}

        {!isLoading && (
          <div className="sit-list-table-wrap">
            {filteredPartners.length > 0 ? (
              <table className="table data-table sit-data-table partner-table">
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
                    <th className="partner-col partner-col--id">{t('common.id')}</th>
                    <th className="partner-col partner-col--name">{t('partners.terminalName')}</th>
                    <th className="partner-col partner-col--flow">{t('common.flow')}</th>
                    <th className="partner-col partner-col--tmn">{t('partners.tmnCode')}</th>
                    <th className="partner-col partner-col--url">{t('partners.ipnUrl')}</th>
                    <th className="partner-col partner-col--status">{t('common.active')}</th>
                    <th className="partner-col partner-col--actions">{t('common.actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredPartners.map((p) => (
                    <tr
                      key={p.id}
                      className="sit-data-row"
                      onClick={() => openEdit(p.id)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          openEdit(p.id);
                        }
                      }}
                      tabIndex={0}
                      role="link"
                      aria-label={t('common.edit')}
                    >
                      <td className="partner-col partner-col--id">
                        <span className="sit-id-badge">#{p.id}</span>
                      </td>
                      <td className="partner-col partner-col--name">
                        <div className="partner-name-cell">
                          <span className="partner-name" title={p.name}>{p.name}</span>
                          {isAdmin && p.createdByEmail && (
                            <span className="partner-creator" title={p.createdByEmail}>
                              {p.createdByEmail}
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="partner-col partner-col--flow">
                        <span className="badge badge-info partner-flow-badge">
                          {getFlowLabel(t, p.flow)}
                        </span>
                      </td>
                      <td className="partner-col partner-col--tmn">
                        <span className="badge badge-info partner-tmn-badge">{p.tmnCode}</span>
                      </td>
                      <td className="partner-col partner-col--url">
                        {p.ipnUrl ? (
                          <span className="partner-url" title={p.ipnUrl}>
                            <i className="ri-link" aria-hidden="true" />
                            {p.ipnUrl}
                          </span>
                        ) : (
                          <span className="text-muted">{t('common.empty')}</span>
                        )}
                      </td>
                      <td className="partner-col partner-col--status">
                        <PartnerActiveBadge active={p.active} t={t} />
                      </td>
                      <td className="partner-col partner-col--actions">
                        <div className="partner-actions">
                          <button
                            type="button"
                            className="btn btn-light-primary btn-sm partner-action-btn"
                            title={t('common.edit')}
                            onClick={(e) => {
                              e.stopPropagation();
                              openEdit(p.id);
                            }}
                          >
                            <i className="ri-edit-line" aria-hidden="true" />
                            <span>{t('common.edit')}</span>
                          </button>
                          <button
                            type="button"
                            className="btn btn-light-danger btn-sm partner-action-btn"
                            title={t('common.delete')}
                            onClick={(e) => handleDelete(p.id, p.name, e)}
                          >
                            <i className="ri-delete-bin-line" aria-hidden="true" />
                            <span>{t('common.delete')}</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <div className="sit-list-empty">
                <i className="ri-terminal-box-line" aria-hidden="true" />
                <p>{hasSearch ? t('common.searchEmpty') : t('partners.empty')}</p>
                {!hasSearch && (
                  <Link to="/partners/create" className="btn btn-primary btn-sm sit-list-empty-cta">
                    <i className="ri-add-line" aria-hidden="true" />
                    {t('partners.emptyLink')}
                  </Link>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </>
  );
};

export default PartnerList;
