export const FLOW_CODES = ['PAY', 'TOKEN', 'RECURRING', 'INSTALMENT'];

export function getFlowLabel(t, flow) {
  if (!flow) return '';
  const key = `flows.${flow}`;
  const label = t(key);
  return label === key ? String(flow) : label;
}
