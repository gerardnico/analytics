# Analytics proxy

## Features

### Handling noisy data

Any web or product analytics solution is subject to attackers injecting noisy data, given the browser is sending events
to be stored.

The proxy imposes:

* strict rate limits,
* filtering to ensure events conform to the schema,
* and buffering so that the underlying sink receives inserts in reasonably sized batches.

Noisy data is always inserted, and you can delete them.
