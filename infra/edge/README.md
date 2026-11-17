# NPM edge deployment

This separate stack preserves the production NPM 2.14.0 image, SQLite data,
certificates, timezone, and both existing networks. It does not deploy the app or DB.
Admin port 81 binds only to host loopback; public HTTP/HTTPS remain on 80/443.

Access from the developer computer:

```sh
ssh -N -L 8181:127.0.0.1:81 zupzup
# Open http://localhost:8181
```

Run the Edge CD workflow after merging. Deployment briefly stops HTTP/HTTPS to
snapshot the SQLite data consistently. The previous container remains stopped
under a timestamped rollback name. No volumes are deleted. The deployment uses
the already installed image and will not implicitly upgrade NPM.

If startup fails, the script restores the old container automatically. Manual
rollback: stop/remove only the new `sugang-helper-npm` container, rename the
reported rollback container to `sugang-helper-npm`, then start it. This also
restores the previous public admin binding; investigate and reapply isolation.
Snapshots are root-readable archives beside the NPM data directory.

After deployment verify public application HTTPS and certificate validity, and
that external TCP 81 cannot connect while the SSH tunnel can open the login page.

Application CD excludes `infra/edge/**`. Changes only to the application workflow
also require manual `workflow_dispatch`; they do not redeploy the application.
Edge CD validates its contract on pull requests and deploys only when manually
started from `main`. Both deployment workflows share the production concurrency
group, so they cannot deploy at the same time.
