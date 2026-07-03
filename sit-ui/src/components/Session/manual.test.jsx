import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import SessionManual from './manual';

const mockDispatch = jest.fn();
const mockFlash = jest.fn((message, flashType = 'success') => ({ type: 'APP_FLASH', message, flashType }));

jest.mock('react-redux', () => ({
  useDispatch: () => mockDispatch,
}));

jest.mock('react-router-dom', () => ({
  useParams: () => ({ sessionId: '10' }),
}));

jest.mock('./SessionHeader', () => () => <div>Session Header</div>);
jest.mock('./AcceptanceTabs', () => () => <div>Acceptance Tabs</div>);

jest.mock('../../stores', () => ({
  appActions: {
    flash: (...args) => mockFlash(...args),
  },
}));

jest.mock('../../i18n/useI18n', () => ({
  useI18n: () => ({
    t: (key) => {
      const messages = {
        'common.loading': 'Loading...',
        'sessions.manualTitle': 'Manual acceptance',
        'sessions.manualSubtitle': 'Manual subtitle',
        'sessions.saveQc': 'Save',
        'sessions.manualImageMaxSize': 'Image max 3MB',
        'sessions.manualPreAuthSection': 'PreAuth section',
        'sessions.manualPreAuthEvidenceHint': 'PreAuth hint',
        'sessions.preAuthScenario5': 'Scenario 5',
        'sessions.manualPreAuthScreenshot': 'Screenshot',
        'sessions.manualSystemSection': 'System checks',
        'sessions.manualExceptionLabel': 'Exception',
        'sessions.manualExceptionDesc': 'Exception desc',
        'sessions.manualWhitelistLabel': 'Whitelist',
        'sessions.manualWhitelistDesc': 'Whitelist desc',
        'sessions.manualLogLabel': 'Log',
        'sessions.manualLogDesc': 'Log desc',
        'common.handled': 'Handled',
        'common.notHandled': 'Not handled',
        'common.yesDone': 'Done',
        'common.noFailed': 'Failed',
        'sessions.manualQcNote': 'Note',
        'sessions.manualPreAuthRequestLog': 'Input',
        'sessions.manualPreAuthRequestLogPlaceholder': 'Input placeholder',
        'sessions.manualPreAuthResponseLog': 'Output',
        'sessions.manualPreAuthResponseLogPlaceholder': 'Output placeholder',
      };
      return messages[key] || key;
    },
  }),
}));

const mockHooks = {
  useSessionQuery: jest.fn(),
  useSessionWorkspaceQuery: jest.fn(),
  useManualAcceptanceQuery: jest.fn(),
  useSaveManualAcceptanceMutation: jest.fn(),
};

jest.mock('../../api/hooks', () => ({
  useSessionQuery: (...args) => mockHooks.useSessionQuery(...args),
  useSessionWorkspaceQuery: (...args) => mockHooks.useSessionWorkspaceQuery(...args),
  useManualAcceptanceQuery: (...args) => mockHooks.useManualAcceptanceQuery(...args),
  useSaveManualAcceptanceMutation: (...args) => mockHooks.useSaveManualAcceptanceMutation(...args),
}));

describe('SessionManual', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockHooks.useSessionQuery.mockReturnValue({
      data: { id: 10, partnerId: 1 },
    });
    mockHooks.useSessionWorkspaceQuery.mockReturnValue({
      data: { partnerFlow: 'PREAUTH' },
    });
    mockHooks.useManualAcceptanceQuery.mockReturnValue({
      data: null,
      isFetched: true,
    });
    mockHooks.useSaveManualAcceptanceMutation.mockReturnValue({
      mutateAsync: jest.fn(),
    });
  });

  test('shows preauth section when partner flow is PREAUTH', () => {
    render(<SessionManual />);
    expect(screen.getByText('PreAuth section')).toBeInTheDocument();
    expect(screen.getByText('PreAuth hint')).toBeInTheDocument();
  });

  test('rejects image larger than 3MB and dispatches flash', () => {
    const { container } = render(<SessionManual />);
    const fileInputs = container.querySelectorAll('input[type="file"]');
    expect(fileInputs.length).toBeGreaterThan(0);

    const oversizeBytes = new Uint8Array(3 * 1024 * 1024 + 1);
    const oversizedFile = new File([oversizeBytes], 'oversize.png', { type: 'image/png' });

    fireEvent.change(fileInputs[0], { target: { files: [oversizedFile] } });

    expect(mockFlash).toHaveBeenCalledWith('Image max 3MB', 'danger');
    expect(mockDispatch).toHaveBeenCalledTimes(1);
  });
});
