import { FormEvent, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth";
import { ApiError } from "../types";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const expired = Boolean((location.state as { expired?: boolean } | null)?.expired);
  const [email, setEmail] = useState("alice@proofhold.local");
  const [password, setPassword] = useState("proofhold");
  const [error, setError] = useState(expired ? "Session expired. Log in again." : "");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setFieldErrors({});
    try {
      const user = await login(email, password);
      navigate(user.role === "STAFF" ? "/staff" : "/search", { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        const next: Record<string, string> = {};
        for (const field of err.problem.errors ?? []) {
          next[field.field] = field.message;
        }
        setFieldErrors(next);
        setError(err.problem.detail);
      } else {
        setError("Could not sign in.");
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <main>
      <h1>Log in</h1>
      {error ? <div className="banner error">{error}</div> : null}
      <form className="card" onSubmit={onSubmit}>
        <label className="field">
          Email
          <input
            name="email"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          {fieldErrors.email ? <span className="err">{fieldErrors.email}</span> : null}
        </label>
        <label className="field">
          Password
          <input
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
          />
          {fieldErrors.password ? <span className="err">{fieldErrors.password}</span> : null}
        </label>
        <button type="submit" disabled={busy}>
          {busy ? "Signing in…" : "Sign in"}
        </button>
      </form>
    </main>
  );
}
