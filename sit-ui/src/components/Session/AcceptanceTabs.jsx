import { NavLink, useParams } from 'react-router-dom';

const AcceptanceTabs = () => {
  const { sessionId } = useParams();

  return (
    <div className="acceptance-tabs">
      <NavLink
        to={`/sessions/${sessionId}/auto`}
        className={({ isActive }) => `acceptance-tab${isActive ? ' active' : ''}`}
      >
        Nghiệm thu tự động
      </NavLink>
      <NavLink
        to={`/sessions/${sessionId}/manual`}
        className={({ isActive }) => `acceptance-tab${isActive ? ' active' : ''}`}
      >
        Nghiệm thu thủ công
      </NavLink>
    </div>
  );
};

export default AcceptanceTabs;
