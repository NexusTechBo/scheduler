const path = require('path');

function toNative(p) {
  return process.platform === 'win32' ? p.replace(/^\/([a-zA-Z])\//, '$1:/') : p;
}

const projectDir = path.resolve(toNative(process.env.CLAUDE_PROJECT_DIR || path.join(__dirname, '..', '..')));

function readInput() {
  return new Promise((resolve) => {
    let data = '';
    process.stdin.setEncoding('utf8');
    process.stdin.on('data', (chunk) => (data += chunk));
    process.stdin.on('end', () => {
      try {
        resolve(JSON.parse(data || '{}'));
      } catch {
        resolve({});
      }
    });
  });
}

function relativePath(filePath) {
  if (!filePath) return '';
  const absolute = path.resolve(projectDir, toNative(filePath));
  return path.relative(projectDir, absolute).split(path.sep).join('/');
}

function decide(decision, reason) {
  process.stdout.write(
    JSON.stringify({
      hookSpecificOutput: {
        hookEventName: 'PreToolUse',
        permissionDecision: decision,
        permissionDecisionReason: reason,
      },
    }),
  );
}

module.exports = { projectDir, readInput, relativePath, decide };
