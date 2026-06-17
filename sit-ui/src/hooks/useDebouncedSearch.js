import { useEffect, useState } from 'react';

export function useDebouncedSearch(initialValue = '', delayMs = 300) {
  const [search, setSearch] = useState(initialValue);
  const [debouncedSearch, setDebouncedSearch] = useState(initialValue);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), delayMs);
    return () => clearTimeout(timer);
  }, [search, delayMs]);

  return {
    search,
    setSearch,
    debouncedSearch,
    hasSearch: Boolean(debouncedSearch.trim()),
  };
}
