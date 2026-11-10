"use client";

import { Suspense } from "react";
import { SettingsPage } from "./settings-page";
import { useSettingsPage } from "./useSettingsPage";

function SettingsContent() {
  return <SettingsPage model={useSettingsPage()} />;
}

export default function SettingsRoutePage() {
  return (
    <Suspense fallback={null}>
      <SettingsContent />
    </Suspense>
  );
}
