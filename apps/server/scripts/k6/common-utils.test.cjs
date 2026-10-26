const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {test} = require('node:test');
const {SourceTextModule, SyntheticModule, createContext} = require('node:vm');

async function headers(pool = [], env = {}, options = {}) {
    const context = createContext({__ENV: env, open: () => JSON.stringify(pool)});
    const dependencies = {
        k6: {check: () => true},
        'k6/data': {SharedArray: function (_name, initialize) { return initialize(); }},
        'k6/execution': {scenario: {iterationInTest: 1}},
    };
    const module = new SourceTextModule(fs.readFileSync(path.join(__dirname, 'common-utils.js'), 'utf8'), {context});
    await module.link(async name => new SyntheticModule(Object.keys(dependencies[name]), function () {
        for (const [key, value] of Object.entries(dependencies[name])) this.setExport(key, value);
    }, {context}));
    await module.evaluate();
    return JSON.parse(JSON.stringify(module.namespace.getAuthHeaders(options).headers));
}

test('uses session cookie and CSRF values from the selected role pool', async () => {
    const pool = [
        {cookie: 'SESSION=user-one', csrfToken: 'csrf-one'},
        {cookie: 'SESSION=user-two', csrfToken: 'csrf-two'},
        {cookie: 'SESSION=admin', csrfToken: 'csrf-admin', admin: true},
    ];
    const user = await headers(pool);
    assert.equal(user.Cookie, 'SESSION=user-two');
    assert.equal(user['X-CSRF-TOKEN'], 'csrf-two');
    const admin = await headers(pool, {}, {isAdmin: true});
    assert.equal(admin.Cookie, 'SESSION=admin');
    assert.equal(admin['X-CSRF-TOKEN'], 'csrf-admin');
    assert.equal(admin.Authorization, undefined);
});

test('accepts environment values and explicit overrides without JWT fallback', async () => {
    const env = {SESSION_COOKIE: 'SESSION=env', CSRF_TOKEN: 'csrf-env', AUTH_TOKEN: 'unused-legacy-token'};
    const fromEnv = await headers([], env);
    assert.equal(fromEnv.Cookie, 'SESSION=env');
    assert.equal(fromEnv['X-CSRF-TOKEN'], 'csrf-env');
    const explicit = await headers([], env, {sessionCookie: 'SESSION=explicit', csrfToken: 'csrf-explicit'});
    assert.equal(explicit.Cookie, 'SESSION=explicit');
    assert.equal(explicit['X-CSRF-TOKEN'], 'csrf-explicit');
    assert.equal(explicit.Authorization, undefined);
});

test('keeps anonymous read-only load tests free of authentication headers', async () => {
    const anonymous = await headers();
    assert.equal(anonymous.Cookie, undefined);
    assert.equal(anonymous['X-CSRF-TOKEN'], undefined);
    assert.equal(anonymous.Authorization, undefined);
    assert.equal(anonymous['Content-Type'], 'application/json');
});
