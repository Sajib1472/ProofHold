import { FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api";
import { ApiError, type ItemCategory, type LocationDesk } from "../types";

const CATEGORIES: ItemCategory[] = ["WALLET", "KEYS", "PHONE", "BAG", "LAPTOP", "ID_CARD", "JEWELRY", "OTHER"];

export function StaffLogItemPage() {
  const navigate = useNavigate();
  const [locations, setLocations] = useState<LocationDesk[]>([]);
  const [locationId, setLocationId] = useState("1");
  const [category, setCategory] = useState<ItemCategory>("WALLET");
  const [whereFound, setWhereFound] = useState("2nd floor");
  const [fullDescription, setFullDescription] = useState("black leather wallet");
  const [serial, setSerial] = useState("");
  const [photoUrl, setPhotoUrl] = useState("");
  const [q1, setQ1] = useState("What initials are inside?");
  const [a1, setA1] = useState("JS");
  const [q2, setQ2] = useState("About how many cards?");
  const [a2, setA2] = useState("8");
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.locations().then((rows) => {
      setLocations(rows);
      if (rows[0]) setLocationId(String(rows[0].id));
    });
  }, []);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setFieldErrors({});
    const holdUntil = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString();
    try {
      const item = await api.createItem({
        locationId: Number(locationId),
        category,
        foundAt: new Date().toISOString(),
        holdUntil,
        whereFound,
        photoUrl: photoUrl || undefined,
        serial: serial || undefined,
        fullDescription,
        challenges: [
          { prompt: q1, expectedAnswer: a1 },
          { prompt: q2, expectedAnswer: a2 }
        ]
      });
      navigate(`/staff/items/${item.id}`);
    } catch (err) {
      if (err instanceof ApiError) {
        const next: Record<string, string> = {};
        for (const field of err.problem.errors ?? []) next[field.field] = field.message;
        setFieldErrors(next);
        setError(err.problem.detail);
      } else {
        setError("Could not log item.");
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <main>
      <h1>Log found item</h1>
      {error ? <div className="banner error">{error}</div> : null}
      <form className="card" onSubmit={onSubmit}>
        <label className="field">
          Desk
          <select value={locationId} onChange={(e) => setLocationId(e.target.value)}>
            {locations.map((loc) => (
              <option key={loc.id} value={loc.id}>
                {loc.name}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          Category
          <select value={category} onChange={(e) => setCategory(e.target.value as ItemCategory)}>
            {CATEGORIES.map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </label>
        <label className="field">
          Where found
          <input value={whereFound} onChange={(e) => setWhereFound(e.target.value)} required />
        </label>
        <label className="field">
          Full description
          <textarea value={fullDescription} onChange={(e) => setFullDescription(e.target.value)} required />
        </label>
        <label className="field">
          Serial
          <input value={serial} onChange={(e) => setSerial(e.target.value)} />
        </label>
        <label className="field">
          Photo URL
          <input value={photoUrl} onChange={(e) => setPhotoUrl(e.target.value)} />
        </label>
        <label className="field">
          Challenge 1
          <input value={q1} onChange={(e) => setQ1(e.target.value)} required minLength={4} />
          <input value={a1} onChange={(e) => setA1(e.target.value)} required />
          {fieldErrors.challenges ? <span className="err">{fieldErrors.challenges}</span> : null}
        </label>
        <label className="field">
          Challenge 2
          <input value={q2} onChange={(e) => setQ2(e.target.value)} required minLength={4} />
          <input value={a2} onChange={(e) => setA2(e.target.value)} required />
        </label>
        <button type="submit" disabled={busy}>
          {busy ? "Saving…" : "Log item"}
        </button>
      </form>
    </main>
  );
}
