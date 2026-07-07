import { createWriteStream } from "node:fs";
import { mkdir } from "node:fs/promises";
import { dirname } from "node:path";
import { spawn } from "node:child_process";

const logFile = process.env.WEB_LOG_FILE ?? "./build/logs/web/web.log";

await mkdir(dirname(logFile), { recursive: true });

const logStream = createWriteStream(logFile, { flags: "a" });
const nextArgs = ["start", ...process.argv.slice(2)];
const nextBinary = process.platform === "win32" ? "next.cmd" : "next";
const child = spawn(nextBinary, nextArgs, {
  env: process.env,
  stdio: ["inherit", "pipe", "pipe"],
});

const forward = (stream, chunk) => {
  stream.write(chunk);
  logStream.write(chunk);
};

child.stdout.on("data", (chunk) => {
  forward(process.stdout, chunk);
});

child.stderr.on("data", (chunk) => {
  forward(process.stderr, chunk);
});

const shutdown = (signal) => {
  if (!child.killed) {
    child.kill(signal);
  }
};

process.on("SIGINT", () => shutdown("SIGINT"));
process.on("SIGTERM", () => shutdown("SIGTERM"));

child.on("close", (code, signal) => {
  logStream.end();
  if (signal) {
    process.exitCode = 1;
    return;
  }
  process.exitCode = code ?? 1;
});
