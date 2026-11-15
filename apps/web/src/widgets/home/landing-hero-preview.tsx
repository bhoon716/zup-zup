import { Search } from "lucide-react";
import Image from "next/image";

import { BRAND_ICON_32_PATH } from "@/shared/seo/branding";

import { HeroSearchPreview, SystemNotificationPreview } from "./landing-walkthrough-previews";

export function LandingHeroPreview() {
  return (
    <div
      aria-hidden="true"
      inert
      className="grid items-center gap-3 rounded-2xl bg-primary/10 p-3 sm:gap-4 sm:p-5 lg:grid-cols-[minmax(0,1fr)_minmax(0,20rem)] lg:gap-5 lg:p-6"
    >
      <div className="min-w-0 overflow-hidden rounded-xl border bg-card shadow-lg shadow-primary/10">
        <div className="flex items-center gap-4 border-b px-3 py-2.5 text-sm sm:px-4">
          <span className="flex items-center gap-1.5 font-bold text-primary">
            <Image src={BRAND_ICON_32_PATH} alt="" width={22} height={22} />
            줍줍
          </span>
          <span className="flex items-center gap-1.5 font-medium text-muted-foreground">
            <Search className="size-3.5" /> 강의 검색
          </span>
        </div>
        <div className="bg-background p-3 sm:p-4">
          <HeroSearchPreview />
        </div>
      </div>
      <div className="grid justify-items-end sm:pr-1 lg:pr-0">
        <SystemNotificationPreview />
      </div>
    </div>
  );
}
