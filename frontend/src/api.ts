import { ApiError, type AuditPage, type Claim, type ClaimPage, type ChallengePrompt, type Handoff, type LocationDesk, type Problem, type PublicItem, type PublicItemPage, type StaffClaimView, type StaffItem, type TokenResponse, type User } from "./types";

const TOKEN_KEY = "proofhold.token";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null): void {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const token = getToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  const response = await fetch(path, { ...init, headers });
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const problem = (data ?? {
      type: "about:blank",
      title: response.statusText,
      status: response.status,
      detail: response.statusText,
      instance: path
    }) as Problem;
    throw new ApiError(problem, response.status);
  }
  return data as T;
}

export const api = {
  login(email: string, password: string) {
    return request<TokenResponse>("/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password })
    });
  },
  me() {
    return request<User>("/v1/auth/me");
  },
  listItems(params: URLSearchParams) {
    return request<PublicItemPage>(`/v1/items?${params.toString()}`);
  },
  getItem(id: number) {
    return request<PublicItem>(`/v1/items/${id}`);
  },
  challenges(itemId: number) {
    return request<ChallengePrompt[]>(`/v1/items/${itemId}/challenges`);
  },
  submitClaim(itemId: number, answers: { challengeId: number; value: string }[], idempotencyKey: string) {
    return request<Claim>(`/v1/items/${itemId}/claims`, {
      method: "POST",
      headers: { "Idempotency-Key": idempotencyKey },
      body: JSON.stringify({ answers })
    });
  },
  myClaims() {
    return request<ClaimPage>("/v1/me/claims");
  },
  locations() {
    return request<LocationDesk[]>("/v1/locations");
  },
  async getStaffItem(id: number) {
    const headers = new Headers();
    const token = getToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
    const response = await fetch(`/v1/items/${id}`, { headers });
    const data = (await response.json()) as StaffItem | Problem;
    if (!response.ok) {
      throw new ApiError(data as Problem, response.status);
    }
    return { item: data as StaffItem, etag: response.headers.get("ETag") };
  },
  createItem(body: unknown) {
    return request<StaffItem>("/v1/items", { method: "POST", body: JSON.stringify(body) });
  },
  itemClaims(itemId: number) {
    return request<StaffClaimView[]>(`/v1/items/${itemId}/claims`);
  },
  decide(claimId: number, etag: string, decision: "APPROVE" | "REJECT", reason: string) {
    return request<{ claim: Claim; itemVersion: number }>(`/v1/claims/${claimId}/decision`, {
      method: "POST",
      headers: { "If-Match": etag },
      body: JSON.stringify({ decision, reason })
    });
  },
  createHandoff(itemId: number, etag: string, slotStart: string, slotEnd: string) {
    return request<Handoff>(`/v1/items/${itemId}/handoffs`, {
      method: "POST",
      headers: { "Idempotency-Key": crypto.randomUUID(), "If-Match": etag },
      body: JSON.stringify({ slotStart, slotEnd })
    });
  },
  completeHandoff(handoffId: number, etag: string) {
    return request<Handoff>(`/v1/handoffs/${handoffId}/complete`, {
      method: "POST",
      headers: { "If-Match": etag }
    });
  },
  donate(itemId: number, etag: string) {
    return request<StaffItem>(`/v1/items/${itemId}/donate`, {
      method: "POST",
      headers: { "If-Match": etag }
    });
  },
  audit(itemId: number) {
    return request<AuditPage>(`/v1/items/${itemId}/audit`);
  }
};
