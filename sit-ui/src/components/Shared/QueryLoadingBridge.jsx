import { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { useIsMutating } from '@tanstack/react-query';
import { appActions } from '../../stores';

/** Sync user-initiated mutations to the global loading overlay (not background reads). */
export default function QueryLoadingBridge() {
  const dispatch = useDispatch();
  const mutating = useIsMutating({
    predicate: (mutation) => !mutation.meta?.silent,
  });
  const busy = mutating > 0;

  useEffect(() => {
    if (busy) {
      dispatch(appActions.loadingOn());
    } else {
      dispatch(appActions.loadingOff());
    }
  }, [busy, dispatch]);

  return null;
}
