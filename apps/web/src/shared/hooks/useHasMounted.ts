import { useState, useEffect } from 'react';

/**
 * 컴포넌트가 클라이언트에 마운트되었는지 여부를 반환합니다.
 * SSR과 CSR 간의 하이드레이션 불일치(Hydration Mismatch)를 방지하기 위해 사용합니다.
 */
export function useHasMounted() {
  const [hasMounted, setHasMounted] = useState(false);

  useEffect(() => {
    setHasMounted(true);
  }, []);

  return hasMounted;
}
