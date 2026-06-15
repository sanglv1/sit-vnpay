import { useState } from 'react';
import { useDispatch } from 'react-redux';
import {
  MutationCache,
  QueryCache,
  QueryClient,
  QueryClientProvider,
} from '@tanstack/react-query';
import { appActions } from '../stores';

export default function QueryProvider({ children }) {
  const dispatch = useDispatch();

  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: {
      queries: {
        retry: 1,
        refetchOnWindowFocus: false,
        staleTime: 30_000,
      },
    },
    queryCache: new QueryCache({
      onError: (error, query) => {
        if (query.meta?.silent) return;
        dispatch(appActions.flash(error.message, 'danger'));
      },
    }),
    mutationCache: new MutationCache({
      onError: (error, _variables, _context, mutation) => {
        if (mutation.meta?.silent) return;
        dispatch(appActions.flash(error.message, 'danger'));
      },
    }),
  }));

  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
}
