import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import Breadcrumbs from '../Shared/Breadcrumbs';
import { useI18n } from '../../i18n/useI18n';
import { useCreateSessionMutation, useTestMetadataQuery } from '../../api/hooks';

const SessionCreate = () => {
  const navigate = useNavigate();
  const { t } = useI18n();
  const { data: metadata, isLoading } = useTestMetadataQuery();
  const createSession = useCreateSessionMutation();
  const partners = metadata?.partners ?? [];
  const { register, handleSubmit, formState: { errors } } = useForm();

  const onSubmit = async (values) => {
    try {
      const session = await createSession.mutateAsync({
        partnerId: Number(values.partnerId),
        note: values.note,
      });
      navigate(`/sessions/${session.id}/auto`);
    } catch {
      // mutation cache handles flash
    }
  };

  return (
    <>
      <Breadcrumbs title={t('sessions.createTitle')} />
      <div className="card-header sit-page-header">
        <h3 className="card-title mb-0">{t('sessions.createHeading')}</h3>
        <p className="sit-page-subtitle mb-0">{t('sessions.createSubtitle')}</p>
      </div>
      <div className="card-body">
        {isLoading && (
          <div className="sit-list-loading">
            <i className="ri-loader-4-line" aria-hidden="true" />
            <span>{t('common.loading')}</span>
          </div>
        )}
        {!isLoading && (
          <form onSubmit={handleSubmit(onSubmit)}>
            <div className="row">
              <div className="col-lg-6">
                <label className="form-label">{t('sessions.terminal')} *</label>
                <select
                  className="form-select"
                  defaultValue=""
                  {...register('partnerId', { required: t('sessions.selectTerminal') })}
                >
                  <option value="" disabled>{t('common.select')}</option>
                  {partners.map((p) => (
                    <option key={p.id} value={p.id}>{p.name} ({p.tmnCode})</option>
                  ))}
                </select>
                {errors.partnerId && <div className="fv-help-block">{errors.partnerId.message}</div>}
              </div>
              <div className="col-lg-6">
                <label className="form-label">{t('common.note')}</label>
                <input className="form-control" {...register('note')} placeholder={t('common.optional')} />
              </div>
            </div>
            <div className="form-footer">
              <button type="button" className="btn btn-light-primary" onClick={() => navigate('/sessions')}>
                {t('common.cancel')}
              </button>
              <button type="submit" className="btn btn-primary">{t('sessions.createButton')}</button>
            </div>
          </form>
        )}
      </div>
    </>
  );
};

export default SessionCreate;
