const fs = require('fs');
const path = require('path');
const { projectDir, readInput, relativePath, decide } = require('./lib');

const ENV_FILE = /(^|\/)\.env$/;
const MIGRATION = /^backend\/src\/main\/resources\/db\/migration\/.+\.sql$/;

readInput().then((input) => {
  const tool = input.tool_name;
  const relative = relativePath(input.tool_input?.file_path);
  if (!relative) return;

  if (ENV_FILE.test(relative)) {
    if (tool === 'Read') {
      decide('ask', `${relative} contains secrets (SMTP, JWT, database). Confirm before reading it.`);
    } else {
      decide('deny', `${relative} contains secrets and must be edited by hand, never by Claude.`);
    }
    return;
  }

  if (tool !== 'Read' && MIGRATION.test(relative) && fs.existsSync(path.join(projectDir, relative))) {
    decide(
      'ask',
      `${relative} is an existing Flyway migration. Editing an applied migration breaks databases that ` +
        'already ran it; add a new V{n}__description.sql instead unless you really mean to change it.',
    );
  }
});
