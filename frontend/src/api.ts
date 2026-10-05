import { ApiError, type Claim, type ClaimPage, type ChallengePrompt, type Problem, type PublicItem, type PublicItemPage, type TokenResponse, type User } from "./types";

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
  }
};
