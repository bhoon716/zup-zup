"use client";

import { useEffect, useRef, useState } from "react";
import { Loader2 } from "lucide-react";
import { renderKakaoMapByKeyword } from "@/shared/lib/kakao-map";
import { cn } from "@/shared/lib/utils";

interface KakaoMapEmbedProps {
  query: string;
  className?: string;
}

/**
 * 키워드를 기반으로 카카오맵을 특정 컨테이너에 임베드하여 보여주는 컴포넌트입니다.
 */
export function KakaoMapEmbed({ query, className }: KakaoMapEmbedProps) {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const mapJsKey = process.env.NEXT_PUBLIC_KAKAO_MAP_JS_KEY ?? "";
  const currentOrigin = typeof window !== "undefined" ? window.location.origin : "";

  useEffect(() => {
    if (!mapContainerRef.current) {
      return;
    }

    if (!mapJsKey) {
      setErrorMessage("카카오맵 키가 설정되지 않았습니다.");
      return;
    }

    let cancelled = false;

    const run = async () => {
      setLoading(true);
      setErrorMessage(null);

      try {
        const renderResult = await renderKakaoMapByKeyword({
          container: mapContainerRef.current as HTMLElement,
          appKey: mapJsKey,
          keyword: query,
          level: 4,
        });

        if (cancelled) {
          return;
        }

        if (renderResult.status !== "OK") {
          setErrorMessage("위치를 정확히 찾지 못했습니다. 외부 지도로 확인해 주세요.");
        }
      } catch (error) {
        if (cancelled) {
          return;
        }

        const message = error instanceof Error
          ? error.message
          : "지도를 불러오는 중 오류가 발생했습니다.";
        setErrorMessage(message);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    run();

    return () => {
      cancelled = true;
    };
  }, [mapJsKey, query]);

  return (
    <div className={cn("bg-background", className)}>
      <div className="relative">
        <div ref={mapContainerRef} className="h-56 w-full bg-muted/40" />

        {loading && (
          <div className="absolute inset-0 flex items-center justify-center bg-background/70">
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Loader2 className="w-4 h-4 animate-spin" />
              지도 로딩 중...
            </div>
          </div>
        )}
      </div>

      <div className="px-3 py-2 border-t border-border bg-background space-y-1.5">
        {errorMessage && (
          <div className="space-y-1">
            <p className="text-[11px] text-amber-600">{errorMessage}</p>
            {currentOrigin && (
              <p className="text-[11px] text-muted-foreground">
                카카오 콘솔 Web 도메인에 <span className="font-medium">{currentOrigin}</span> 등록 여부를 확인해 주세요.
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
