import { Link } from 'react-router-dom';
import Breadcrumbs from '../Shared/Breadcrumbs';
import { useI18n } from '../../i18n/useI18n';
import { getGuideContent } from '../../i18n/guideContent';

const FLOW_BADGE_CLASS = {
  PAY: 'guide-flow-pay',
  TOKEN: 'guide-flow-token',
  RECURRING: 'guide-flow-recurring',
  INSTALMENT: 'guide-flow-instalment',
};

const renderRichText = (text) => {
  if (typeof text !== 'string') return text;

  const tokens = text.split(/(\*\*[^*]+\*\*|`[^`]+`)/g);
  return tokens.map((part, i) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={i}>{part.slice(2, -2)}</strong>;
    }
    if (part.startsWith('`') && part.endsWith('`')) {
      return <code key={i} className="guide-inline-code">{part.slice(1, -1)}</code>;
    }
    return part;
  });
};

const FlowBadge = ({ flow }) => (
  <span className={`guide-flow-badge ${FLOW_BADGE_CLASS[flow] || ''}`}>{flow}</span>
);

const ModeBadge = ({ mode }) => {
  const isAuto = mode === 'Tự động' || mode === 'Auto';
  return (
    <span className={`guide-mode-badge ${isAuto ? 'guide-mode-auto' : 'guide-mode-manual'}`}>
      {mode}
    </span>
  );
};

const RspBadge = ({ code }) => (
  <span className="guide-rsp-badge">{code}</span>
);

const FlowDiagram = ({ main, tail, labels }) => (
  <div className="guide-flow">
    <div className="guide-flow-label">{labels.flowOverview}</div>
    <div className="guide-flow-row">
      {main.map((node, i) => (
        <div key={`main-${i}`} className="guide-flow-node-wrap">
          {i > 0 && <span className="guide-flow-arrow" aria-hidden>→</span>}
          <a href={`#guide-step-${node.step}`} className="guide-flow-node">
            <span className="guide-flow-node-num">{labels.step} {node.step}</span>
            <span className="guide-flow-node-label">{node.label}</span>
            {node.tag && <span className="guide-flow-node-tag">{node.tag}</span>}
          </a>
        </div>
      ))}
    </div>
    <div className="guide-flow-branch" aria-hidden>↓</div>
    <div className="guide-flow-row guide-flow-row-tail">
      {tail.map((node, i) => (
        <div key={`tail-${i}`} className="guide-flow-node-wrap">
          {i > 0 && <span className="guide-flow-arrow" aria-hidden>→</span>}
          <a href={`#guide-step-${node.step}`} className="guide-flow-node guide-flow-node-secondary">
            {node.step && <span className="guide-flow-node-num">{labels.step} {node.step}</span>}
            <span className="guide-flow-node-label">{node.label}</span>
            {node.tag && <span className="guide-flow-node-tag">{node.tag}</span>}
          </a>
        </div>
      ))}
    </div>
  </div>
);

