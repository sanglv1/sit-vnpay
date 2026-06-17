import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Breadcrumbs from '../Shared/Breadcrumbs';
import SitSearchBox from '../Shared/SitSearchBox';
import { useI18n } from '../../i18n/useI18n';
import { useDebouncedSearch } from '../../hooks/useDebouncedSearch';
import { useSessionsQuery } from '../../api/hooks';

const PAGE_SIZE = 20;

const SessionList = () => {
  const navigate = useNavigate();
  const { t, locale } = useI18n();
  const { search, setSearch, debouncedSearch, hasSearch } = useDebouncedSearch();
  const [page, setPage] = useState(0);
  const [completion, setCompletion] = useState('ALL');

  const listParams = useMemo(() => {
    const params = { page, size: PAGE_SIZE };
    if (debouncedSearch.trim()) params.q = debouncedSearch.trim();
    if (completion !== 'ALL') params.completion = completion;
    return params;
  }, [debouncedSearch, completion, page]);

  const { data, isLoading, isFetching } = useSessionsQuery(listParams);

  const formatDate = (v) => (
    v ? new Date(v).toLocaleString(locale === 'en' ? 'en-US' : 'vi-VN') : t('common.empty')
  );

  const isSessionCompleted = (session) => (
    session.autoTotal > 0 && session.autoPassed === session.autoTotal
  );

  const onSearchChange = (e) => {
    setSearch(e.target.value);
    setPage(0);
  };

  const onCompletionChange = (e) => {
    setCompletion(e.target.value);
    setPage(0);
  };

  const totalPages = data?.totalPages ?? 0;
  const canGoPrev = page > 0;
  const canGoNext = data ? page < data.totalPages - 1 : false;

  return (
    <>
      <Breadcrumbs title={t('sessions.title')} />
      <div className="card-header">
        <div className="d-flex justify-content-between align-items-center">
          <div>
            <h3 className="card-title mb-0">{t('sessions.title')}</h3>
            <p className="text-muted mb-0 mt-1" style={{ fontSize: 13 }}>
              {t('sessions.subtitle')}
            </p>
          </div>
          <Link to="/sessions/new" className="btn btn-primary btn-sm">
            <i className="ri-add-line" /> {t('sessions.create')}
          </Link>
        </div>
      </div>
      <div className="card-body">
        <div className="row g-2 mb-3">
          <div className="col-lg-8">
            <SitSearchBox
              value={search}
              onChange={onSearchChange}
              placeholder={t('sessions.searchPlaceholder')}
            />
          </div>
          <div className="col-lg-4">
            <select
              className="form-select"
              value={completion}
              onChange={onCompletionChange}
              aria-label={t('sessions.filterCompletion')}
            >
              <option value="ALL">{t('sessions.filterAll')}</option>
              <option value="COMPLETED">{t('sessions.filterCompleted')}</option>
              <option value="IN_PROGRESS">{t('sessions.filterInProgress')}</option>
              <option value="NOT_STARTED">{t('sessions.filterNotStarted')}</option>
            </select>
          </div>
        </div>

        {isLoading && (
          <p className="text-muted text-center">{t('common.loading')}</p>
        )}

        {data && (
          <div className={`table-wrap${isFetching ? ' opacity-75' : ''}`}>
            <table className="table table-striped data-table session-table">
              <colgroup>
                <col className="col-id" />
                <col className="col-tmn" />
                <col className="col-auto" />
                <col className="col-status" />
                <col className="col-datetime" />
                <col className="col-actions" />
              </colgroup>
              <thead>
                <tr>
                  <th className="text-center">{t('common.id')}</th>
                  <th className="text-center">{t('sessions.tmnCode')}</th>
                  <th className="text-center">{t('sessions.auto')}</th>
                  <th className="text-center">{t('common.status')}</th>
                  <th className="text-center">{t('sessions.updated')}</th>
                  <th className="text-center">{t('common.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((s) => (
                  <tr key={s.id}>
                    <td className="text-center">{s.id}</td>
                    <td className="text-center">
                      <span className="badge badge-info">{s.tmnCode}</span>
                    </td>
                    <td className="text-center">
                      {s.autoPassed}/{s.autoTotal} {t('common.pass')}
                    </td>
                    <td className="text-center">
                      <span className={`badge ${isSessionCompleted(s) ? 'badge-success' : 'badge-danger'}`}>
                        {isSessionCompleted(s) ? t('sessions.statusCompleted') : t('sessions.statusFailed')}
                      </span>
                    </td>
                    <td className="text-center">{formatDate(s.updatedAt)}</td>
                    <td className="text-center">
                      <button
                        type="button"
                        className="btn btn-icon"
                        title={t('sessions.open')}
                        onClick={() => navigate(`/sessions/${s.id}/auto`)}
                      >
                        <i className="ri-eye-line text-primary" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {data.content.length === 0 && (
              <p className="text-muted text-center">
                {hasSearch || completion !== 'ALL'
                  ? t('common.searchEmpty')
                  : (
                    <>
                      {t('sessions.empty')}
                      {' '}
                      <Link to="/sessions/new">{t('sessions.emptyLink')}</Link>
                    </>
                  )}
              </p>
            )}
            {totalPages > 1 && (
              <div className="d-flex justify-content-between align-items-center mt-3">
                <button
                  type="button"
                  className="btn btn-light-primary btn-sm"
                  disabled={!canGoPrev}
                  onClick={() => setPage((p) => p - 1)}
                >
                  {t('common.previous')}
                </button>
                <span className="text-muted" style={{ fontSize: 13 }}>
                  {t('common.pageOf', { page: page + 1, total: totalPages })}
                </span>
                <button
                  type="button"
                  className="btn btn-light-primary btn-sm"
                  disabled={!canGoNext}
                  onClick={() => setPage((p) => p + 1)}
                >
                  {t('common.next')}
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </>
  );
};

export default SessionList;
