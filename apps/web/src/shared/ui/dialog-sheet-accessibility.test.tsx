import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import {
  Dialog,
  DialogContent,
  DialogTitle,
} from "./dialog";
import {
  Sheet,
  SheetContent,
  SheetTitle,
} from "./sheet";

describe("Dialog and Sheet accessibility labels", () => {
  it("uses the Korean label for the Dialog close button", () => {
    render(
      <Dialog open>
        <DialogContent>
          <DialogTitle>대화상자</DialogTitle>
        </DialogContent>
      </Dialog>
    );

    expect(screen.getByRole("button", { name: "닫기" })).toBeInTheDocument();
  });

  it("uses the Korean label for the Sheet close button", () => {
    render(
      <Sheet open>
        <SheetContent>
          <SheetTitle>시트</SheetTitle>
        </SheetContent>
      </Sheet>
    );

    expect(screen.getByRole("button", { name: "닫기" })).toBeInTheDocument();
  });
});
