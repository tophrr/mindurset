// Contract tests for the stub Worker. They load contract/fixtures/generation-ok.json
// instead of copying it, exactly as contract/README.md requires of both test suites.

import { describe, expect, it } from "vitest";
import fixture from "../../contract/fixtures/generation-ok.json";
import worker from "../src/index";

const fixtureKeys = Object.keys(fixture).sort();

describe("mindurset-worker contract stub", () => {
  it("GET /v1/health returns {\"ok\":true}", async () => {
    const res = await worker.fetch(new Request("http://stub.test/v1/health"));
    expect(res.status).toBe(200);
    expect(res.headers.get("content-type")).toContain("application/json");
    expect(await res.json()).toEqual({ ok: true });
  });

  it("POST /v1/reframe carries exactly the fixture keys, two perspectives and one micro_action", async () => {
    const res = await worker.fetch(
      new Request("http://stub.test/v1/reframe", {
        method: "POST",
        body: JSON.stringify({ thought: "I will fail no matter what." }),
      }),
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as typeof fixture;

    expect(Object.keys(body).sort()).toEqual(fixtureKeys);
    expect(Object.keys(body.micro_action).sort()).toEqual(
      Object.keys(fixture.micro_action).sort(),
    );
    expect(Object.keys(body.risk_level).sort()).toEqual(
      Object.keys(fixture.risk_level).sort(),
    );
    expect(body.perspectives).toHaveLength(2);
    expect(body.micro_action.minutes).toBeGreaterThanOrEqual(1);
    expect(body.micro_action.minutes).toBeLessThanOrEqual(15);
  });

  it("an unknown path returns 404 JSON", async () => {
    const res = await worker.fetch(
      new Request("http://stub.test/v1/unknown"),
    );
    expect(res.status).toBe(404);
    expect(res.headers.get("content-type")).toContain("application/json");
    expect(await res.json()).toEqual({ error: "not_found" });
  });
});
