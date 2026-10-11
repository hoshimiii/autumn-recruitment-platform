import assert from 'node:assert/strict';
import { spawn, spawnSync } from 'node:child_process';
import { randomBytes, randomUUID } from 'node:crypto';
import { readFileSync, existsSync } from 'node:fs';
import { createServer } from 'node:net';
import { resolve } from 'node:path';

// Only the bootstrap Admin API uses password grant; every tested user uses authorization code.
const provider = 'http://127.0.0.1:18180', backendUrl = 'http://127.0.0.1:18189';
const issuer = `${provider}/realms/recruitops`, callback = `${backendUrl}/login/oauth2/code/recruitops`;
const project = `recruitops-oidc-smoke-${randomBytes(6).toString('hex')}`, composeFile = resolve('infra/compose.yml');
const jarFile = resolve('apps/server/target/recruitment-server-0.1.0-SNAPSHOT.jar');
const password = () => randomBytes(24).toString('base64url');
const composeEnv = { ...process.env, RECRUITOPS_KEYCLOAK_ADMIN_PASSWORD: password() };
const javaEnv = Object.fromEntries(Object.entries(process.env).filter(([key]) =>
  !/^(SPRING_|RECRUITOPS_|SERVER_|LOGGING_|JAVA_TOOL_OPTIONS$|JDK_JAVA_OPTIONS$|_JAVA_OPTIONS$)/i.test(key)));
