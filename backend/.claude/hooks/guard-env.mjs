// PreToolUse (Edit|Write): never write secrets files; .env.example is the only editable one.
import { readFileSync } from "node:fs";
import { basename } from "node:path";

const input = JSON.parse(readFileSync(0, "utf8"));
const name = basename((input.tool_input?.file_path ?? "").replaceAll("\\", "/"));

if (/^\.env(\..+)?$/.test(name) && name !== ".env.example") {
  console.error(`Blocked: ${name} holds secrets and must be edited by a human. Add new variables to .env.example instead.`);
  process.exit(2);
}
