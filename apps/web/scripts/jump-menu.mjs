const DEFAULT_BASE_URL = "https://jump.jbnu.ac.kr";
const MENU_INDEX_ENDPOINT = "/Main/onLoad.do";
const MENU_INFO_ENDPOINT = "/Main/getMenuInfo.do";

const MENU_FIELD_ALIASES = {
  menuId: ["MENU_ID", "menuId"],
  menuKey: ["MENU_KEY", "menuKey"],
  menuName: ["MENU_NM", "MENU_NAME", "menuName"],
  pageId: ["PGM_ID", "pageId"],
  taskAuthrtId: ["TASK_AUTHRT_ID", "taskAuthrtId"],
  appContext: ["APP_CONTEXT", "appContext"],
  callPage: ["CALL_PAGE", "callPage"],
};

function isPlainObject(value) {
  return Object.prototype.toString.call(value) === "[object Object]";
}

function firstDefined(value, keys) {
  for (const key of keys) {
    if (value?.[key] !== undefined && value?.[key] !== null && value?.[key] !== "") {
      return value[key];
    }
  }
  return undefined;
}

function normalizeMenuRow(raw) {
  if (!isPlainObject(raw)) {
    return null;
  }

  const menuId = firstDefined(raw, MENU_FIELD_ALIASES.menuId);
  const menuKey = firstDefined(raw, MENU_FIELD_ALIASES.menuKey);
  const menuName = firstDefined(raw, MENU_FIELD_ALIASES.menuName);
  const pageId = firstDefined(raw, MENU_FIELD_ALIASES.pageId);
  const taskAuthrtId = firstDefined(raw, MENU_FIELD_ALIASES.taskAuthrtId);
  const appContext = firstDefined(raw, MENU_FIELD_ALIASES.appContext);
  const callPage = firstDefined(raw, MENU_FIELD_ALIASES.callPage);

  if (!menuId && !menuKey && !menuName && !pageId) {
    return null;
  }

  const row = {
    menuId: menuId ?? "",
    menuKey: menuKey ?? "",
    menuName: menuName ?? "",
    pageId: pageId ?? "",
  };

  if (taskAuthrtId) {
    row.taskAuthrtId = taskAuthrtId;
  }
  if (appContext) {
    row.appContext = appContext;
  }
  if (callPage) {
    row.callPage = callPage;
  }

  return row;
}

function getDedupKey(row) {
  return [row.menuId, row.menuKey, row.menuName, row.pageId, row.callPage].join("|");
}

function walkMenus(value, rows, seen, visited) {
  if (value === null || value === undefined) {
    return;
  }

  if (Array.isArray(value)) {
    for (const item of value) {
      walkMenus(item, rows, seen, visited);
    }
    return;
  }

  if (!isPlainObject(value)) {
    return;
  }

  if (visited.has(value)) {
    return;
  }
  visited.add(value);

  const normalized = normalizeMenuRow(value);
  if (normalized) {
    const key = getDedupKey(normalized);
    if (!seen.has(key)) {
      seen.add(key);
      rows.push(normalized);
    }
  }

  for (const child of Object.values(value)) {
    walkMenus(child, rows, seen, visited);
  }
}

export function extractMenuRows(payload) {
  const rows = [];
  const seen = new Set();
  const visited = new WeakSet();
  walkMenus(payload, rows, seen, visited);
  return rows;
}

export function formatMenuRows(rows) {
  return rows
    .map((row) => [row.menuId, row.menuKey, row.menuName, row.pageId || row.callPage]
      .filter(Boolean)
      .join(" | "))
    .join("\n");
}

export function buildMenuInfoRequest(row) {
  const body = new URLSearchParams();
  if (row.menuKey) {
    body.set("_AUTH_MENU_KEY", row.menuKey);
  }
  if (row.taskAuthrtId) {
    body.set("_AUTH_TASK_AUTHRT_ID", row.taskAuthrtId);
  }
  if (row.appContext) {
    body.set("_APP_CONTEXT", row.appContext);
  }
  return body;
}

