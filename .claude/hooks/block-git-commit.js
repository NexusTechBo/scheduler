const { readInput, decide } = require('./lib');

const OPTIONS_WITH_VALUE = new Set(['-C', '-c', '--git-dir', '--work-tree', '--namespace']);
const BLOCKED = new Set(['commit', 'push']);

const isGit = (token) => /^(git|git\.exe)$/i.test(token.replace(/["']/g, '').split(/[\\/]/).pop());

function gitSubcommand(segment) {
  const tokens = segment.trim().split(/\s+/);
  let i = 0;
  while (i < tokens.length && !isGit(tokens[i]) && /^\w+=|\\$|^["'][^"']*$/.test(tokens[i])) i++;
  if (i >= tokens.length || !isGit(tokens[i])) return null;
  for (i++; i < tokens.length; i++) {
    const token = tokens[i];
    if (OPTIONS_WITH_VALUE.has(token)) i++;
    else if (!token.startsWith('-')) return token;
  }
  return null;
}

readInput().then((input) => {
  const command = input.tool_input?.command ?? '';
  const blocked = command
    .split(/&&|\|\||[;|&\n]/)
    .map(gitSubcommand)
    .find((sub) => sub && BLOCKED.has(sub.toLowerCase()));
  if (blocked) {
    decide('deny', `git ${blocked} is not allowed: the user reviews the code and commits or pushes by hand.`);
  }
});
