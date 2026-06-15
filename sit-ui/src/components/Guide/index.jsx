import { Link } from 'react-router-dom';
import Breadcrumbs from '../Shared/Breadcrumbs';
import { useI18n } from '../../i18n/useI18n';
import { getGuideContent } from '../../i18n/guideContent';

const renderInlineCode = (text) => {
  if (typeof text !== 'string' || !text.includes('`')) {
    return text;
  }
  const parts = text.split(/(`[^`]+`)/g);
  return parts.map((part, i) => {
    if (part.startsWith('`') && part.endsWith('`')) {
      return <code key={i} className="guide-inline-code">{part.slice(1, -1)}</code>;
    }
    return part;
  });
};

const GuideTable = ({ headers, rows, highlightLast }) => (
  <div className="table-wrap">
    <table className="table table-striped guide-table">
      <thead>
        <tr>
          {headers.map((h) => <th key={h}>{h}</th>)}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, i) => (
          <tr key={i}>
            {row.map((cell, j) => (
              <td key={j}>
                {highlightLast && j === row.length - 1
                  ? <strong>{cell}</strong>
                  : renderInlineCode(cell)}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  </div>
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
        {step.items.map((item) => <li key={item}>{item}</li>)}
      </ol>
    )}

    {step.note && <p className="guide-note">{renderInlineCode(step.note)}</p>}

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
          {step.runModes.map((mode) => <li key={mode}>{mode}</li>)}
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
        />
        <h4 className="guide-subtitle">{labels.passCriteriaTitle}</h4>
        <p className="guide-text">{renderInlineCode(step.passCriteria)}</p>
        <h4 className="guide-subtitle">{labels.ipnLogicTitle}</h4>
        <pre className="code-block">{step.ipnLogic}</pre>
      </>
    )}

    {step.manualItems && (
      <GuideTable
        headers={[labels.item, labels.content, labels.howTo]}
        rows={step.manualItems}
      />
    )}

    {step.saveNote && <p className="guide-text guide-text-muted">{step.saveNote}</p>}

    {step.resultScreens && (
      <GuideTable
        headers={[labels.screen, labels.description]}
        rows={step.resultScreens}
      />
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
        <pre className="code-block guide-flow-diagram">{content.flowDiagram}</pre>

        <nav className="guide-toc" aria-label={content.title}>
          <div className="guide-toc-title">{labels.step}</div>
          <ul>
            {content.steps.map((step) => (
              <li key={step.id}>
                <a href={`#guide-step-${step.id}`}>
                  {labels.step} {step.id}: {step.title}
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
          />
        </section>
      </div>
    </>
  );
};

export default Guide;
