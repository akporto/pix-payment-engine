import http from "k6/http";
import { check, sleep } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.4.0/index.js";

export const options = {
  vus: 30,
  duration: "60s",
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<3000"],
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

const ACCOUNT_PAIRS = [
  { sender: "a0000000-0000-0000-0000-000000000001", receiver: "b0000000-0000-0000-0000-000000000002" },
  { sender: "c0000000-0000-0000-0000-000000000001", receiver: "c0000000-0000-0000-0000-000000000002" },
  { sender: "d0000000-0000-0000-0000-000000000001", receiver: "d0000000-0000-0000-0000-000000000002" },
  { sender: "e0000000-0000-0000-0000-000000000001", receiver: "e0000000-0000-0000-0000-000000000002" },
];

export default function () {
  const pair = ACCOUNT_PAIRS[__VU % ACCOUNT_PAIRS.length];
  const idempotencyKey = uuidv4();
  const payload = JSON.stringify({
    senderAccountId: pair.sender,
    receiverAccountId: pair.receiver,
    amount: 10.00,
  });

  const res = http.post(`${BASE_URL}/api/v1/payments`, payload, {
    headers: {
      "Content-Type": "application/json",
      "X-Idempotency-Key": idempotencyKey,
    },
  });

  check(res, {
    "2xx": (r) => r.status >= 200 && r.status < 300,
  });

  sleep(0.02);
}
