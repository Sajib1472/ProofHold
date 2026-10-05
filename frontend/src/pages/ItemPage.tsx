import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";
import { ApiError, type PublicItem } from "../types";
import { useAuth } from "../auth";

export function ItemPage() {
  const { itemId } = useParams();
  const { user } = useAuth();
  const [item, setItem] = useState<PublicItem | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!itemId) return;
    api
      .getItem(Number(itemId))
      .then((body) => {
        setItem({
          id: body.id,
          locationId: body.locationId,
          locationName: body.locationName,
          category: body.category,
          foundOn: body.foundOn,
          status: body.status,
          pendingClaimCount: body.pendingClaimCount,
          version: body.version
        });
      })
      .catch((err) => setError(err instanceof ApiError ? err.problem.detail : "Could not load item."))
      .finally(() => setLoading(false));
  }, [itemId]);

  return (
    <main>
      <h1>Item</h1>
      {loading ? <p>Loading…</p> : null}
      {error ? <div className="banner error">{error}</div> : null}
      {item ? (
        <article className="card">
          <p>{item.category}</p>
          <p>
            {item.locationName} · {item.foundOn} · {item.status}
          </p>
          {user?.role === "CLAIMER" ? <Link to={`/items/${item.id}/claim`}>Start a claim</Link> : null}
        </article>
      ) : null}
    </main>
  );
}
