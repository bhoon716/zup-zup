"use client";

import { Button } from "@/shared/ui/button";
import { ExternalLink, ChevronDown } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";

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

/**
 * 전북대학교 주요 사이트 바로가기 링크들을 렌더링합니다.
 */
export function JBNUSiteLinks({ isMobile, onLinkClick }: JBNUSiteLinksProps) {
  const dropdownTriggerId = "jbnu-site-links-trigger";
  const dropdownContentId = "jbnu-site-links-content";

  if (isMobile) {
    return (
      <div className="mt-4 space-y-1 pt-4 border-t border-gray-100">
        <p className="px-4 py-2 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
          바로가기
        </p>
        <div className="flex flex-col gap-0.5 pl-3">
          {JBNU_SITES.map((site) => (
            <Button asChild key={site.url} variant="ghost" size="sm" className="w-full justify-start gap-3 h-11 px-4 text-sm font-medium hover:bg-primary/5 text-gray-600">
              <a href={site.url} target="_blank" rel="noopener noreferrer" onClick={onLinkClick}>
                <ExternalLink className="w-4 h-4 opacity-50" />
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
          <Button id={dropdownTriggerId} aria-controls={dropdownContentId} variant="ghost" size="sm" className="gap-1.5 rounded-xl px-3 h-9 text-gray-600 font-medium hover:bg-primary/5 group transition-colors">
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
