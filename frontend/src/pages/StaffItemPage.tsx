import { FormEvent, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../api";
import { ApiError, type AuditEvent, type StaffClaimView, type StaffItem } from "../types";

export function StaffItemPage() {
  const { itemId } = useParams();
  const [item, setItem] = useState<StaffItem | null>(null);
  const [etag, setEtag] = useState<string | null>(null);
  const [claims, setClaims] = useState<StaffClaimView[]>([]);
  const [audit, setAudit] = useState<AuditEvent[]>([]);
  const [reason, setReason] = useState("");
  const [slotStart, setSlotStart] = useState("");
  const [slotEnd, setSlotEnd] = useState("");
  const [handoffId, setHandoffId] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  function reload() {
    if (!itemId) return;
    const id = Number(itemId);
    Promise.all([api.getStaffItem(id), api.itemClaims(id), api.audit(id)])
      .then(([got, claimRows, auditPage]) => {
        setItem({
          id: got.item.id,
          locationId: got.item.locationId,
          locationName: got.item.locationName,
          category: got.item.category,
          foundOn: got.item.foundOn,
          status: got.item.status,
          pendingClaimCount: got.item.pendingClaimCount,
          version: got.item.version,
          holdUntil: got.item.holdUntil,
          whereFound: got.item.whereFound,
          photoUrl: got.item.photoUrl,
          serial: got.item.serial,
          uniqueMarks: got.item.uniqueMarks,
          fullDescription: got.item.fullDescription,
          challenges: got.item.challenges
        });
        setEtag(got.etag);
        setClaims(claimRows);
        setAudit(auditPage.content);
      })
      .catch((err) => setError(err instanceof ApiError ? err.problem.detail : "Load failed."))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [itemId]);

  async function decide(claimId: number, decision: "APPROVE" | "REJECT") {
    if (!etag) return;
    setError("");
    try {
      await api.decide(claimId, etag, decision, reason);
      reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.problem.detail : "Decision failed.");
    }
  }

  async function book(event: FormEvent) {
    event.preventDefault();
    if (!item || !etag) return;
    try {
      const handoff = await api.createHandoff(item.id, etag, new Date(slotStart).toISOString(), new Date(slotEnd).toISOString());
      setHandoffId(String(handoff.id));
      reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.problem.detail : "Pickup booking failed.");
    }
  }

  async function markReturned() {
    if (!etag || !handoffId) return;
    try {
      await api.completeHandoff(Number(handoffId), etag);
      reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.problem.detail : "Complete failed.");
    }
  }

  async function donate() {
    if (!item || !etag) return;
    try {
      await api.donate(item.id, etag);
      reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.problem.detail : "Donate failed.");
    }
  }

  const pending = claims.filter((row) => row.claim.status === "PENDING");
  const rejected = claims.filter((row) => row.claim.status === "REJECTED");

  return (
    <main>
      <h1>Staff item</h1>
      {loading ? <p>Loading…</p> : null}
      {error ? <div className="banner error">{error}</div> : null}
      {item ? (
        <article className="card">
          <p>
            {item.category} · {item.status} · {item.locationName}
          </p>
          <p>{item.fullDescription}</p>
          <p>Serial: {item.serial || "—"}</p>
          {item.photoUrl ? <p>Photo: {item.photoUrl}</p> : null}
        </article>
      ) : null}

      {rejected.length > 0 ? (
        <div className="banner empty">
          {rejected.length} rejected claim(s) (including auto-reject after another winner). They do not block the queue.
        </div>
      ) : null}

      {pending.map((row) => (
        <section className="card" key={row.claim.id}>
          <h2>Claim {row.claim.id} · score {row.claim.answerScore ?? "—"}</h2>
          {row.answers.map((answer) => (
            <p key={answer.challengeId}>
              {answer.prompt}: {answer.value}
            </p>
          ))}
          <label className="field">
            Reason
            <input value={reason} onChange={(e) => setReason(e.target.value)} />
          </label>
          <button type="button" onClick={() => decide(row.claim.id, "APPROVE")}>
            Approve
          </button>{" "}
          <button type="button" onClick={() => decide(row.claim.id, "REJECT")}>
            Reject
          </button>
        </section>
      ))}

      <form className="card" onSubmit={book}>
        <h2>Pickup</h2>
        <label className="field">
          Start
          <input type="datetime-local" value={slotStart} onChange={(e) => setSlotStart(e.target.value)} required />
        </label>
        <label className="field">
          End
          <input type="datetime-local" value={slotEnd} onChange={(e) => setSlotEnd(e.target.value)} required />
        </label>
        <button type="submit">Book pickup</button>
        <label className="field">
          Handoff id
          <input value={handoffId} onChange={(e) => setHandoffId(e.target.value)} />
        </label>
        <button type="button" onClick={markReturned}>
          Mark returned
        </button>
        <button type="button" onClick={donate}>
          Donate expired
        </button>
      </form>

      <section className="card">
        <h2>Audit</h2>
        {audit.length === 0 ? <p>No events yet.</p> : null}
        <ol>
          {audit.map((event) => (
            <li key={event.id}>
              {event.at} · {event.action}
            </li>
          ))}
        </ol>
      </section>
    </main>
  );
}