const roles = ['VIEWER', 'COLLECTOR', 'REVIEWER', 'ADMIN'];
let backend, launcherIdentity, jvmIdentity, actualJvmPid, started = false, ownedProject = false;
let stage = 'preflight', startupBuffer = '', processError = false, cleanupFailed = false;
let lastHttpStatus = 0;
const sleep = ms => new Promise(done => setTimeout(done, ms));
const normalize = value => value.replaceAll('\\', '/').toLowerCase();
function command(executable, args, env = process.env, timeout = 60_000) {
  const result = spawnSync(executable, args, { encoding: 'utf8', windowsHide: true, env, timeout, maxBuffer: 4 * 1024 * 1024 });
  assert.equal(result.status, 0, `${stage}: child command failed (output withheld)`);
  return result.stdout.trim();
}
const docker = (...args) => command('docker', args);
const compose = (...args) => command('docker', ['compose', '-p', project, '-f', composeFile, '--profile', 'sso', ...args], composeEnv, 240_000);
const ids = filter => docker('ps', '-a', '--filter', filter, '--format', '{{.ID}}').split(/\r?\n/).filter(Boolean);
const labels = id => JSON.parse(docker('inspect', '--format', '{{json .Config.Labels}}', id));
function ownedContainers() {
  const containers = ids(`label=com.docker.compose.project=${project}`);
  for (const id of containers) {
    const value = labels(id);
    assert.ok(value['com.docker.compose.project'] === project && value['com.docker.compose.service'] === 'keycloak'
      && String(value['com.docker.compose.project.config_files']).split(',').some(file => normalize(file) === normalize(composeFile)),
    'Refuse cleanup of a container with unexpected ownership');
  }
  return containers;
}
function processIdentity(pid) {
  if (process.platform !== 'win32') return { ProcessId: pid, CreationDate: pid };
  assert.ok(Number.isSafeInteger(pid) && pid > 0, 'Invalid process identity');
  const output = command('powershell.exe', ['-NoProfile', '-NonInteractive', '-Command',
    `Get-CimInstance Win32_Process -Filter 'ProcessId=${pid}' | Select-Object ProcessId,ParentProcessId,Name,CreationDate | ConvertTo-Json -Compress`]);
  return output ? JSON.parse(output) : null;
}
function sameProcess(identity) {
  const current = identity && processIdentity(identity.ProcessId);
  return current && current.CreationDate === identity.CreationDate;
}
async function freePort(port) {
  const probe = createServer();
  await new Promise((done, reject) => { probe.once('error', reject); probe.listen(port, '127.0.0.1', done); });
  await new Promise(done => probe.close(done));
}
async function until(check, timeout, message) {
  const end = Date.now() + timeout;
  while (Date.now() < end) { if (await Promise.resolve().then(check).catch(() => false)) return; await sleep(500); }
  throw new Error(message);
}
async function request(url, init = {}, jar) {
  const target = new URL(url);
  assert.ok([provider, backendUrl].includes(target.origin), 'Refuse a redirect outside the two local origins');
  const headers = new Headers(init.headers);
  if (jar) {
    const cookies = jar.filter(cookie => cookie.host === target.hostname && (target.pathname === cookie.path
      || target.pathname.startsWith(cookie.path.endsWith('/') ? cookie.path : `${cookie.path}/`)))
      .map(cookie => `${cookie.name}=${cookie.value}`).join('; ');
    if (cookies) headers.set('Cookie', cookies);
  }
  const response = await fetch(target, { ...init, headers, redirect: 'manual', signal: AbortSignal.timeout(10_000) });
  lastHttpStatus = response.status;
  for (const value of response.headers.getSetCookie()) {
    if (!jar) continue;
    const parts = value.split(';').map(part => part.trim()), equal = parts[0].indexOf('=');
    const cookie = { host: target.hostname, name: parts[0].slice(0, equal), value: parts[0].slice(equal + 1), path: '/' };
    const path = parts.find(part => /^path=/i.test(part)); if (path) cookie.path = path.slice(5);
    const domain = parts.find(part => /^domain=/i.test(part));
    assert.ok(!domain || domain.slice(7).replace(/^\./, '') === target.hostname, 'Unexpected cookie domain');
    const previous = jar.findIndex(item => item.host === cookie.host && item.name === cookie.name && item.path === cookie.path);
    if (previous >= 0) jar.splice(previous, 1);
    if (!parts.some(part => /^max-age=0$/i.test(part)) && cookie.value) jar.push(cookie);
  }
  return response;
}
async function jsonRequest(url, init = {}, expected = 200, jar) {
  const response = await request(url, init, jar);
  assert.ok((Array.isArray(expected) ? expected : [expected]).includes(response.status), `${stage}: unexpected HTTP status`);
  const text = await response.text();
  return text ? JSON.parse(text) : undefined;
}
const jsonBody = (method, body, headers = {}) => ({ method, headers: { 'Content-Type': 'application/json', ...headers }, body: JSON.stringify(body) });
const decodeHtml = value => value.replaceAll('&amp;', '&').replaceAll('&quot;', '"').replace(/&#(\d+);/g, (_, code) => String.fromCharCode(Number(code)));
async function login(role, credentials) {
  const jar = [];
  let response = await request(`${backendUrl}/oauth2/authorization/recruitops`, {}, jar);
  for (let step = 0; step < 12; step++) {
    if ([301, 302, 303, 307, 308].includes(response.status)) {
      response = await request(new URL(response.headers.get('location'), response.url), {}, jar); continue;
    }
    assert.equal(response.status, 200, `${role}: authorization-code login response`);
    if (response.headers.get('content-type')?.includes('application/json')) {
      const session = await response.json();
      assert.ok(session.authenticated && session.mode === 'oidc', `${role}: authenticated OIDC session missing`);
      return { jar, session };
    }
    const html = await response.text();
    const form = html.match(/<form\b[^>]*id=["']kc-form-login["'][^>]*>/i)?.[0];
    const action = form?.match(/\baction=["']([^"']+)["']/i)?.[1];
    assert.ok(action, `${role}: expected Keycloak credential form`);
    const target = new URL(decodeHtml(action), response.url);
    assert.ok(target.origin === provider && target.pathname.startsWith('/realms/recruitops/login-actions/'), 'Unexpected credential recipient');
    response = await request(target, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ username: credentials.username, password: credentials.password, credentialId: '' }) }, jar);
  }
  throw new Error(`${role}: authorization redirect budget exhausted`);
}
async function bearerFromCode(jar, clientSecret) {
  const state = randomUUID();
  const query = new URLSearchParams({ client_id: 'recruitops-web', redirect_uri: callback, response_type: 'code', scope: 'openid', state });
  const response = await request(`${issuer}/protocol/openid-connect/auth?${query}`, {}, jar);
  assert.equal(response.status, 302, 'Provider SSO must return a real authorization code');
  const redirect = new URL(response.headers.get('location'));
  assert.ok(redirect.origin === backendUrl && redirect.pathname === '/login/oauth2/code/recruitops'
    && redirect.searchParams.get('state') === state && redirect.searchParams.has('code'), 'Unexpected code callback');
  const issued = await jsonRequest(`${issuer}/protocol/openid-connect/token`, { method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ grant_type: 'authorization_code',
      client_id: 'recruitops-web', client_secret: clientSecret, redirect_uri: callback, code: redirect.searchParams.get('code') }) });
  assert.ok(typeof issued.access_token === 'string', 'Authorization-code access token missing');
  return issued.access_token;
}
function cleanupOwned() {
  let failed = false;
  try {
    if (!jvmIdentity && actualJvmPid && backend) {
      const candidate = processIdentity(actualJvmPid);
      if (candidate && (process.platform !== 'win32' || (/^java(?:w)?\.exe$/i.test(candidate.Name)
        && (candidate.ProcessId === backend.pid || candidate.ParentProcessId === backend.pid)))) jvmIdentity = candidate;
    }
    const identities = new Map([jvmIdentity, launcherIdentity].filter(Boolean).map(identity => [identity.ProcessId, identity]));
    for (const identity of identities.values()) {
      if (!sameProcess(identity)) continue;
      try {
        if (process.platform === 'win32') command('taskkill', ['/pid', String(identity.ProcessId), '/T', '/F']);
        else process.kill(identity.ProcessId, 'SIGTERM');
      } catch { if (sameProcess(identity)) failed = true; }
    }
  } catch { failed = true; }
  try {
    if (ownedProject) { ownedContainers(); compose('down'); assert.equal(ownedContainers().length, 0, 'Owned containers survived cleanup'); }
  } catch { failed = true; }
  assert.ok(!failed, 'Owned runtime cleanup failed');
}
for (const [signal, code] of [['SIGINT', 130], ['SIGTERM', 143]]) process.once(signal, () => {
  try { cleanupOwned(); } catch { console.error(JSON.stringify({ status: 'FAIL', stage: 'interrupted owned cleanup' })); }
  process.exit(code);
});