function parseArgs(argv) {
  const args = {
    baseUrl: DEFAULT_BASE_URL,
    cookie: process.env.JUMP_COOKIE ?? process.env.COOKIE ?? "",
    menuId: "",
    menuKey: "",
    details: false,
    json: false,
    help: false,
  };

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === "--help" || arg === "-h") {
      args.help = true;
      continue;
    }
    if (arg === "--details") {
      args.details = true;
      continue;
    }
    if (arg === "--json") {
      args.json = true;
      continue;
    }
    if (arg.startsWith("--base-url=")) {
      args.baseUrl = arg.slice("--base-url=".length);
      continue;
    }
    if (arg === "--base-url") {
      args.baseUrl = argv[++i] ?? args.baseUrl;
      continue;
    }
    if (arg.startsWith("--cookie=")) {
      args.cookie = arg.slice("--cookie=".length);
      continue;
    }
    if (arg === "--cookie") {
      args.cookie = argv[++i] ?? args.cookie;
      continue;
    }
    if (arg.startsWith("--menu-id=")) {
      args.menuId = arg.slice("--menu-id=".length);
      continue;
    }
    if (arg === "--menu-id") {
      args.menuId = argv[++i] ?? args.menuId;
      continue;
    }
    if (arg.startsWith("--menu-key=")) {
      args.menuKey = arg.slice("--menu-key=".length);
      continue;
    }
    if (arg === "--menu-key") {
      args.menuKey = argv[++i] ?? args.menuKey;
      continue;
    }
  }

  return args;
}

function createHeaders(cookie) {
  const headers = {
    "content-type": "application/x-www-form-urlencoded; charset=UTF-8",
    accept: "application/json, text/plain, */*",
    "x-requested-with": "XMLHttpRequest",
  };

  if (cookie) {
    headers.cookie = cookie;
  }

  return headers;
}

async function postJson(url, body, cookie) {
  const response = await fetch(url, {
    method: "POST",
    headers: createHeaders(cookie),
    body,
  });

  const text = await response.text();
  let payload;
  try {
    payload = JSON.parse(text);
  } catch {
    payload = { raw: text };
  }

  if (payload?.ERRMSGINFO?.ERRMSG) {
    throw new Error(`${url.pathname}: ${payload.ERRMSGINFO.ERRMSG}`);
  }

  if (!response.ok) {
    throw new Error(`${url.pathname}: HTTP ${response.status}`);
  }

  return payload;
}

async function loadMenuIndex(baseUrl, cookie) {
  const url = new URL(MENU_INDEX_ENDPOINT, baseUrl);
  const payload = await postJson(url, "", cookie);
  return extractMenuRows(payload);
}

async function loadMenuInfo(baseUrl, cookie, row) {
  const url = new URL(MENU_INFO_ENDPOINT, baseUrl);
  const payload = await postJson(url, buildMenuInfoRequest(row), cookie);
  return extractMenuRows(payload);
}

function printUsage() {
  const lines = [
    "Usage:",
    "  node scripts/jump-menu.mjs [options]",
    "",
    "Options:",
    "  --cookie <value>      Raw Cookie header value",
    "  --base-url <url>      Jump base URL (default: https://jump.jbnu.ac.kr)",
    "  --menu-id <id>        Filter by MENU_ID",
    "  --menu-key <key>      Filter by MENU_KEY",
    "  --details             Fetch /Main/getMenuInfo.do for each matched row",
    "  --json                Print JSON instead of a text report",
    "  -h, --help            Show this message",
    "",
    "Environment:",
    "  JUMP_COOKIE           Raw Cookie header value",
    "  COOKIE                Fallback cookie header value",
  ];

  process.stdout.write(`${lines.join("\n")}\n`);
}

function toOutputRows(rows) {
  return rows.map((row) => ({
    menuId: row.menuId,
    menuKey: row.menuKey,
    menuName: row.menuName,
    pageId: row.pageId,
    taskAuthrtId: row.taskAuthrtId,
    appContext: row.appContext,
    callPage: row.callPage,
  }));
}

async function runCli() {
  const args = parseArgs(process.argv.slice(2));
  if (args.help) {
    printUsage();
    return;
  }

  const baseUrl = new URL(args.baseUrl).toString();
  const indexRows = await loadMenuIndex(baseUrl, args.cookie);
  let rows = indexRows;

  if (args.menuId) {
    rows = rows.filter((row) => row.menuId === args.menuId);
  }
  if (args.menuKey) {
    rows = rows.filter((row) => row.menuKey === args.menuKey);
  }

  if (args.details) {
    const detailedRows = [];
    for (const row of rows) {
      const detailRows = await loadMenuInfo(baseUrl, args.cookie, row);
      if (detailRows.length > 0) {
        detailedRows.push(...detailRows);
      } else {
        detailedRows.push(row);
      }
    }
    rows = detailedRows;
  }

  const outputRows = toOutputRows(rows);
  if (args.json) {
    process.stdout.write(`${JSON.stringify(outputRows, null, 2)}\n`);
    return;
  }

  process.stdout.write(`${formatMenuRows(outputRows)}\n`);
}

if (import.meta.url === `file://${process.argv[1]}`) {
  runCli().catch((error) => {
    process.stderr.write(`${error instanceof Error ? error.message : String(error)}\n`);
    process.exitCode = 1;
  });
}
