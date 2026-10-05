import { FormEvent, useEffect, useId, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "../api";
import { ApiError, type ChallengePrompt } from "../types";

export function ClaimWizardPage() {
  const { itemId } = useParams();
  const navigate = useNavigate();
  const formId = useId();
  const [prompts, setPrompts] = useState<ChallengePrompt[]>([]);
  const [answers, setAnswers] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [conflict, setConflict] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [done, setDone] = useState(false);

  useEffect(() => {
    if (!itemId) return;
    api
      .challenges(Number(itemId))
      .then((rows) => {
        setPrompts(rows);
        const start: Record<number, string> = {};
        for (const row of rows) start[row.id] = "";
        setAnswers(start);
      })
      .catch((err) => {
        if (err instanceof ApiError && err.status === 409) {
          setConflict("This item is already verified. You cannot claim it.");
        } else if (err instanceof ApiError && err.status === 401) {
          navigate("/login", { state: { expired: true } });
        } else {
          setError(err instanceof ApiError ? err.problem.detail : "Could not load questions.");
        }
      })
      .finally(() => setLoading(false));
  }, [itemId, navigate]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    if (!itemId) return;
    setBusy(true);
    setError("");
    setConflict("");
    setFieldErrors({});
    const payload = prompts.map((p) => ({ challengeId: p.id, value: answers[p.id] ?? "" }));
    try {
      await api.submitClaim(Number(itemId), payload, crypto.randomUUID());
      setDone(true);
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setConflict(err.problem.detail);
      } else if (err instanceof ApiError && err.status === 401) {
        navigate("/login", { state: { expired: true } });
      } else if (err instanceof ApiError && err.status === 422) {
        const next: Record<string, string> = {};
        for (const field of err.problem.errors ?? []) {
          next[field.field] = field.message;
        }
        setFieldErrors(next);
        setError(err.problem.detail);
      } else {
        setError(err instanceof ApiError ? err.problem.detail : "Claim failed.");
      }
    } finally {
      setBusy(false);
    }
  }

  if (done) {
    return (
      <main>
        <h1>Claim submitted</h1>
        <div className="banner empty">Pending staff review. You can follow it under My claims.</div>
      </main>
    );
  }

  return (
    <main>
      <h1>Claim wizard</h1>
      {loading ? <p>Loading questions…</p> : null}
      {conflict ? <div className="banner error">{conflict}</div> : null}
      {error ? <div className="banner error">{error}</div> : null}
      {!loading && prompts.length === 0 && !conflict ? (
        <div className="banner empty">No questions are available for this item.</div>
      ) : null}
      {prompts.length > 0 ? (
        <form className="card" onSubmit={onSubmit}>
          {prompts.map((prompt, index) => (
            <label className="field" key={prompt.id}>
              {prompt.prompt}
              <input
                id={`${formId}-${prompt.id}`}
                name={`answer-${prompt.id}`}
                value={answers[prompt.id] ?? ""}
                onChange={(e) => setAnswers((prev) => ({ ...prev, [prompt.id]: e.target.value }))}
                required
                autoFocus={index === 0}
              />
              {fieldErrors[`answers[${index}].value`] ? (
                <span className="err">{fieldErrors[`answers[${index}].value`]}</span>
              ) : null}
            </label>
          ))}
          <button type="submit" disabled={busy}>
            {busy ? "Submitting…" : "Submit claim"}
          </button>
        </form>
      ) : null}
    </main>
  );
}
