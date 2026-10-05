export type Role = "STAFF" | "CLAIMER" | "FINDER";

export type ItemCategory =
  | "WALLET"
  | "KEYS"
  | "PHONE"
  | "BAG"
  | "LAPTOP"
  | "ID_CARD"
  | "JEWELRY"
  | "OTHER";

export type ItemStatus =
  | "HELD"
  | "CLAIM_PENDING"
  | "VERIFIED"
  | "READY_FOR_PICKUP"
  | "RETURNED"
  | "EXPIRED"
  | "DONATED";

export type ClaimStatus = "PENDING" | "VERIFIED" | "REJECTED";

export interface User {
  id: number;
  email: string;
  role: Role;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface PageInfo {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PublicItem {
  id: number;
  locationId: number;
  locationName: string;
  category: ItemCategory;
  foundOn: string;
  status: ItemStatus;
  pendingClaimCount: number;
  version: number;
}

export interface PublicItemPage {
  content: PublicItem[];
  page: PageInfo;
}

export interface ChallengePrompt {
  id: number;
  prompt: string;
}

export interface Claim {
  id: number;
  itemId: number;
  claimerId: number;
  status: ClaimStatus;
  answerScore?: number | null;
  reason?: string | null;
  createdAt: string;
  decidedAt?: string | null;
}

export interface ClaimPage {
  content: Claim[];
  page: PageInfo;
}

export interface FieldError {
  field: string;
  code: string;
  message: string;
}

export interface Problem {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
  errors?: FieldError[];
}

export class ApiError extends Error {
  constructor(
    public problem: Problem,
    public status: number
  ) {
    super(problem.detail);
  }
}

export interface LocationDesk {
  id: number;
  name: string;
  timezone: string;
  openFrom: string;
  openTo: string;
}

export interface StaffChallenge {
  id: number;
  prompt: string;
}

export interface StaffItem extends PublicItem {
  holdUntil?: string;
  whereFound?: string;
  photoUrl?: string | null;
  serial?: string | null;
  uniqueMarks?: string | null;
  fullDescription?: string | null;
  challenges?: StaffChallenge[];
}

export interface StaffClaimView {
  claim: Claim;
  answers: { challengeId: number; prompt: string; value: string }[];
}

export interface AuditEvent {
  id: number;
  itemId: number;
  actorId: number;
  action: string;
  at: string;
  payload?: Record<string, unknown>;
}

export interface AuditPage {
  content: AuditEvent[];
  page: PageInfo;
}

export interface Handoff {
  id: number;
  itemId: number;
  claimId: number;
  slotStart: string;
  slotEnd: string;
  status: string;
}

