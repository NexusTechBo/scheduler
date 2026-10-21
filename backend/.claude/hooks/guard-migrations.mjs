// PreToolUse (Edit|Write): applied Flyway migrations are immutable; only new V<n>__*.sql files may be written.
import { spawnSync } from "node:child_process";
import { readFileSync } from "node:fs";

const input = JSON.parse(readFileSync(0, "utf8"));
const file = (input.tool_input?.file_path ?? "").replaceAll("\\", "/");
if (!/\/db\/migration\/.+\.sql$/.test(file)) process.exit(0);

const cwd = process.env.CLAUDE_PROJECT_DIR ?? process.cwd();
const r = spawnSync("git", ["log", "-1", "--format=%H", "--", file], { cwd, encoding: "utf8" });

if (r.stdout?.trim()) {
  console.error(`Blocked: ${file} is a committed Flyway migration. Editing it breaks the checksum in every tenant schema. Create a new V<n>__description.sql instead.`);
  process.exit(2);
}
