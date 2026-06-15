import { useI18n } from '../../i18n/useI18n';

const basename = process.env.REACT_APP_BASENAME || '';

const LANGUAGES = [
  { code: 'vi', label: 'VI', flag: `${basename}/media/flag-vn.svg`, aria: 'Tiếng Việt' },
  { code: 'en', label: 'EN', flag: `${basename}/media/flag-gb.svg`, aria: 'English' },
];

const LanguageSwitcher = ({ className = '' }) => {
  const { locale, setLocale } = useI18n();

  return (
    <div className={`lang-switch ${className}`.trim()} role="group" aria-label="Ngôn ngữ">
      {LANGUAGES.map((lang, index) => (
        <button
          key={lang.code}
          type="button"
          className={`lang-switch-btn${locale === lang.code ? ' active' : ''}${index > 0 ? ' lang-switch-btn-divider' : ''}`}
          onClick={() => setLocale(lang.code)}
          aria-label={lang.aria}
          aria-pressed={locale === lang.code}
        >
          <img src={lang.flag} alt="" className="lang-flag-icon" />
          <span className="lang-switch-label">{lang.label}</span>
        </button>
      ))}
    </div>
  );
};

export default LanguageSwitcher;