const GuideTable = ({ headers, rows, highlightLast, centerCols = [], flowCol, modeCol, rspCol }) => (
  <div className="table-wrap">
    <table className="table table-striped guide-table">
      <thead>
        <tr>
          {headers.map((h, i) => (
            <th
              key={h}
              className={centerCols.includes(i) ? 'text-center' : undefined}
            >
              {h}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, i) => (
          <tr key={i}>
            {row.map((cell, j) => (
              <td
                key={j}
                className={centerCols.includes(j) ? 'text-center' : undefined}
              >
                {flowCol === j
                  ? <FlowBadge flow={cell} />
                  : modeCol === j
                    ? <ModeBadge mode={cell} />
                    : rspCol === j
                      ? <RspBadge code={cell} />
                      : highlightLast && j === row.length - 1
                        ? <RspBadge code={cell} />
                        : renderRichText(cell)}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);

const IpnLogicSteps = ({ steps, whenLabel }) => (
  <ol className="guide-ipn-logic">
    {steps.map((item) => (
      <li key={item.step} className="guide-ipn-logic-item">
        <div className="guide-ipn-logic-head">
          <span className="guide-ipn-logic-num">{item.step}</span>
          <span className="guide-ipn-logic-title">{item.title}</span>
          <RspBadge code={item.rsp} />
        </div>
        <p className="guide-ipn-logic-detail">{renderRichText(item.detail)}</p>
        <p className="guide-ipn-logic-when">
          <span className="guide-ipn-logic-when-label">{whenLabel}:</span> {item.when}
        </p>
      </li>
    ))}
  </ol>
);

const ScreenPath = ({ path }) => {
  const staticPath = path.replace('{id}', ':id');
  const canLink = !path.includes('{id}');
  if (canLink) {
    return <Link to={path} className="guide-path-link"><code>{path}</code></Link>;
  }
  return <code className="guide-inline-code">{staticPath}</code>;
};

const StepCard = ({ step, labels }) => (
  <section className="guide-step" id={`guide-step-${step.id}`}>
    <div className="guide-step-header">
      <span className="guide-step-badge">{labels.step} {step.id}</span>
      <h3 className="guide-step-title">{step.title}</h3>
      {step.path && (
        <Link to={step.path} className="btn btn-light-primary btn-sm guide-step-link">
          <i className="ri-arrow-right-line" /> {labels.goTo}: {step.pathLabel}
        </Link>
      )}
    </div>

    {step.items && (
      <ol className="guide-list">
        {step.items.map((item) => (
          <li key={item}>{renderRichText(item)}</li>
        ))}
      </ol>
    )}

    {step.note && <p className="guide-note">{renderRichText(step.note)}</p>}

    {step.partnerFields && (
      <>
        <h4 className="guide-subtitle">{labels.partnerFieldsTitle}</h4>
        <GuideTable
          headers={[labels.field, labels.description]}
          rows={step.partnerFields}
        />
        <h4 className="guide-subtitle">{labels.flowDiffTitle}</h4>
        <GuideTable
          headers={[labels.flow, labels.feature]}
          rows={step.flowDiff}
          flowCol={0}
        />
      </>
    )}

    {step.params && (
      <>
        <h4 className="guide-subtitle">{labels.paramsTitle}</h4>
        <GuideTable
          headers={[labels.param, labels.description]}
          rows={step.params}
        />
      </>
    )}

    {step.runModes && (
      <>
        <h4 className="guide-subtitle">{labels.runModesTitle}</h4>
        <ul className="guide-list guide-list-unordered">
          {step.runModes.map((mode) => (
            <li key={mode}>{renderRichText(mode)}</li>
          ))}
        </ul>
      </>
    )}

    {step.ipnCases && (
      <>
        <h4 className="guide-subtitle">{labels.ipnCasesTitle}</h4>
        <GuideTable
          headers={[labels.step, labels.case, labels.scenario, labels.rspCode]}
          rows={step.ipnCases}
          highlightLast
          centerCols={[0, 1, 3]}
        />
        <h4 className="guide-subtitle">{labels.passCriteriaTitle}</h4>
        <p className="guide-pass-criteria">{renderRichText(step.passCriteria)}</p>
        <h4 className="guide-subtitle">{labels.ipnLogicTitle}</h4>
        <IpnLogicSteps steps={step.ipnLogic} whenLabel={labels.when} />
      </>
    )}

    {step.manualItems && (
      <GuideTable
        headers={[labels.item, labels.content, labels.howTo]}
        rows={step.manualItems}
      />
    )}

    {step.saveNote && <p className="guide-text guide-text-muted">{renderRichText(step.saveNote)}</p>}

    {step.resultScreens && (
      <>
        <div className="table-wrap">
          <table className="table table-striped guide-table">
            <thead>
              <tr>
                <th>{labels.screen}</th>
                <th>{labels.description}</th>
              </tr>
            </thead>
            <tbody>
              {step.resultScreens.map(([path, desc]) => (
                <tr key={path}>
                  <td><ScreenPath path={path} /></td>
                  <td>{renderRichText(desc)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {step.exportNote && (
          <>
            <h4 className="guide-subtitle">{labels.exportTitle}</h4>
            <p className="guide-text">{renderRichText(step.exportNote)}</p>
          </>
        )}
      </>
    )}
  </section>
);

const Guide = () => {
  const { locale } = useI18n();
  const content = getGuideContent(locale);
  const { labels } = content;

  return (
    <>
      <Breadcrumbs title={content.title} />
      <div className="card-header guide-page-header">
        <div>
          <h3 className="card-title mb-1">{content.title}</h3>
          <p className="guide-page-subtitle mb-0">{content.subtitle}</p>
        </div>
        <Link to="/sessions/new" className="btn btn-primary btn-sm">
          <i className="ri-add-line" /> {locale === 'en' ? 'New session' : 'Tạo phiên kiểm thử'}
        </Link>
      </div>

      <div className="card-body guide-page-body">
        <p className="guide-intro">{content.intro}</p>

        <FlowDiagram main={content.flowMain} tail={content.flowTail} labels={labels} />

        <nav className="guide-toc" aria-label={content.title}>
          <div className="guide-toc-title">{labels.tocTitle}</div>
          <ul className="guide-toc-grid">
            {content.steps.map((step) => (
              <li key={step.id}>
                <a href={`#guide-step-${step.id}`} className="guide-toc-link">
                  <span className="guide-toc-num">{step.id}</span>
                  <span className="guide-toc-text">{step.title}</span>
                </a>
              </li>
            ))}
          </ul>
        </nav>

        {content.steps.map((step) => (
          <StepCard key={step.id} step={step} labels={labels} />
        ))}

        <section className="guide-checklist">
          <h3 className="guide-checklist-title">{content.checklistTitle}</h3>
          <GuideTable
            headers={['#', labels.task, labels.mode]}
            rows={content.checklist.map((row, i) => [String(i + 1), row[0], row[1]])}
            centerCols={[0, 2]}
            modeCol={2}
          />
        </section>
      </div>
    </>
  );
};

export default Guide;
