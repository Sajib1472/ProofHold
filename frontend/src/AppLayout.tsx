import { Link, Navigate, NavLink, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "./auth";

export function AppLayout() {
  const { user, logout } = useAuth();
  return (
    <>
      <header className="bar">
        <strong>ProofHold</strong>
        <nav>
          {user ? (
            <>
              <NavLink to="/search">Search</NavLink>
              {user.role === "CLAIMER" ? <NavLink to="/claims">My claims</NavLink> : null}
              {user.role === "STAFF" ? <NavLink to="/staff">Staff</NavLink> : null}
              <button type="button" onClick={logout}>
                Log out {user.email}
              </button>
            </>
          ) : (
            <Link to="/login">Log in</Link>
          )}
        </nav>
      </header>
      <Outlet />
    </>
  );
}

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, ready } = useAuth();
  const location = useLocation();
  if (!ready) {
    return <main>Loading session…</main>;
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location, expired: true }} />;
  }
  return children;
}

export function RequireStaff({ children }: { children: React.ReactNode }) {
  const { user, ready } = useAuth();
  if (!ready) {
    return <main>Loading session…</main>;
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ expired: true }} />;
  }
  if (user.role !== "STAFF") {
    return (
      <main>
        <div className="banner error">Staff only. The API also returns 403 for these routes.</div>
      </main>
    );
  }
  return children;
}
