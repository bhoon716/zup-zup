// @vitest-environment node
import { mkdtempSync, rmSync, writeFileSync, mkdirSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { afterAll, describe, expect, it } from "vitest";
import { ESLint } from "eslint";
import { clearCaches, parseAndGenerateServices } from "@typescript-eslint/typescript-estree";

describe("보안 수정된 lint 의존성 호환", () => {
  const fixture = mkdtempSync(path.join(tmpdir(), "zupzup-parser-"));
  mkdirSync(path.join(fixture, "src"));
  writeFileSync(path.join(fixture, "tsconfig.json"), JSON.stringify({
    compilerOptions: { strict: true, allowJs: true }, include: ["src/**/*.ts"],
  }));
  writeFileSync(path.join(fixture, "src/included.ts"), "export const included = 1;");
  writeFileSync(path.join(fixture, ".scratch.js"), "const value = 1;");
  writeFileSync(path.join(fixture, "excluded.ts"), "const excluded = 1;");

  afterAll(() => {
    clearCaches();
    rmSync(fixture, { recursive: true, force: true });
  });

  it("parser의 default-project glob은 숨김 JS를 허용하고 목록 밖 TS는 거절한다", () => {
    const options = {
      tsconfigRootDir: fixture,
      projectService: { allowDefaultProject: ["*.js"] },
    };
    const parsed = parseAndGenerateServices("const value = 1;", {
      ...options, filePath: path.join(fixture, ".scratch.js"),
    });
    expect(parsed.services.program).toBeDefined();
    expect(parsed.ast.body).toHaveLength(1);
    expect(() => parseAndGenerateServices("const excluded = 1;", {
      ...options, filePath: path.join(fixture, "excluded.ts"),
    })).toThrow(/not found|not included|allowDefaultProject/i);
  });

  it("공유 계층의 상위 계층 참조는 계속 차단하고 공유 계층 참조는 허용한다", async () => {
    const eslint = new ESLint();
    const filePath = path.resolve("src/shared/lib/pwa-navigation.ts");
    const [blocked] = await eslint.lintText(
      'import { LoginModal } from "@/widgets/auth/login-modal"; export const value = LoginModal;',
      { filePath },
    );
    expect(blocked.messages.some(({ ruleId }) => ruleId === "boundaries/dependencies")).toBe(true);
    const [allowed] = await eslint.lintText(
      'import { cn } from "@/shared/lib/utils"; export const value = cn;',
      { filePath },
    );
    expect(allowed.messages.some(({ ruleId }) => ruleId === "boundaries/dependencies")).toBe(false);
  });
});
