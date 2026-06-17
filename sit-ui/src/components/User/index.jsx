import { useState } from 'react';

import { useForm } from 'react-hook-form';

import { useDispatch } from 'react-redux';

import Breadcrumbs from '../Shared/Breadcrumbs';
import SitSearchBox from '../Shared/SitSearchBox';

import { useI18n } from '../../i18n/useI18n';
import { useDebouncedSearch } from '../../hooks/useDebouncedSearch';

import {
  useDeleteUserMutation,
  useResetPasswordMutation,
  useSaveUserMutation,
  useUpdateUserStatusMutation,
  useUsersQuery,
} from '../../api/hooks';

import { appActions } from '../../stores';



const ROLES = [

  { value: 'ADMIN', label: 'ADMIN' },

  { value: 'MERCHANT_QC', label: 'MERCHANT QC' },

];



const EMPTY_FORM = {

  email: '',

  password: '',

  fullName: '',

  role: 'MERCHANT_QC',

  active: true,

};



const Modal = ({ title, onClose, closeLabel, children }) => (

  <div className="sit-modal-backdrop" onClick={onClose} role="presentation">

    <div className="sit-modal" onClick={(e) => e.stopPropagation()} role="dialog">

      <div className="sit-modal-header">

        <h4 className="sit-modal-title">{title}</h4>

        <button type="button" className="sit-modal-close" onClick={onClose} aria-label={closeLabel}>

          <i className="ri-close-line" />

        </button>

      </div>

      {children}

    </div>

  </div>

);



