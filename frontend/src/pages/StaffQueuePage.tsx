import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import { ApiError, type PublicItem } from "../types";

export function StaffQueuePage() {
  const [rows, setRows] = useState<PublicItem[]>([]);
  const [rejectedNote] = useState("Rejected claims stay on the item. They do not block the queue.");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const params = new URLSearchParams();
    params.set("status", "CLAIM_PENDING");
    api
      .listItems(params)
      .then((page) => setRows(page.content))
      .catch((err) => setError(err instanceof ApiError ? err.problem.detail : "Queue failed."))
      .finally(() => setLoading(false));
  }, []);

  return (
    <main>
      <h1>Staff desk</h1>
      <p>
        <Link to="/staff/log">Log a found item</Link>
      </p>
      <p>{rejectedNote}</p>
      {loading ? <p>Loading…</p> : null}
      {error ? <div className="banner error">{error}</div> : null}
      {!loading && rows.length === 0 ? <div className="banner empty">No pending claims.</div> : null}
      <div className="cards">
        {rows.map((item) => (
          <article className="card" key={item.id}>
            <p>
              {item.category} · {item.locationName} · {item.pendingClaimCount} pending
            </p>
            <Link to={`/staff/items/${item.id}`}>Review</Link>
          </article>
        ))}
      </div>
    </main>
  );
}
