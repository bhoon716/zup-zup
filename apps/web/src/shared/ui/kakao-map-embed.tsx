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
  const [isApiError, setIsApiError] = useState(false);

  const mapJsKey = process.env.NEXT_PUBLIC_KAKAO_MAP_JS_KEY ?? "";
  const currentOrigin = typeof window !== "undefined" ? window.location.origin : "";

  useEffect(() => {
    if (!mapContainerRef.current) {
      return;
    }

    if (!mapJsKey) {
      setErrorMessage("카카오맵 키가 설정되지 않았습니다.");
      setIsApiError(true);
      return;
    }

    let cancelled = false;

    const run = async () => {
      setLoading(true);
      setErrorMessage(null);
      setIsApiError(false);

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

        if (renderResult.status === "ZERO_RESULT") {
          setErrorMessage("검색된 장소가 없습니다. 건물명이나 주소를 다시 확인해 주세요.");
          setIsApiError(false);
        } else if (renderResult.status !== "OK") {
          setErrorMessage("지도를 불러오는 중 오류가 발생했습니다.");
          setIsApiError(true);
        }
      } catch (error) {
        if (cancelled) {
          return;
        }

        const message = error instanceof Error
          ? error.message
          : "지도를 불러오는 중 오류가 발생했습니다.";
        setErrorMessage(message);
        setIsApiError(true);
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
            <p className="text-[11px] text-amber-600 font-medium">{errorMessage}</p>
            {isApiError && currentOrigin && (
              <p className="text-[10px] text-muted-foreground leading-relaxed">
                카카오 콘솔 Web 도메인에 <span className="font-bold text-foreground">{currentOrigin}</span> 등록 여부를 확인해 주세요.
              </p>
            )}
            {!isApiError && (
              <div className="flex items-center gap-2 pt-0.5">
                <a 
                  href={`https://map.kakao.com/link/search/${encodeURIComponent(query)}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-[10px] text-primary hover:underline font-medium"
                >
                  카카오맵에서 직접 찾기 →
                </a>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
