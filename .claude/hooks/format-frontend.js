const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');
const { projectDir, readInput, relativePath } = require('./lib');

const FORMATTED = /^frontend\/src\/.+\.(ts|html|css|json)$/;

readInput().then((input) => {
  const filePath = input.tool_input?.file_path ?? input.tool_response?.filePath;
  const relative = relativePath(filePath);
  if (!FORMATTED.test(relative)) return;

  const frontendDir = path.join(projectDir, 'frontend');
  const prettier = path.join(frontendDir, 'node_modules', 'prettier', 'bin', 'prettier.cjs');
  if (!fs.existsSync(prettier)) return;

  const result = spawnSync(process.execPath, [prettier, '--write', path.join(projectDir, relative)], {
    cwd: frontendDir,
    encoding: 'utf8',
  });
  if (result.status !== 0) {
    process.stderr.write(`Prettier could not format ${relative}:\n${result.stderr}`);
  }
});
