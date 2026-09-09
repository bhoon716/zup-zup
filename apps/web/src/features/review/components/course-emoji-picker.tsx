"use client";

import emojiData from "@emoji-mart/data";
import koI18n from "@emoji-mart/data/i18n/ko.json";
import Picker from "@emoji-mart/react";

const emojiPickerI18n = {
  ...koI18n,
  search: "모든 이모티콘 검색",
  categories: {
    ...koI18n.categories,
    frequent: "자주 사용됨",
  },
};

interface CourseEmojiPickerProps {
  onEmojiSelect: (emoji: { native?: string }) => void;
}

export function CourseEmojiPicker({ onEmojiSelect }: CourseEmojiPickerProps) {
  return (
    <Picker
      data={emojiData}
      i18n={emojiPickerI18n}
      theme="dark"
      onEmojiSelect={onEmojiSelect}
      searchPosition="top"
      navPosition="top"
      previewPosition="none"
      maxFrequentRows={2}
      perLine={8}
      emojiSize={24}
      style={{ width: "100%" }}
    />
  );
}
