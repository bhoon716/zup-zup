"use client";

import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search } from "lucide-react";
import { useState, useEffect } from "react";
import type { CourseSearchCondition } from "@/types/api";

interface CourseSearchBarProps {
  onSearch: (condition: CourseSearchCondition) => void;
}

export function CourseSearchBar({ onSearch }: CourseSearchBarProps) {
  const [keyword, setKeyword] = useState("");
  const [debouncedKeyword, setDebouncedKeyword] = useState("");

  // Debounce logic
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedKeyword(keyword);
    }, 500);

    return () => clearTimeout(timer);
  }, [keyword]);

  // Trigger search when debounced keyword changes
  useEffect(() => {
    if (debouncedKeyword.trim()) {
      onSearch({ name: debouncedKeyword });
    }
  }, [debouncedKeyword, onSearch]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (keyword.trim()) {
      onSearch({ name: keyword });
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex gap-2">
      <div className="flex-1 relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
        <Input
          type="text"
          placeholder="강좌명, 교수명, 과목코드로 검색..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          className="pl-10"
        />
      </div>
      <Button type="submit" disabled={!keyword.trim()}>
        검색
      </Button>
    </form>
  );
}
