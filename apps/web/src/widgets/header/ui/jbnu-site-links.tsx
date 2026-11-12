"use client";

import { Button } from "@/shared/ui/button";
import { ExternalLink, ChevronDown } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import { mobileMenuHeadingClass, mobileMenuRowClass, mobileMenuSectionClass } from "./mobile-menu-styles";

const JBNU_SITES = [
  { name: "수강신청시스템", url: "https://sugang.jbnu.ac.kr/index.do" },
  { name: "개설강좌조회", url: "https://jump.jbnu.ac.kr/link?div=sucrPlan" },
  { name: "전북대학교", url: "https://www.jbnu.ac.kr/web/index.do" },
  { name: "전북대 JUMP", url: "https://jump.jbnu.ac.kr/index.do" },
];

interface JBNUSiteLinksProps {
  isMobile: boolean;
  onLinkClick?: () => void;
}

export function JBNUSiteLinks({ isMobile, onLinkClick }: JBNUSiteLinksProps) {
  const dropdownTriggerId = "jbnu-site-links-trigger";
  const dropdownContentId = "jbnu-site-links-content";

  if (isMobile) {
    return (
      <div className={mobileMenuSectionClass}>
        <h3 className={mobileMenuHeadingClass}>바로가기</h3>
        <div className="flex flex-col">
          {JBNU_SITES.map((site) => (
            <Button asChild key={site.url} variant="ghost" size="sm" className={mobileMenuRowClass}>
              <a href={site.url} target="_blank" rel="noopener noreferrer" onClick={onLinkClick}>
                <ExternalLink aria-hidden="true" className="text-muted-foreground" />
                {site.name}
              </a>
            </Button>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center">
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button id={dropdownTriggerId} aria-controls={dropdownContentId} variant="ghost" size="sm" className="gap-1.5 rounded-xl px-3 h-9 text-gray-600 font-medium hover:bg-primary/5 group transition-colors xl:has-[>svg]:px-2 2xl:has-[>svg]:px-3">
            <ExternalLink className="w-[1.1rem] h-[1.1rem]" />
            <span className="text-sm">바로가기</span>
            <ChevronDown className="w-3.5 h-3.5 opacity-40" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent id={dropdownContentId} aria-labelledby={dropdownTriggerId} align="end" className="w-48 rounded-2xl border-gray-100 bg-white p-1.5 shadow-xl">
          {JBNU_SITES.map((site) => (
            <DropdownMenuItem asChild key={site.url} className="flex items-center justify-between gap-2 rounded-xl px-3 py-2.5 text-sm text-gray-600 hover:bg-gray-50 hover:text-primary cursor-pointer transition-colors group">
              <a href={site.url} target="_blank" rel="noopener noreferrer">
                <span className="font-medium">{site.name}</span>
                <ExternalLink className="w-3.5 h-3.5 opacity-0 group-hover:opacity-100 transition-opacity" />
              </a>
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
