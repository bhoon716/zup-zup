"use client";

import { SettingsPage } from "./settings-page";
import { useSettingsPage } from "./useSettingsPage";

export default function SettingsRoutePage() {
  return <SettingsPage model={useSettingsPage()} />;
}
