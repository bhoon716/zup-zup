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
  { name: "수강신청시스템", url: "https://oasis.jbnu.ac.kr/jbnu/sugang/" },
  { name: "개설강좌조회", url: "https://oasis.jbnu.ac.kr/jbnu/sugang/sbjt/sbjt.html" },
  { name: "전북대학교", url: "https://www.jbnu.ac.kr/web/index.do" },
  { name: "오아시스 3.0", url: "https://oasis.jbnu.ac.kr/com/login.do" },
];

interface JBNUSiteLinksProps {
  isMobile: boolean;
  onLinkClick?: () => void;
}

/**
 * 전북대학교 주요 사이트 바로가기 링크들을 렌더링합니다.
 */
export function JBNUSiteLinks({ isMobile, onLinkClick }: JBNUSiteLinksProps) {
  if (isMobile) {
    return (
      <div className="mt-4 space-y-1 pt-4 border-t border-gray-100">
        <p className="px-4 py-2 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
          바로가기
        </p>
        <div className="flex flex-col gap-0.5 pl-3">
          {JBNU_SITES.map((site) => (
            <a key={site.url} href={site.url} target="_blank" rel="noopener noreferrer" onClick={onLinkClick}>
              <Button variant="ghost" size="sm" className="w-full justify-start gap-3 h-11 px-4 text-sm font-medium hover:bg-primary/5 text-gray-600">
                <ExternalLink className="w-4 h-4 opacity-50" />
                {site.name}
              </Button>
            </a>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center">
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="sm" className="gap-1.5 rounded-xl px-3 h-9 text-gray-600 font-medium hover:bg-primary/5 group transition-colors">
            <ExternalLink className="w-[1.1rem] h-[1.1rem]" />
            <span className="text-sm">바로가기</span>
            <ChevronDown className="w-3.5 h-3.5 opacity-40" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-48 rounded-2xl border-gray-100 bg-white p-1.5 shadow-xl">
          {JBNU_SITES.map((site) => (
            <a key={site.url} href={site.url} target="_blank" rel="noopener noreferrer">
              <DropdownMenuItem className="flex items-center justify-between gap-2 rounded-xl px-3 py-2.5 text-sm text-gray-600 hover:bg-gray-50 hover:text-primary cursor-pointer transition-colors group">
                <span className="font-medium">{site.name}</span>
                <ExternalLink className="w-3.5 h-3.5 opacity-0 group-hover:opacity-100 transition-opacity" />
              </DropdownMenuItem>
            </a>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
