import { getCampusMapQuery } from "@/shared/lib/map-links";

const KAKAO_MAP_SCRIPT_ID = "kakao-map-sdk";

const DEFAULT_CENTER = { lat: 35.846521, lng: 127.129558 };




export function escapeHtml(str: string): string {
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}


const JBNU_CAMPUS_BOUNDS = {
  minLat: 35.8380,
  maxLat: 35.8580,
  minLng: 127.1200,
  maxLng: 127.1420,
};


const OFF_CAMPUS_KEYWORDS = ["익산", "고창", "남원", "새만금", "군산", "병원", "혁신도시"];

interface KakaoLatLngInstance {
  getLat(): number;
  getLng(): number;
}

interface KakaoMapInstance {
  setCenter(latLng: KakaoLatLngInstance): void;
  setLevel(level: number): void;
}

interface KakaoMarkerInstance {
  setMap(map: KakaoMapInstance | null): void;
}

interface KakaoInfoWindowInstance {
  open(map: KakaoMapInstance, marker: KakaoMarkerInstance): void;
  close(): void;
}

export interface KakaoPlaceResult {
  id: string;
  place_name: string;
  address_name: string;
  road_address_name?: string;
  x: string;
  y: string;
}

export type KakaoSearchStatus = "OK" | "ZERO_RESULT" | "ERROR";

export interface KakaoMapApi {
  maps: {
    load(callback: () => void): void;
    LatLng: new (lat: number, lng: number) => KakaoLatLngInstance;
    Map: new (
      container: HTMLElement,
      options: { center: KakaoLatLngInstance; level: number },
    ) => KakaoMapInstance;
    Marker: new (options: {
      map: KakaoMapInstance;
      position: KakaoLatLngInstance;
    }) => KakaoMarkerInstance;
    InfoWindow: new (options: {
      content: string;
    }) => KakaoInfoWindowInstance;
    services: {
      Status: Record<KakaoSearchStatus, KakaoSearchStatus>;
      Places: new () => {
        keywordSearch(
          keyword: string,
          callback: (
            data: KakaoPlaceResult[],
            status: KakaoSearchStatus,
          ) => void,
          options?: { location?: KakaoLatLngInstance; radius?: number; size?: number; page?: number },
        ): void;
      };
    };
  };
}

declare global {
  interface Window {
    kakao?: KakaoMapApi;
  }
}

let kakaoSdkPromise: Promise<KakaoMapApi> | null = null;


function isKakaoReady(kakao?: KakaoMapApi): kakao is KakaoMapApi {
  return Boolean(kakao?.maps?.services);
}


function getKakaoScript(appKey: string): HTMLScriptElement {
  const script = document.createElement("script");
  script.id = KAKAO_MAP_SCRIPT_ID;
  script.async = true;
  script.src = `https://dapi.kakao.com/v2/maps/sdk.js?autoload=false&libraries=services&appkey=${appKey}`;
  return script;
}




export async function loadKakaoMapSdk(appKey: string): Promise<KakaoMapApi> {
  if (typeof window === "undefined") {
    throw new Error("브라우저 환경에서만 카카오맵 SDK를 로드할 수 있습니다.");
  }

  if (!appKey) {
    throw new Error("카카오맵 JavaScript 키가 설정되지 않았습니다.");
  }

  if (isKakaoReady(window.kakao)) {
    return window.kakao;
  }

  if (kakaoSdkPromise) {
    return kakaoSdkPromise;
  }

  kakaoSdkPromise = new Promise<KakaoMapApi>((resolve, reject) => {
    const existingScript = document.getElementById(KAKAO_MAP_SCRIPT_ID) as HTMLScriptElement | null;
    const script = existingScript ?? getKakaoScript(appKey);

    const onLoad = () => {
      if (!window.kakao?.maps) {
        reject(new Error("카카오맵 SDK 로드에 실패했습니다."));
        kakaoSdkPromise = null;
        return;
      }

      window.kakao.maps.load(() => {
        if (!isKakaoReady(window.kakao)) {
          reject(new Error("카카오맵 서비스 모듈 초기화에 실패했습니다."));
          kakaoSdkPromise = null;
          return;
        }
        resolve(window.kakao);
      });
    };

    script.addEventListener("load", onLoad, { once: true });
    script.addEventListener(
      "error",
      () => {
        reject(new Error("카카오맵 SDK 스크립트를 불러오지 못했습니다."));
        kakaoSdkPromise = null;
      },
      { once: true },
    );

    if (!existingScript) {
      document.head.appendChild(script);
    }
  });

  return kakaoSdkPromise;
}

export interface KakaoMapRenderResult {
  status: KakaoSearchStatus;
  place?: KakaoPlaceResult;
}




export async function renderKakaoMapByKeyword(params: {
  container: HTMLElement;
  appKey: string;
  keyword: string;
  level?: number;
}): Promise<KakaoMapRenderResult> {
  const { container, appKey, keyword, level = 4 } = params;
  const kakao = await loadKakaoMapSdk(appKey);

  const initialCenter = new kakao.maps.LatLng(DEFAULT_CENTER.lat, DEFAULT_CENTER.lng);
  const map = new kakao.maps.Map(container, { center: initialCenter, level });

  const normalizedKeyword = getCampusMapQuery(keyword) ?? keyword;
  const isOffCampus = OFF_CAMPUS_KEYWORDS.some((k) => normalizedKeyword.includes(k) || keyword.includes(k));

  return new Promise<KakaoMapRenderResult>((resolve) => {
    const places = new kakao.maps.services.Places();

    const doSearch = (options?: { location?: KakaoLatLngInstance; radius?: number }) => {
      places.keywordSearch(
        normalizedKeyword,
        (data, status) => {
          if (status !== kakao.maps.services.Status.OK || data.length === 0) {

            if (options?.location) {
              doSearch(undefined);
              return;
            }
            map.setCenter(initialCenter);
            resolve({ status });
            return;
          }


          const campusPlace = !isOffCampus
            ? data.find((p) => {
                const lat = Number(p.y);
                const lng = Number(p.x);
                return (
                  lat >= JBNU_CAMPUS_BOUNDS.minLat &&
                  lat <= JBNU_CAMPUS_BOUNDS.maxLat &&
                  lng >= JBNU_CAMPUS_BOUNDS.minLng &&
                  lng <= JBNU_CAMPUS_BOUNDS.maxLng
                );
              })
            : undefined;

          const place = campusPlace ?? data[0];
          const lat = Number(place.y);
          const lng = Number(place.x);
          if (Number.isNaN(lat) || Number.isNaN(lng)) {
            resolve({ status: "ERROR" });
            return;
          }

          const position = new kakao.maps.LatLng(lat, lng);
          map.setCenter(position);
          map.setLevel(level);

          const marker = new kakao.maps.Marker({ map, position });
          const infoWindow = new kakao.maps.InfoWindow({
            content: `<div style="padding:8px 10px;font-size:12px;line-height:1.3;">${escapeHtml(place.place_name)}</div>`,
          });
          infoWindow.open(map, marker);

          resolve({ status, place });
        },
        options ? { location: options.location, radius: options.radius, size: 10, page: 1 } : { size: 10, page: 1 },
      );
    };


    if (!isOffCampus) {
      doSearch({ location: initialCenter, radius: 1500 });
    } else {
      doSearch(undefined);
    }
  });
}