try {
  assert.ok(existsSync(jarFile), 'Package the Java JAR before running this smoke');
  assert.ok(readFileSync(composeFile, 'utf8').includes('quay.io/keycloak/keycloak:26.7.5'), 'Expected pinned Keycloak image');
  assert.ok(ids(`label=com.docker.compose.project=${project}`).length === 0, 'Generated project collision');
  for (const id of ids('label=com.docker.compose.project=recruitops-middleware-demo')) {
    const service = labels(id)['com.docker.compose.service'];
    assert.ok(service !== 'keycloak', 'Existing demo Keycloak ownership collision');
    const running = docker('inspect', '--format', '{{.State.Running}}', id) === 'true';
    assert.ok(!running || !['redis', 'elasticsearch'].includes(service), 'Stop owned cache/search demo before the sequential SSO smoke');
  }
  await freePort(18180); await freePort(18189);
  stage = 'Keycloak startup'; ownedProject = true;
  compose('up', '-d', '--no-deps', 'keycloak');
  const owned = ownedContainers(); assert.equal(owned.length, 1, 'Exactly one owned Keycloak container required');
  const memory = Number(docker('inspect', '--format', '{{.HostConfig.Memory}}', owned[0]));
  assert.ok(memory > 0 && memory <= 768 * 1024 * 1024, 'Keycloak memory boundary exceeded');
  await until(() => request(`${issuer}/.well-known/openid-configuration`).then(response => response.ok), 180_000, 'Keycloak startup timeout');
  stage = 'synthetic identity setup';
  const bootstrap = await jsonRequest(`${provider}/realms/master/protocol/openid-connect/token`, { method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ grant_type: 'password', client_id: 'admin-cli',
      username: 'demo-admin', password: composeEnv.RECRUITOPS_KEYCLOAK_ADMIN_PASSWORD }) });
  const adminHeaders = { Authorization: `Bearer ${bootstrap.access_token}` };
  const adminApi = `${provider}/admin/realms/recruitops`;
  const adminGet = path => jsonRequest(`${adminApi}${path}`, { headers: adminHeaders });
  const clients = await adminGet('/clients?clientId=recruitops-web'); assert.equal(clients.length, 1, 'Imported confidential client required');
  const client = clients[0]; assert.ok(client.standardFlowEnabled && !client.directAccessGrantsEnabled, 'No user direct grants permitted');
  await jsonRequest(`${adminApi}/clients/${client.id}`, jsonBody('PUT', { ...client, redirectUris: [callback] }, adminHeaders), 204);
  const clientSecret = (await adminGet(`/clients/${client.id}/client-secret`)).value;
  assert.ok(typeof clientSecret === 'string' && clientSecret.length > 8, 'Ephemeral client secret missing');
  const users = {};
  for (const role of roles) {
    const realmRole = await adminGet(`/roles/${role}`);
    const username = `smoke-${role.toLowerCase()}`, secret = password();
    await jsonRequest(`${adminApi}/users`, jsonBody('POST', { username, enabled: true, firstName: 'Synthetic', lastName: role,
      email: `${username}@example.invalid`, emailVerified: true, requiredActions: [], credentials: [{ type: 'password', value: secret, temporary: false }] }, adminHeaders), 201);
    const matches = await adminGet(`/users?username=${username}&exact=true`); assert.equal(matches.length, 1, 'Synthetic user missing');
    await jsonRequest(`${adminApi}/users/${matches[0].id}/role-mappings/realm`, jsonBody('POST', [realmRole], adminHeaders), 204);
    users[role] = { username, password: secret };
  }
  stage = 'owned Java startup';
  backend = spawn('java', ['-Xmx384m', '-jar', jarFile], { windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'], env: { ...javaEnv,
    SPRING_PROFILES_ACTIVE: 'oidc', SERVER_PORT: '18189', SERVER_ADDRESS: '127.0.0.1',
    SPRING_DATASOURCE_URL: `jdbc:sqlite:file:oidc-${randomUUID()}?mode=memory&cache=shared`,
    RECRUITOPS_OIDC_ISSUER: issuer, RECRUITOPS_OIDC_AUDIENCE: 'recruitops-api', RECRUITOPS_OIDC_CLIENT_ID: 'recruitops-web',
    RECRUITOPS_OIDC_CLIENT_SECRET: clientSecret, RECRUITOPS_OIDC_ROLE_CLAIM: 'realm_access.roles',
    RECRUITOPS_OIDC_FRONTEND_URL: `${backendUrl}/api/v1/auth/session`, RECRUITOPS_COLLECTOR_SCHEDULER_ENABLED: 'false',
    RECRUITOPS_REDIS_ENABLED: 'false', RECRUITOPS_ELASTICSEARCH_ENABLED: 'false' } });
  backend.on('error', () => { processError = true; });
  backend.stderr.on('data', () => {});
  backend.stdout.on('data', chunk => {
    if (started && actualJvmPid) return;
    startupBuffer = (startupBuffer + chunk.toString()).slice(-16_384);
    const match = startupBuffer.match(/with PID (\d+)/); if (match) actualJvmPid = Number(match[1]);
    if (startupBuffer.includes('Started RecruitOpsApplication')) started = true;
  });
  launcherIdentity = processIdentity(backend.pid);
  await until(() => !processError && started && actualJvmPid && request(`${backendUrl}/api/v1/health`).then(response => response.ok), 90_000, 'Java startup timeout');
  jvmIdentity = processIdentity(actualJvmPid);
  assert.ok(jvmIdentity && (process.platform !== 'win32' || (/^java(?:w)?\.exe$/i.test(jvmIdentity.Name)
    && (jvmIdentity.ProcessId === backend.pid || jvmIdentity.ParentProcessId === backend.pid))), 'Cannot verify owned JVM identity');
  startupBuffer = '';
  stage = 'anonymous gates';
  await jsonRequest(`${backendUrl}/api/v1/jobs`);
  await jsonRequest(`${backendUrl}/api/v1/sources`, {}, 401);
  // Anonymous rejection can become 401 when the embedded server dispatches a CSRF error.
  await jsonRequest(`${backendUrl}/api/v1/manual-items`, jsonBody('POST', {}), [401, 403]);
  const anonymous = await jsonRequest(`${backendUrl}/api/v1/auth/session`); assert.ok(!anonymous.authenticated && !anonymous.capabilities.length, 'Anonymous capability escalation');
  for (const role of roles) {
    stage = `${role} session and RBAC`;
    const { jar, session } = await login(role, users[role]);
    assert.deepEqual(session.roles, [role], 'Only issuer-assigned role must be recognized');
    assert.deepEqual(session.capabilities, role === 'ADMIN' ? roles : [role], 'Unexpected role capabilities');
    const csrf = { [session.csrfHeader]: session.csrfToken }, manual = { platform: 'Synthetic SSO smoke', companyName: 'Synthetic Company', requiredAction: 'Review', role: 'ADMIN', roles: ['ADMIN'] };
    await jsonRequest(`${backendUrl}/api/v1/review-items`, {}, ['REVIEWER', 'ADMIN'].includes(role) ? 200 : 403, jar);
    await jsonRequest(`${backendUrl}/api/v1/agent-settings`, {}, role === 'ADMIN' ? 200 : 403, jar);
    await jsonRequest(`${backendUrl}/api/v1/projections`, {}, role === 'ADMIN' ? 200 : 403, jar);
    await jsonRequest(`${backendUrl}/api/v1/manual-items`, jsonBody('POST', manual), 403, jar);
    await jsonRequest(`${backendUrl}/api/v1/manual-items`, jsonBody('POST', manual, csrf), ['COLLECTOR', 'ADMIN'].includes(role) ? 201 : 403, jar);
    if (role === 'ADMIN') {
      const current = await jsonRequest(`${backendUrl}/api/v1/agent-settings`, {}, 200, jar);
      assert.equal(current.enabled, false, 'AI must remain disabled in the synthetic database');
      await jsonRequest(`${backendUrl}/api/v1/agent-settings`, jsonBody('PUT', { ...current, enabled: false }), 403, jar);
      await jsonRequest(`${backendUrl}/api/v1/agent-settings`, jsonBody('PUT', { ...current, enabled: false }, csrf), 200, jar);
    }
    if (role === 'COLLECTOR') {
      const bearer = await bearerFromCode(jar, clientSecret), headers = { Authorization: `Bearer ${bearer}` };
      const identity = await jsonRequest(`${backendUrl}/api/v1/auth/session`, { headers }); assert.deepEqual(identity.roles, ['COLLECTOR']);
      await jsonRequest(`${backendUrl}/api/v1/manual-items`, jsonBody('POST', manual, headers), 201);
      await jsonRequest(`${backendUrl}/api/v1/review-items`, { headers }, 403);
    }
    assert.equal(session.logoutUrl, '/api/v1/auth/logout');
    await jsonRequest(`${backendUrl}${session.logoutUrl}`, { method: 'POST', headers: csrf }, 204, jar);
    const loggedOut = await jsonRequest(`${backendUrl}/api/v1/auth/session`, {}, 200, jar);
    assert.ok(!loggedOut.authenticated && !loggedOut.capabilities.length, 'Backend session survives logout');
    await jsonRequest(`${backendUrl}/api/v1/sources`, {}, 401, jar);
  }
  stage = 'acceptance complete';
} catch {
  console.error(JSON.stringify({ status: 'FAIL', stage, httpStatus: lastHttpStatus }));
  process.exitCode = 1;
} finally {
  try {
    cleanupOwned();
  } catch { cleanupFailed = true; process.exitCode = 1; console.error(JSON.stringify({ status: 'FAIL', stage: 'owned cleanup' })); }
  if (!process.exitCode && !cleanupFailed) console.log(JSON.stringify({ status: 'PASS', provider: 'actual Keycloak 26.7.5', flow: 'authorization code',
    roles, csrf: 'session writes and logout', bearer: 'actual COLLECTOR code-exchange token', cleanup: 'owned JVM and isolated compose project removed' }));
}
