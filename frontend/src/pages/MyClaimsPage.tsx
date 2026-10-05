import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import { ApiError, type Claim } from "../types";

export function MyClaimsPage() {
  const [rows, setRows] = useState<Claim[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    api
      .myClaims()
      .then((page) => setRows(page.content))
      .catch((err) => setError(err instanceof ApiError ? err.problem.detail : "Could not load claims."))
      .finally(() => setLoading(false));
  }, []);

  return (
    <main>
      <h1>My claims</h1>
      {loading ? <p>Loading…</p> : null}
      {error ? <div className="banner error">{error}</div> : null}
      {!loading && rows.length === 0 ? <div className="banner empty">You have not filed a claim yet.</div> : null}
      <div className="cards">
        {rows.map((claim) => (
          <article className="card" key={claim.id}>
            <p>
              Item {claim.itemId} · {claim.status}
            </p>
            <Link to={`/items/${claim.itemId}`}>Open item</Link>
          </article>
        ))}
      </div>
    </main>
  );
}
