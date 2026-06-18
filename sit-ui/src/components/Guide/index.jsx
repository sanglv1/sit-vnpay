import { Link } from 'react-router-dom';
import Breadcrumbs from '../Shared/Breadcrumbs';
import { useI18n } from '../../i18n/useI18n';
import { FLOW_CODES, getFlowLabel } from '../../i18n/flowLabels';
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

const FlowBadge = ({ flow }) => {
  const { t } = useI18n();
  return (
    <span className={`guide-flow-badge ${FLOW_BADGE_CLASS[flow] || ''}`}>
      {getFlowLabel(t, flow)}
    </span>
  );
};

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

const FlowDiagram = ({ nodes, labels }) => (
  <div className="guide-flow">
    <div className="guide-flow-label">{labels.flowOverview}</div>
    <div className="guide-flow-track">
      {nodes.map((node, i) => (
        <div key={node.step} className="guide-flow-node-wrap">
          {i > 0 && <span className="guide-flow-arrow" aria-hidden>→</span>}
          <a href={`#guide-step-${node.step}`} className="guide-flow-pill">
            <span className="guide-flow-pill-num">{node.step}</span>
            <span className="guide-flow-pill-label">{node.label}</span>
            {node.tag && <span className="guide-flow-pill-tag">{node.tag}</span>}
          </a>
        </div>
      ))}
    </div>
  </div>
);

const FlowLegend = () => {
  const { t } = useI18n();
  return (
    <div className="guide-flow-legend" aria-label={t('common.flow')}>
      {FLOW_CODES.map((flow) => (
        <FlowBadge key={flow} flow={flow} />
      ))}
    </div>
  );
};

const GUIDE_TABLE_VARIANTS = {
  fields: {
    cols: ['guide-col-field', 'guide-col-desc'],
    center: [],
  },
  ipn: {
    cols: ['guide-col-step', 'guide-col-case', 'guide-col-scenario', 'guide-col-rsp'],
    center: [0, 1, 3],
  },
  manual: {
    cols: ['guide-col-item', 'guide-col-content', 'guide-col-howto'],
    center: [1],
  },
  checklist: {
    cols: ['guide-col-num', 'guide-col-task', 'guide-col-mode'],
    center: [0, 2],
  },
};

const GuideTable = ({
  variant = 'fields',
  headers,
  rows = [],
  highlightLast,
  flowCol,
  modeCol,
  rspCol,
  compact,
}) => {
  const config = GUIDE_TABLE_VARIANTS[variant] || GUIDE_TABLE_VARIANTS.fields;
  const centerCols = config.center;

  return (
    <div className={`sit-list-table-wrap guide-table-wrap${compact ? ' guide-table-wrap--compact' : ''}`}>
      <table className={`table data-table guide-table guide-table--${variant}${compact ? ' guide-table--compact' : ''}`}>
        <colgroup>
          {config.cols.map((colClass) => (
            <col key={colClass} className={colClass} />
          ))}
        </colgroup>
        <thead>
          <tr>
            {headers.map((h, i) => (
              <th
                key={h}
                className={`guide-col ${config.cols[i] || ''}${centerCols.includes(i) ? ' guide-col--center' : ' guide-col--start'}`}
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
                  className={`guide-col ${config.cols[j] || ''}${centerCols.includes(j) ? ' guide-col--center' : ' guide-col--start'}`}
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
};

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
          variant="fields"
          headers={[labels.field, labels.description]}
          rows={step.partnerFields}
          compact
        />
      </>
    )}

    {step.params && (
      <>
        <h4 className="guide-subtitle">{labels.paramsTitle}</h4>
        <GuideTable
          variant="fields"
          headers={[labels.param, labels.description]}
          rows={step.params}
          compact
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
          variant="ipn"
          headers={[labels.step, labels.case, labels.scenario, labels.rspCode]}
          rows={step.ipnCases}
          highlightLast
          compact
        />
        {step.passCriteria && (
          <p className="guide-pass-criteria">{renderRichText(step.passCriteria)}</p>
        )}
        {step.ipnLogic && (
          <>
            <h4 className="guide-subtitle">{labels.ipnLogicTitle}</h4>
            <IpnLogicSteps steps={step.ipnLogic} whenLabel={labels.when} />
          </>
        )}
      </>
    )}

    {step.manualItems && (
      <GuideTable
        variant="manual"
        headers={[labels.item, labels.content, labels.howTo]}
        rows={step.manualItems}
        compact
      />
    )}

    {step.saveNote && <p className="guide-text guide-text-muted">{renderRichText(step.saveNote)}</p>}

    {step.exportNote && (
      <p className="guide-text">{renderRichText(step.exportNote)}</p>
    )}
  </section>
);

const Guide = () => {
  const { t, locale } = useI18n();
  const content = getGuideContent(locale);
  const { labels } = content;
  const flowNodes = [...content.flowMain, ...content.flowTail];

  return (
    <>
      <Breadcrumbs title={content.title} />
      <div className="card-header guide-page-header sit-page-header">
        <div>
          <h3 className="card-title mb-0">{content.title}</h3>
          <p className="sit-page-subtitle mb-0">{content.subtitle}</p>
        </div>
        <Link to="/sessions/new" className="btn btn-primary btn-sm">
          <i className="ri-add-line" /> {t('sessions.create')}
        </Link>
      </div>

      <div className="card-body guide-page-body">
        <div className="guide-top">
          <p className="guide-intro">{content.intro}</p>
          <FlowLegend />
          <FlowDiagram nodes={flowNodes} labels={labels} />
        </div>

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
            variant="checklist"
            headers={['#', labels.task, labels.mode]}
            rows={content.checklist.map((row, i) => [String(i + 1), row[0], row[1]])}
            modeCol={2}
            compact
          />
        </section>
      </div>
    </>
  );
};

export default Guide;