const UserList = () => {

  const dispatch = useDispatch();

  const { t } = useI18n();

  const { search, setSearch, debouncedSearch, hasSearch } = useDebouncedSearch();

  const [formOpen, setFormOpen] = useState(false);

  const [resetOpen, setResetOpen] = useState(false);

  const [editing, setEditing] = useState(null);

  const [resetUser, setResetUser] = useState(null);



  const { register, handleSubmit, reset, formState: { errors } } = useForm({ defaultValues: EMPTY_FORM });

  const resetForm = useForm({ defaultValues: { password: '' } });

  const saveUser = useSaveUserMutation();
  const resetPassword = useResetPasswordMutation();
  const updateStatus = useUpdateUserStatusMutation();
  const deleteUser = useDeleteUserMutation();
  const { data: users = [] } = useUsersQuery(debouncedSearch);



  const openCreate = () => {

    setEditing(null);

    reset(EMPTY_FORM);

    setFormOpen(true);

  };



  const openEdit = (user) => {

    setEditing(user);

    reset({

      email: user.email,

      password: '',

      fullName: user.fullName,

      role: user.role,

      active: user.active,

    });

    setFormOpen(true);

  };



  const closeForm = () => {

    setFormOpen(false);

    setEditing(null);

  };



  const onSubmit = async (values) => {

    if (!editing && (!values.password || values.password.length < 6)) {

      dispatch(appActions.flash(t('users.passwordMin'), 'danger'));

      return;

    }

    try {

      const payload = {

        email: values.email,

        fullName: values.fullName,

        role: values.role,

        active: values.active === true || values.active === 'true',

      };

      if (values.password) {

        payload.password = values.password;

      }

      await saveUser.mutateAsync({ id: editing?.id ?? null, data: payload });

      dispatch(appActions.flash(editing ? t('users.updated') : t('users.saved')));

      closeForm();

    } catch {

      // mutation cache handles API errors

    }

  };



  const openResetPassword = (user) => {

    setResetUser(user);

    resetForm.reset({ password: '' });

    setResetOpen(true);

  };



  const onResetPassword = async (values) => {

    try {

      await resetPassword.mutateAsync({ id: resetUser.id, password: values.password });

      dispatch(appActions.flash(t('users.resetSuccess')));

      setResetOpen(false);

      setResetUser(null);

    } catch {

      // mutation cache handles API errors

    }

  };



  const toggleStatus = async (user) => {

    const next = !user.active;

    const confirmKey = next ? 'users.confirmActivate' : 'users.confirmDeactivate';

    if (!window.confirm(t(confirmKey, { name: user.fullName }))) return;

    try {

      await updateStatus.mutateAsync({ id: user.id, active: next });

      dispatch(appActions.flash(next ? t('users.statusActivated') : t('users.statusDeactivated')));

    } catch {

      // mutation cache handles API errors

    }

  };



  const handleDelete = async (user) => {

    if (!window.confirm(t('users.confirmDelete', { name: user.fullName }))) return;

    try {

      await deleteUser.mutateAsync(user.id);

      dispatch(appActions.flash(t('users.deleted')));

    } catch {

      // mutation cache handles API errors

    }

  };



  return (

    <>

      <Breadcrumbs title={t('users.title')} />

      <div className="card-header">

        <div className="d-flex justify-content-between align-items-center flex-wrap gap-2">

          <div>

            <h3 className="card-title mb-0">{t('users.title')}</h3>

            <p className="text-muted mb-0 mt-1" style={{ fontSize: 13 }}>

              {t('users.subtitle')}

            </p>

          </div>

          <button type="button" className="btn btn-primary btn-sm" onClick={openCreate}>

            <i className="ri-add-line" /> {t('users.add')}

          </button>

        </div>

      </div>

      <div className="card-body">

        <SitSearchBox
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={t('users.searchPlaceholder')}
        />

        <div className="table-wrap">

          <table className="table table-striped data-table user-table">

            <colgroup>

              <col className="col-name" />

              <col className="col-email" />

              <col className="col-role" />

              <col className="col-status" />

              <col className="col-actions" />

            </colgroup>

            <thead>

              <tr>

                <th className="text-start">{t('users.fullName')}</th>

                <th className="text-start">{t('common.email')}</th>

                <th className="text-center">{t('users.role')}</th>

                <th className="text-center">{t('common.status')}</th>

                <th className="text-end" />

              </tr>

            </thead>

            <tbody>

              {users.map((u) => (

                <tr key={u.id}>

                  <td title={u.fullName}>{u.fullName}</td>

                  <td title={u.email}>{u.email}</td>

                  <td className="text-center">

                    <span className={`badge ${u.role === 'ADMIN' ? 'badge-info' : 'badge-warning'}`}>

                      {u.roleLabel}

                    </span>

                  </td>

                  <td className="text-center">

                    <span className={`badge ${u.active ? 'badge-success' : 'badge-danger'}`}>

                      {u.active ? t('common.activated') : t('common.deactivated')}

                    </span>

                  </td>

                  <td className="text-end">

                    <button type="button" className="btn btn-icon" title={t('common.edit')} onClick={() => openEdit(u)}>

                      <i className="ri-edit-line text-primary" />

                    </button>

                    <button

                      type="button"

                      className="btn btn-icon"

                      title={t('common.resetPassword')}

                      onClick={() => openResetPassword(u)}

                    >

                      <i className="ri-key-2-line text-warning" />

                    </button>

                    <button

                      type="button"

                      className="btn btn-icon"

                      title={u.active ? t('common.deactivate') : t('common.activate')}

                      onClick={() => toggleStatus(u)}

                    >

                      <i className={`ri-user-${u.active ? 'unfollow' : 'add'}-line text-muted`} />

                    </button>

                    <button type="button" className="btn btn-icon" title={t('common.delete')} onClick={() => handleDelete(u)}>

                      <i className="ri-delete-bin-line text-danger" />

                    </button>

                  </td>

                </tr>

              ))}

            </tbody>

          </table>

          {users.length === 0 && (

            <p className="text-muted text-center">
              {hasSearch ? t('common.searchEmpty') : t('users.empty')}
            </p>

          )}

        </div>

      </div>



      {formOpen && (

        <Modal

          title={editing ? t('users.edit') : t('users.add')}

          onClose={closeForm}

          closeLabel={t('common.close')}

        >

          <form onSubmit={handleSubmit(onSubmit)} className="sit-modal-body">

            <div className="mb-3">

              <label className="form-label">{t('common.email')} *</label>

              <input className="form-control" type="email" {...register('email', { required: t('common.required') })} />

              {errors.email && <div className="fv-help-block">{errors.email.message}</div>}

            </div>

            <div className="mb-3">

              <label className="form-label">

                {editing ? t('users.passwordOptional') : `${t('users.password')} *`}

              </label>

              <input className="form-control" type="password" autoComplete="new-password" {...register('password')} />

            </div>

            <div className="mb-3">

              <label className="form-label">{t('users.fullName')} *</label>

              <input

                className="form-control"

                placeholder={t('users.fullNamePlaceholder')}

                {...register('fullName', { required: t('common.required') })}

              />

              {errors.fullName && <div className="fv-help-block">{errors.fullName.message}</div>}

            </div>

            <div className="mb-3">

              <label className="form-label">{t('users.role')} *</label>

              <select className="form-select" {...register('role', { required: true })}>

                {ROLES.map((r) => (

                  <option key={r.value} value={r.value}>{r.label}</option>

                ))}

              </select>

            </div>

            <div className="mb-3">

              <label className="form-label">{t('common.status')}</label>

              <select className="form-select" {...register('active')}>

                <option value="true">{t('common.activated')}</option>

                <option value="false">{t('common.deactivated')}</option>

              </select>

            </div>

            <div className="sit-modal-footer">

              <button type="button" className="btn btn-light-primary" onClick={closeForm}>{t('common.cancel')}</button>

              <button type="submit" className="btn btn-primary">{t('common.save')}</button>

            </div>

          </form>

        </Modal>

      )}



      {resetOpen && resetUser && (

        <Modal

          title={t('users.resetTitle', { name: resetUser.fullName })}

          onClose={() => setResetOpen(false)}

          closeLabel={t('common.close')}

        >

          <form onSubmit={resetForm.handleSubmit(onResetPassword)} className="sit-modal-body">

            <div className="mb-3">

              <label className="form-label">{t('users.passwordNew')} *</label>

              <input

                className="form-control"

                type="password"

                autoComplete="new-password"

                {...resetForm.register('password', {

                  required: t('common.required'),

                  minLength: { value: 6, message: t('users.passwordMinValidation') },

                })}

              />

              {resetForm.formState.errors.password && (

                <div className="fv-help-block">{resetForm.formState.errors.password.message}</div>

              )}

            </div>

            <div className="sit-modal-footer">

              <button type="button" className="btn btn-light-primary" onClick={() => setResetOpen(false)}>

                {t('common.cancel')}

              </button>

              <button type="submit" className="btn btn-primary">{t('common.save')}</button>

            </div>

          </form>

        </Modal>

      )}

    </>

  );

};



export default UserList;

