# Visible browser worker intake

Run from the repository root after `npm ci`. Installed Chrome is required. The
supervisor starts at most two independent Node/browser worker processes; each
claims a fixed task and returns evidenced rows to the Java pending workflow.
These are application worker agents, not native Codex or CUA subagents.

## Non-billable acceptance

Build the Java JAR, then start an isolated in-memory backend in one terminal:

```powershell
java -jar apps/server/target/recruitment-server-0.1.0-SNAPSHOT.jar --server.port=18189 '--spring.datasource.url=jdbc:sqlite:file:browser-fixture?mode=memory&cache=shared' --recruitops.browser.fixtures-enabled=true --recruitops.collector.allow-private-addresses=true --recruitops.collector.minimum-floor-seconds=0 --recruitops.collector.scheduler-enabled=false
```

In another terminal:

```powershell
node --test scripts/browser-workers/*.test.mjs
node scripts/browser-workers/smoke.mjs http://127.0.0.1:18189/api/v1 --provider-stub
```

The smoke starts a temporary loopback public-table/provider fixture and creates
one synthetic source plus two tasks in that isolated database. It asserts two
separate workers, shared pacing, one manual company record, one pending role and
zero auto-publications. No external source or model is called. Stop only the
backend started in that terminal with Ctrl+C. Fixture flags must remain off in
normal operation; the smoke is not a live Feishu or paid-model acceptance test.

## Approved public source

An administrator configures a public source, confirmed terms, robots policy and
a `selectors` JSON containing a visible row selector, for example
`{"row":"article"}` for a page that actually uses those rows. The source may use
`MANUAL` mode for browser-visible intake; this does not enable the HTTP collector.
After confirming visible browser extraction is permitted, approve it through:

```text
POST /api/v1/browser-tasks/sources/{sourceId}/approve
{"visibleBrowserApproved":true,"adapter":"sub2api"}
```

Source changes invalidate approval. A COLLECTOR creates fixed zero-based row
ranges with exclusive `rowEnd` in Add/Import, or through:

```text
POST /api/v1/browser-tasks
{"sourceId":6,"rowStart":0,"rowEnd":2,"maxChars":4000}
```

Set `SUB2API_BASE_URL`, `SUB2API_TOKEN` and `SUB2API_MODEL` in the worker process
environment. Only an explicitly configured loopback provider is supported;
real model calls may incur provider costs and need separate authorization.
In OIDC mode also set `BROWSER_WORKER_TOKEN` to a real validated COLLECTOR token.
The browser contexts contain no imported login cookies. Run existing task IDs:

```powershell
node scripts/browser-workers/supervisor.mjs --api http://127.0.0.1:8080/api/v1 --tasks 1,2 --workers 2 --headed
```

Every read/XHR must pass current origin, robots and source policy checks
and shared SQL pacing. Login, CAPTCHA, 401/403/429, copy/export restrictions,
mutations, downloads, HTTP redirects and unapproved origins stop intake. Configure
the final public URL directly; workers cannot safely replay redirected documents
under the original browser URL. Restricting the source
to one approved origin can reject pages with third-party assets; this is a
deliberate boundary, not permission to widen the origin or bypass a barrier.

Results must contain explicitly visible stable row IDs, item discriminators and
field values. Unknown department/cohort fields remain absent. COMPANY rows go
to manual records; ROLE additionally needs independently visible title, official
job ID and JD, and goes to pending review. Human review controls publication.
The UI can import a result from an already claimed task, with its current worker
ID and lease token; arbitrary unleased JSON cannot publish or complete a task.

Results must cover the entire assigned range: exactly `rowEnd-rowStart` rows and
each zero-based index once. Empty, partial or duplicate-index results are rejected
transactionally. The task list exposes status and range/budgets, never another
worker's ID or lease token; the claimant obtains its credentials from `/claim`.

Workers report ordinary operational failures through the fenced `/fail` endpoint:
only that task becomes `FAILED`, and source approval and peer tasks remain usable.
This includes missing rows/selectors, browser/network/time-budget failures,
local provider errors and malformed extraction output. Confirmed access barriers
use `/stop`: that task becomes `BLOCKED` and source approval is revoked so peers
stop before further reads. Request-policy errors carry typed classification;
401/403/429, login/CAPTCHA, robots denial, unapproved origin and copy/export
restrictions remain source barriers. A same-origin redirect to an allowed URL
is an operational failure requiring a corrected final URL; an unapproved or
denied redirect remains a source barrier. Both paths immediately close the
worker context. An unavailable robots/API check prevents forwarding but preserves
approval; a previously revoked policy remains revoked.
