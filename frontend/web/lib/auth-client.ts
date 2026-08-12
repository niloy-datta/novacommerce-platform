export type AuthUser = { id: string; email: string; roles: string[] };
type ApiErrorBody = { message?: string; code?: string; fieldErrors?: Record<string, string> };

const baseUrl = process.env.NEXT_PUBLIC_AUTH_API_URL ?? "http://localhost:8081";
let csrfToken: string | undefined;
let csrfHeader = "X-XSRF-TOKEN";

export class AuthApiError extends Error {
  constructor(public readonly code: string, message: string, public readonly fields: Record<string, string> = {}) { super(message); }
}

async function parseError(response: Response): Promise<AuthApiError> {
  const body = await response.json().catch(() => ({} as ApiErrorBody)) as ApiErrorBody;
  return new AuthApiError(body.code ?? "REQUEST_FAILED", body.message ?? "Request failed", body.fieldErrors ?? {});
}

async function requestCsrf(): Promise<void> {
  const response = await fetch(`${baseUrl}/api/v1/auth/csrf`, { credentials: "include", cache: "no-store" });
  if (!response.ok) throw await parseError(response);
  const body = await response.json() as { token: string; headerName: string };
  csrfToken = body.token;
  csrfHeader = body.headerName;
}

async function unsafe(path: string, body?: unknown): Promise<Response> {
  if (!csrfToken) await requestCsrf();
  const response = await fetch(`${baseUrl}${path}`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json", [csrfHeader]: csrfToken ?? "" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) throw await parseError(response);
  return response;
}

export const authClient = {
  async register(email: string, password: string): Promise<AuthUser> {
    const response = await unsafe("/api/v1/auth/register", { email, password });
    return response.json() as Promise<AuthUser>;
  },
  async login(email: string, password: string): Promise<AuthUser> {
    const response = await unsafe("/api/v1/auth/login", { email, password });
    const body = await response.json() as { user: AuthUser };
    await requestCsrf();
    return body.user;
  },
  async me(): Promise<AuthUser> {
    const response = await fetch(`${baseUrl}/api/v1/auth/me`, { credentials: "include", cache: "no-store" });
    if (!response.ok) throw await parseError(response);
    return response.json() as Promise<AuthUser>;
  },
  async logout(): Promise<void> {
    await unsafe("/api/v1/auth/logout");
    await requestCsrf();
  },
};
