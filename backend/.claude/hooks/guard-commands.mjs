// PreToolUse (Bash|PowerShell): block destructive or irreversible commands.
import { readFileSync } from "node:fs";

const input = JSON.parse(readFileSync(0, "utf8"));
const cmd = input.tool_input?.command ?? "";

const rules = [
  [/\bgit\s+push\b/, "git push: pushing must be done by a human"],
  [/--no-verify\b/, "--no-verify: git hooks must not be skipped"],
  [/\bgit\s+reset\s+--hard\b/, "git reset --hard: discards uncommitted work"],
  [/\bflyway\s*clean\b|\bflywayClean\b/i, "flyway clean: drops every object in the schemas"],
  [/\bdrop\s+(schema|database)\b/i, "DROP SCHEMA/DATABASE: deletes tenant data"],
];

const hit = rules.find(([re]) => re.test(cmd));
if (hit) {
  console.error(`Blocked: ${hit[1]}. Ask the user to run it themselves if it is really needed.`);
  process.exit(2);
}
