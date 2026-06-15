import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { useI18n } from '../../i18n/useI18n';
import { useDashboardQuery } from '../../api/hooks';

const formatSessionTime = (value) => {
  if (!value) return '—';
  const d = new Date(value);
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  const mo = String(d.getMonth() + 1).padStart(2, '0');
  return `${hh}:${mm} ${dd}-${mo}`;
};

const StatCard = ({ icon, iconClass, value, valueClass, label, hint }) => (
  <div className="dash-stat-card">
    <div className={`dash-stat-icon ${iconClass}`}>
      <i className={icon} />
    </div>
    <div className={`dash-stat-value ${valueClass || ''}`}>{value}</div>
    <div className="dash-stat-label">{label}</div>
    {hint && <div className="dash-stat-hint">{hint}</div>}
  </div>
);

const AcceptanceChart = ({ days, legendPassed, legendFailed }) => {
  const max = useMemo(() => {
    const peak = Math.max(...days.map((d) => Math.max(d.passed, d.failed)), 1);
    return peak;
  }, [days]);

  return (
    <div className="dash-chart">
      <div className="dash-chart-bars">
        {days.map((day) => (
          <div key={day.label} className="dash-chart-group">
            <div className="dash-chart-bar-wrap">
              <div
                className="dash-chart-bar dash-chart-bar-pass"
                style={{ height: `${(day.passed / max) * 100}%` }}
                title={`Pass: ${day.passed}`}
              />
              <div
                className="dash-chart-bar dash-chart-bar-fail"
                style={{ height: `${(day.failed / max) * 100}%` }}
                title={`Fail: ${day.failed}`}
              />
            </div>
            <div className="dash-chart-label">{day.label}</div>
          </div>
        ))}
      </div>
      <div className="dash-chart-legend">
        <span><i className="dash-legend-dot dash-legend-pass" /> {legendPassed}</span>
        <span><i className="dash-legend-dot dash-legend-fail" /> {legendFailed}</span>
      </div>
    </div>
  );
};

const Home = () => {
  const user = useSelector((state) => state.auth.user);
  const { t } = useI18n();
  const { data, error, isLoading } = useDashboardQuery();

  const weekHint = data
    ? t('home.weekChange', {
      n: `${data.sessionsWeekChangePercent >= 0 ? '+' : ''}${data.sessionsWeekChangePercent}`,
    })
    : '';

  const displayName = user?.fullName || t('common.guest');

  return (
    <div className="dash-page">
      <div className="dash-page-header">
        <div>
          <h2 className="dash-page-title">{t('home.title')}</h2>
          <p className="dash-page-subtitle">
            {t('home.welcome', { name: displayName })}
          </p>
        </div>
        <Link to="/sessions/new" className="btn btn-primary btn-sm">
          <i className="ri-add-line" /> {t('home.createSession')}
        </Link>
      </div>

      {error && <div className="alert alert-danger">{error.message}</div>}
      {isLoading && !data && <p className="text-muted text-center py-4">{t('common.loading')}</p>}

      {data && (
        <>
          <div className="dash-stat-grid">
            <StatCard
              icon="ri-terminal-box-line"
              iconClass="dash-icon-neutral"
              value={data.terminalCount}
              label={t('home.terminalCount')}
              hint={t('home.terminalDelta', { n: data.terminalDeltaMonth })}
            />
            <StatCard
              icon="ri-line-chart-line"
              iconClass="dash-icon-info"
              value={data.sessionsThisWeek}
              valueClass="text-brand"
              label={t('home.sessionsWeek')}
              hint={weekHint}
            />
            <StatCard
              icon="ri-checkbox-circle-line"
              iconClass="dash-icon-success"
              value={data.testCasePassed}
              valueClass="text-success"
              label={t('home.passed')}
              hint={t('home.passRate', { n: data.passRatePercent })}
            />
            <StatCard
              icon="ri-close-circle-line"
              iconClass="dash-icon-danger"
              value={data.testCaseFailed}
              valueClass="text-danger"
              label={t('home.failed')}
              hint={data.topFailHint}
            />
          </div>

          <div className="dash-panels">
            <div className="dash-panel">
              <div className="dash-panel-header">
                <h3 className="dash-panel-title">{t('home.chartTitle')}</h3>
                <p className="dash-panel-subtitle">{t('home.chartSubtitle')}</p>
              </div>
              <AcceptanceChart
                days={data.chartLast7Days}
                legendPassed={t('home.chartPassed')}
                legendFailed={t('home.chartFailed')}
              />
            </div>

            <div className="dash-panel">
              <div className="dash-panel-header">
                <h3 className="dash-panel-title">{t('home.recentSessions')}</h3>
                <p className="dash-panel-subtitle">{t('home.recentSubtitle')}</p>
              </div>
              <div className="dash-session-list">
                {data.recentSessions.length === 0 && (
                  <p className="text-muted text-center py-3">
                    {t('home.noSessions')}{' '}
                    <Link to="/sessions/new">{t('home.createNew')}</Link>
                  </p>
                )}
                {data.recentSessions.map((s) => (
                  <Link key={s.id} to={`/sessions/${s.id}/auto`} className="dash-session-item">
                    <span className={`dash-session-dot ${
                      s.statusLabel === 'PASSED' ? 'passed' : s.statusLabel === 'OPEN' ? 'open' : 'failed'
                    }`} />
                    <div className="dash-session-body">
                      <div className="dash-session-title">
                        #{s.id} - {s.tmnCode} [{s.statusLabel}]
                      </div>
                      <div className="dash-session-email">{s.createdByEmail}</div>
                    </div>
                    <div className="dash-session-time">{formatSessionTime(s.createdAt)}</div>
                  </Link>
                ))}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default Home;
