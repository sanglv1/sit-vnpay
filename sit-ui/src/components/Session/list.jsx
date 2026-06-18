import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import Breadcrumbs from '../Shared/Breadcrumbs';
import SitSearchBox from '../Shared/SitSearchBox';
import { useI18n } from '../../i18n/useI18n';
import { useDebouncedSearch } from '../../hooks/useDebouncedSearch';
import { useSessionsQuery } from '../../api/hooks';

const PAGE_SIZE = 20;

const getSessionStatus = (session) => {
  const { autoPassed, autoTotal } = session;
  if (autoTotal > 0 && autoPassed === autoTotal) return 'completed';
  if (autoPassed === 0) return 'not_started';
  return 'in_progress';
};

const SessionProgress = ({ passed, total, t }) => {
  const pct = total > 0 ? Math.round((passed / total) * 100) : 0;
  const variant = passed === total && total > 0
    ? 'complete'
    : passed > 0
      ? 'partial'
      : 'empty';

  return (
    <div className="session-progress">
      <div className="session-progress-track" aria-hidden="true">
        <div
          className={`session-progress-fill session-progress-fill--${variant}`}
          style={{ width: `${pct}%` }}
        />
      </div>
      <span className="session-progress-label">
        {passed}/{total} {t('common.pass')}
      </span>
    </div>
  );
};

const SessionStatusBadge = ({ status, t }) => {
  const config = {
    completed: { className: 'badge-success', icon: 'ri-checkbox-circle-fill', label: t('sessions.statusCompleted') },
    in_progress: { className: 'badge-warning', icon: 'ri-loader-4-line', label: t('sessions.statusInProgress') },
    not_started: { className: 'badge-muted', icon: 'ri-time-line', label: t('sessions.statusNotStarted') },
  };
  const { className, icon, label } = config[status] || config.not_started;

  return (
    <span className={`badge sit-status-badge ${className}`}>
      <i className={icon} aria-hidden="true" />
      {label}
    </span>
  );
};

