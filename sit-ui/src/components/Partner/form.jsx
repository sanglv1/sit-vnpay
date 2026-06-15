import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import Breadcrumbs from '../Shared/Breadcrumbs';
import { useI18n } from '../../i18n/useI18n';
import { usePartnerQuery, useSavePartnerMutation } from '../../api/hooks';
import { appActions } from '../../stores';

const FLOWS = ['PAY', 'TOKEN', 'RECURRING', 'INSTALMENT'];

const PartnerForm = () => {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { t } = useI18n();
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    defaultValues: { active: true, flow: 'PAY' },
  });
  const { data: partner } = usePartnerQuery(id, { enabled: isEdit });
  const savePartner = useSavePartnerMutation();

  useEffect(() => {
    if (!partner) return;
    reset({
      name: partner.name,
      flow: partner.flow,
      tmnCode: partner.tmnCode,
      secretKey: partner.secretKey,
      returnUrl: partner.returnUrl,
      ipnUrl: partner.ipnUrl,
      note: partner.note || '',
      active: partner.active,
    });
  }, [partner, reset]);

  const onSubmit = async (values) => {
    const payload = {
      ...values,
      returnUrl: values.returnUrl?.trim() ?? '',
      active: values.active === true || values.active === 'true',
    };
    try {
      await savePartner.mutateAsync({ id: isEdit ? id : null, data: payload });
      dispatch(appActions.flash(t('partners.saved')));
      navigate('/partners');
    } catch {
      // mutation cache handles flash
    }
  };

  const title = isEdit ? t('partners.edit') : t('partners.add');

  return (
    <>
      <Breadcrumbs title={title} />
      <div className="card-header">
        <h3 className="card-title mb-0">{title}</h3>
      </div>
      <div className="card-body">
        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="row">
            <div className="col-lg-6">
              <label className="form-label">{t('partners.terminalName')} *</label>
              <input className="form-control" {...register('name', { required: t('common.required') })} />
              {errors.name && <div className="fv-help-block">{errors.name.message}</div>}
            </div>
            <div className="col-lg-3">
              <label className="form-label">{t('common.flow')} *</label>
              <select className="form-select" {...register('flow', { required: true })}>
                {FLOWS.map((f) => <option key={f} value={f}>{f}</option>)}
              </select>
            </div>
            <div className="col-lg-3">
              <label className="form-label">{t('common.status')}</label>
              <select className="form-select" {...register('active')}>
                <option value="true">{t('common.active')}</option>
                <option value="false">{t('common.inactive')}</option>
              </select>
            </div>
            <div className="col-lg-4">
              <label className="form-label">{t('partners.tmnCode')} *</label>
              <input className="form-control" {...register('tmnCode', { required: t('common.required') })} />
            </div>
            <div className="col-lg-8">
              <label className="form-label">{t('partners.secretKey')} *</label>
              <input className="form-control" {...register('secretKey', { required: t('common.required') })} />
            </div>
            <div className="col-12">
              <label className="form-label">{t('partners.returnUrl')}</label>
              <input
                className="form-control"
                placeholder={t('partners.returnUrlPlaceholder')}
                {...register('returnUrl')}
              />
            </div>
            <div className="col-12">
              <label className="form-label">{t('partners.ipnUrl')} *</label>
              <input className="form-control" {...register('ipnUrl', { required: t('common.required') })} />
            </div>
            <div className="col-12">
              <label className="form-label">{t('common.note')}</label>
              <textarea className="form-control" rows={2} {...register('note')} />
            </div>
          </div>
          <div className="form-footer">
            <Link to="/partners" className="btn btn-light-primary">{t('common.cancel')}</Link>
            <button type="submit" className="btn btn-primary">{t('common.save')}</button>
          </div>
        </form>
      </div>
    </>
  );
};

export default PartnerForm;
