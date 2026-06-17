const SitSearchBox = ({ value, onChange, placeholder, ariaLabel }) => (
  <div className="table-top">
    <div className="sit-search-box">
      <i className="ri-search-line" aria-hidden />
      <input
        type="search"
        className="form-control"
        placeholder={placeholder}
        aria-label={ariaLabel || placeholder}
        value={value}
        onChange={onChange}
      />
    </div>
  </div>
);

export default SitSearchBox;
