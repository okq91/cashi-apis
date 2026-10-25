# Performance testing with JMeter

`payments-load-test.jmx` load-tests the payment endpoints and reports response time and throughput.

## What the plan does

| Step | Detail |
|---|---|
| **setUp** (once) | `POST /api/auth/register` creates a throwaway user, extracts the `accessToken` and shares it with the load threads. Registration is done once on purpose: it hashes the password with BCrypt, which is CPU heavy and would otherwise dominate the results. |
| **Payments load** | Each virtual user loops for the configured duration: `POST /api/payments`, then `GET /api/payments` (first page, 10 items), with a random think time between requests. |
| **Assertions** | `POST` must return `201`, `GET` must return `200` with an `items` array, and every response must come back within `maxMs`. A sample that breaks any of these counts as an error. |

## Setup

```bash
brew install jmeter        # or download from https://jmeter.apache.org/download_jmeter.cgi
```

Start the API (`./gradlew bootRun`) with `JWT_SECRET` set (see the main README).

> **Every run writes data to Firestore.** Each run creates one user plus thousands of payments, and Firestore bills per read and write. Point the API at a **dev/test Firebase project**, not production, and keep the first runs short.

## Run (non-GUI, recommended)

```bash
cd perf
jmeter -n -t payments-load-test.jmx \
  -Jhost=localhost -Jport=8080 \
  -Jthreads=20 -Jrampup=10 -Jduration=60 \
  -l results.jtl -e -o report
open report/index.html
```

`-n` runs without the GUI (the GUI skews load numbers; use it only to edit the plan). `-l` stores raw samples and `-e -o` builds the HTML dashboard. The `report` folder must not exist or must be empty.

### Properties

| Property | Default | Meaning |
|---|---|---|
| `host` / `port` / `protocol` | `localhost` / `8080` / `http` | Target API |
| `threads` | `20` | Concurrent virtual users |
| `rampup` | `10` | Seconds to start all threads |
| `duration` | `60` | Seconds the load runs (keep under the 15 minute access token lifetime, or raise `JWT_EXPIRATION`) |
| `thinkTime` / `thinkTimeRange` | `200` / `200` | Pause between requests: 200 ms plus a random 0 to 200 ms |
| `maxMs` | `2000` | Response-time limit per request; slower responses are counted as errors |

Remove `-Jhost=...` to use a different environment, for example `-Jhost=staging.example.com -Jport=443 -Jprotocol=https`.

## Suggested runs

| Goal | Command |
|---|---|
| Smoke test | `-Jthreads=1 -Jduration=15` |
| Baseline | `-Jthreads=10 -Jduration=120` |
| Load | `-Jthreads=50 -Jrampup=30 -Jduration=300` |
| Stress (find the breaking point) | Repeat with `-Jthreads=100`, `200`, `400` until the error rate or p95 jumps |

## Reading the results

Open `report/index.html`. The numbers to look at:

- **Throughput** (requests/sec) in the Statistics table, per endpoint.
- **Response time**: the average, median, and especially the **90th/95th/99th percentile**. The average hides slow outliers.
- **Error %**: should stay at 0. Non-zero means failed assertions, timeouts or `5xx`.
- **Response Times Over Time** and **Active Threads Over Time**: latency should stay flat as threads ramp up. A rising curve means you've reached saturation.

Throughput grows with threads until the server or Firestore saturates. After that, response times climb and throughput flattens or drops. That knee is the capacity of the API.

## Notes

- Run JMeter on a different machine than the API if you can. On one machine they compete for CPU.
- The first seconds include JVM warm-up and Firestore connection setup, so use a ramp-up and ignore the very start.
- `POST /api/payments` is mostly a Firestore write, so its latency reflects network distance to the Firestore region.
- Generated `results.jtl` and `report/` are not meant to be committed.