const formatSessionDate = (value, locale) => {
  if (!value) return null;
  const d = new Date(value);
  const date = d.toLocaleDateString(locale === 'en' ? 'en-US' : 'vi-VN');
  const time = d.toLocaleTimeString(locale === 'en' ? 'en-US' : 'vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
  return { date, time };
};

const SessionList = () => {
  const navigate = useNavigate();
  const user = useSelector((state) => state.auth.user);
  const isAdmin = user?.role === 'ADMIN';
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

  const onSearchChange = (e) => {
    setSearch(e.target.value);
    setPage(0);
  };

  const onCompletionChange = (e) => {
    setCompletion(e.target.value);
    setPage(0);
  };

  const openSession = (id) => navigate(`/sessions/${id}/auto`);

  const totalPages = data?.totalPages ?? 0;
  const canGoPrev = page > 0;
  const canGoNext = data ? page < data.totalPages - 1 : false;

  return (
    <>
      <Breadcrumbs title={t('sessions.title')} />
      <div className="card-header sit-page-header">
        <div className="d-flex justify-content-between align-items-center">
          <div>
            <h3 className="card-title mb-0">{t('sessions.title')}</h3>
            <p className="sit-page-subtitle mb-0">{t('sessions.subtitle')}</p>
          </div>
          <Link to="/sessions/new" className="btn btn-primary btn-sm">
            <i className="ri-add-line" /> {t('sessions.create')}
          </Link>
        </div>
      </div>
      <div className="card-body">
        <div className="sit-list-toolbar">
          <div className="sit-list-toolbar-search">
            <SitSearchBox
              value={search}
              onChange={onSearchChange}
              placeholder={t('sessions.searchPlaceholder')}
            />
          </div>
          <div className="sit-list-toolbar-filter">
            <i className="ri-filter-3-line sit-filter-icon" aria-hidden="true" />
            <select
              className="form-select sit-filter-select"
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

        {data && data.totalElements > 0 && (
          <p className="sit-list-count">
            {t('sessions.sessionCount', { n: data.totalElements })}
          </p>
        )}

        {isLoading && (
          <div className="sit-list-loading">
            <i className="ri-loader-4-line" aria-hidden="true" />
            <span>{t('common.loading')}</span>
          </div>
        )}

        {data && (
          <div className={`sit-list-table-wrap${isFetching ? ' is-fetching' : ''}`}>
            {data.content.length > 0 ? (
              <table className="table data-table sit-data-table session-table">
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
                    <th className="session-col session-col--id">{t('common.id')}</th>
                    <th className="session-col session-col--tmn">{t('sessions.tmnCode')}</th>
                    <th className="session-col session-col--auto">{t('sessions.auto')}</th>
                    <th className="session-col session-col--status">{t('common.status')}</th>
                    <th className="session-col session-col--datetime">{t('sessions.updated')}</th>
                    <th className="session-col session-col--actions">{t('common.actions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((s) => {
                    const status = getSessionStatus(s);
                    const datetime = formatSessionDate(s.updatedAt, locale);

                    return (
                      <tr
                        key={s.id}
                        className="sit-data-row"
                        onClick={() => openSession(s.id)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            openSession(s.id);
                          }
                        }}
                        tabIndex={0}
                        role="link"
                        aria-label={t('sessions.open')}
                      >
                        <td className="session-col session-col--id">
                          <span className="sit-id-badge">#{s.id}</span>
                        </td>
                        <td className="session-col session-col--tmn">
                          <div className="session-tmn-cell">
                            <span className="badge badge-info session-tmn-badge">{s.tmnCode}</span>
                            {s.partnerName && (
                              <span className="session-terminal-name" title={s.partnerName}>
                                {s.partnerName}
                              </span>
                            )}
                            {isAdmin && s.createdByEmail && (
                              <span className="session-creator" title={s.createdByEmail}>
                                {s.createdByEmail}
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="session-col session-col--auto">
                          <SessionProgress passed={s.autoPassed} total={s.autoTotal} t={t} />
                        </td>
                        <td className="session-col session-col--status">
                          <SessionStatusBadge status={status} t={t} />
                        </td>
                        <td className="session-col session-col--datetime">
                          {datetime ? (
                            <div className="session-datetime">
                              <span className="session-datetime-time">{datetime.time}</span>
                              <span className="session-datetime-date">{datetime.date}</span>
                            </div>
                          ) : (
                            t('common.empty')
                          )}
                        </td>
                        <td className="session-col session-col--actions">
                          <button
                            type="button"
                            className="btn btn-light-primary btn-sm sit-open-btn"
                            title={t('sessions.open')}
                            onClick={(e) => {
                              e.stopPropagation();
                              openSession(s.id);
                            }}
                          >
                            <i className="ri-eye-line" aria-hidden="true" />
                            <span>{t('sessions.open')}</span>
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            ) : (
              <div className="sit-list-empty">
                <i className="ri-file-list-3-line" aria-hidden="true" />
                <p>
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
              </div>
            )}
            {totalPages > 1 && (
              <div className="sit-list-pagination">
                <button
                  type="button"
                  className="btn btn-light-primary btn-sm"
                  disabled={!canGoPrev}
                  onClick={() => setPage((p) => p - 1)}
                >
                  <i className="ri-arrow-left-s-line" aria-hidden="true" />
                  {t('common.previous')}
                </button>
                <span className="sit-list-page-info">
                  {t('common.pageOf', { page: page + 1, total: totalPages })}
                </span>
                <button
                  type="button"
                  className="btn btn-light-primary btn-sm"
                  disabled={!canGoNext}
                  onClick={() => setPage((p) => p + 1)}
                >
                  {t('common.next')}
                  <i className="ri-arrow-right-s-line" aria-hidden="true" />
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
