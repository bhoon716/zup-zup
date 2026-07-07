import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";
import boundaries from "eslint-plugin-boundaries";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  globalIgnores([
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
  {
    plugins: {
      boundaries,
    },
    settings: {
      "boundaries/elements": [
        {
          type: "app",
          pattern: "src/app/**",
        },
        {
          type: "widgets",
          pattern: "src/widgets/**",
        },
        {
          type: "features",
          pattern: "src/features/**",
        },
        {
          type: "entities",
          pattern: "src/entities/**",
        },
        {
          type: "shared",
          pattern: "src/shared/**",
        },
      ],
    },
    rules: {
      "boundaries/dependencies": [
        "error",
        {
          default: "disallow",
          message: "${importKind} from '${dependencyType}' is not allowed in '${fileType}'",
          rules: [
            {
              from: { type: "app" },
              allow: [
                { to: { type: "widgets" } },
                { to: { type: "features" } },
                { to: { type: "entities" } },
                { to: { type: "shared" } },
              ],
            },
            {
              from: { type: "widgets" },
              allow: [
                { to: { type: "features" } },
                { to: { type: "entities" } },
                { to: { type: "shared" } },
              ],
            },
            {
              from: { type: "features" },
              allow: [
                { to: { type: "entities" } },
                { to: { type: "shared" } },
              ],
            },
            {
              from: { type: "entities" },
              allow: [
                { to: { type: "shared" } },
              ],
            },
            {
              from: { type: "shared" },
              allow: [
                { to: { type: "shared" } },
              ],
            },
          ],
        },
      ],
    },
  },
]);

export default eslintConfig;

