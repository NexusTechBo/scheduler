// PostToolUse (Edit|Write): compile after a .java edit; exit 2 feeds compiler errors back to Claude.
import { spawnSync } from "node:child_process";
import { readFileSync } from "node:fs";
import { join } from "node:path";

const input = JSON.parse(readFileSync(0, "utf8"));
const file = input.tool_input?.file_path ?? "";
if (!file.endsWith(".java")) process.exit(0);

const cwd = process.env.CLAUDE_PROJECT_DIR ?? process.cwd();
const gradlew = join(cwd, process.platform === "win32" ? "gradlew.bat" : "gradlew");
const r = spawnSync(`"${gradlew}" compileJava -q --console=plain`, { cwd, encoding: "utf8", shell: true });

if (r.status !== 0) {
  const out = `${r.stdout ?? ""}${r.stderr ?? ""}`.trim().split("\n").slice(-40).join("\n");
  console.error(`compileJava failed after editing ${file}:\n${out}`);
  process.exit(2);
}
