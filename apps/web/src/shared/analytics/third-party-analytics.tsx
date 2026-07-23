"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";
import Script from "next/script";

const CLARITY_PROJECT_ID_PATTERN = /^[a-z0-9]+$/i;
const GA_MEASUREMENT_ID_PATTERN = /^G-[a-z0-9]+$/i;

type DataLayerEntry = IArguments | unknown[];

declare global {
  interface Window {
    dataLayer: DataLayerEntry[];
    gtag?: (...args: unknown[]) => void;
  }
}

interface ThirdPartyAnalyticsProps {
  environment: string;
  clarityProjectId?: string;
  gaMeasurementId?: string;
}

function validId(value: string | undefined, pattern: RegExp) {
  const normalized = value?.trim();
  return normalized && pattern.test(normalized) ? normalized : undefined;
}

export function buildSanitizedPageView(pathname: string, origin: string, measurementId: string) {
  return {
    page_location: `${origin}${pathname}`,
    page_path: pathname,
    send_to: measurementId,
  };
}

function GooglePageViews({ measurementId }: { measurementId: string }) {
  const pathname = usePathname();

  useEffect(() => {
    window.dataLayer = window.dataLayer || [];
    window.gtag = window.gtag || ((...args: unknown[]) => window.dataLayer.push(args));
    window.gtag(
      "event",
      "page_view",
      buildSanitizedPageView(pathname, window.location.origin, measurementId),
    );
  }, [measurementId, pathname]);

  return null;
}

export function ThirdPartyAnalytics({
  environment,
  clarityProjectId,
  gaMeasurementId,
}: ThirdPartyAnalyticsProps) {
  if (environment !== "production") {
    return null;
  }

  const clarityId = validId(clarityProjectId, CLARITY_PROJECT_ID_PATTERN);
  const gaId = validId(gaMeasurementId, GA_MEASUREMENT_ID_PATTERN);

  return (
    <>
      {clarityId ? (
        <Script
          id="microsoft-clarity"
          data-testid="clarity-script"
          strategy="afterInteractive"
          dangerouslySetInnerHTML={{
            __html: `(function(c,l,a,r,i,t,y){c[a]=c[a]||function(){(c[a].q=c[a].q||[]).push(arguments)};t=l.createElement(r);t.async=1;t.src="https://www.clarity.ms/tag/"+i;y=l.getElementsByTagName(r)[0];y.parentNode.insertBefore(t,y);})(window,document,"clarity","script",${JSON.stringify(clarityId)});`,
          }}
        />
      ) : null}
      {gaId ? (
        <>
          <Script
            data-testid="ga-loader"
            src={`https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(gaId)}`}
            strategy="afterInteractive"
          />
          <Script
            id="google-analytics"
            data-testid="ga-bootstrap"
            strategy="afterInteractive"
            dangerouslySetInnerHTML={{
              __html: `window.dataLayer=window.dataLayer||[];window.gtag=window.gtag||function(){window.dataLayer.push(arguments)};window.gtag("js",new Date());window.gtag("config",${JSON.stringify(gaId)},${JSON.stringify({
                allow_ad_personalization_signals: false,
                allow_google_signals: false,
                send_page_view: false,
              })});`,
            }}
          />
          <GooglePageViews measurementId={gaId} />
        </>
      ) : null}
    </>
  );
}
