import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import { ApiError, type ItemCategory, type PublicItem, type PublicItemPage } from "../types";

const CATEGORIES: ItemCategory[] = ["WALLET", "KEYS", "PHONE", "BAG", "LAPTOP", "ID_CARD", "JEWELRY", "OTHER"];

export function SearchPage() {
  const [q, setQ] = useState("");
  const [category, setCategory] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<PublicItemPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  function load(nextPage = page, nextQ = q, nextCategory = category) {
    const params = new URLSearchParams();
    params.set("page", String(nextPage));
    params.set("size", "10");
    if (nextQ.trim()) params.set("q", nextQ.trim());
    if (nextCategory) params.set("category", nextCategory);
    setLoading(true);
    api
      .listItems(params)
      .then(setResult)
      .catch((err) => setError(err instanceof ApiError ? err.problem.detail : "Search failed."))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    setPage(0);
    load(0, q, category);
  }

  return (
    <main>
      <h1>Found items</h1>
      <form onSubmit={onSubmit} className="card">
        <label className="field">
          Search
          <input name="q" value={q} onChange={(e) => setQ(e.target.value)} />
        </label>
        <label className="field">
          Category
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">Any</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </label>
        <button type="submit">Filter</button>
      </form>
      {loading ? <p>Loading…</p> : null}
      {error ? <div className="banner error">{error}</div> : null}
      {!loading && result && result.content.length === 0 ? (
        <div className="banner empty">No items match. Try another category or clear the search.</div>
      ) : null}
      <div className="cards">
        {result?.content.map((item) => (
          <ItemCard key={item.id} item={item} />
        ))}
      </div>
      {result && result.page.totalPages > 1 ? (
        <p>
          <button type="button" disabled={page === 0} onClick={() => { const n = page - 1; setPage(n); load(n); }}>
            Previous
          </button>{" "}
          page {result.page.page + 1} of {result.page.totalPages}{" "}
          <button
            type="button"
            disabled={page + 1 >= result.page.totalPages}
            onClick={() => { const n = page + 1; setPage(n); load(n); }}
          >
            Next
          </button>
        </p>
      ) : null}
    </main>
  );
}

function ItemCard({ item }: { item: PublicItem }) {
  return (
    <article className="card">
      <h2>{item.category}</h2>
      <p>
        {item.locationName} · found {item.foundOn} · {item.status.toLowerCase().replaceAll("_", " ")} ·{" "}
        {item.pendingClaimCount} pending claims
      </p>
      <Link to={`/items/${item.id}`}>Open item</Link>
    </article>
  );
}
