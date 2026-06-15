import { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { useIsFetching, useIsMutating } from '@tanstack/react-query';
import { appActions } from '../../stores';

/** Sync React Query in-flight state to the global loading overlay. */
export default function QueryLoadingBridge() {
  const dispatch = useDispatch();
  const fetching = useIsFetching();
  const mutating = useIsMutating();
  const busy = fetching > 0 || mutating > 0;

  useEffect(() => {
    if (busy) {
      dispatch(appActions.loadingOn());
    } else {
      dispatch(appActions.loadingOff());
    }
  }, [busy, dispatch]);

  return null;
}
