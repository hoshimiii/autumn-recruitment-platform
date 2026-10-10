import test from 'node:test';
import assert from 'node:assert/strict';
import { extractFixture, requireOrigin, extractWithProvider } from './protocol.mjs';
import { startFixture } from './fixture.mjs';

test('fixture parsing uses visible text and preserves unknown optional fields', () => {
  const row=extractFixture('RowId: r-1\nItem: company\nKind: COMPANY\nCompany: Acme');
  assert.equal(row.sourceRowId,'r-1'); assert.deepEqual(row.fields,{companyName:'Acme'});
  assert.throws(()=>extractFixture('Company: Acme'),/identity/);
});
test('origin and access barriers fail closed', () => {
  assert.throws(()=>requireOrigin('https://other.test/','https://allowed.test'),/origin/);
  assert.throws(()=>requireOrigin('https://allowed.test/login','https://allowed.test'),/barrier/);
});
test('local provider stub follows the same extraction contract without paid calls', async () => {
  const fixture=await startFixture();
  try {
    const row=await extractWithProvider('RowId: r-1\nItem: role-1\nKind: ROLE\nCompany: Acme\nTitle: Engineer\nOfficialJobId: j-1\nJD: Build Java',
      {baseUrl:fixture.origin,token:'fixture-only',model:'fixture-model'});
    assert.equal(row.fields.title,'Engineer');assert.equal(row.kind,'ROLE');
  } finally { await fixture.close(); }
});
