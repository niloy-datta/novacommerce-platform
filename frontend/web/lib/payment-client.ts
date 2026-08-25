export type Payment = {
  id: string;
  orderId: string;
  customerId: string;
  amount: string;
  currency: string;
  status: string;
  idempotencyKey: string;
  gatewayProvider: string;
  gatewayTransactionId: string | null;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
};

const base = process.env.NEXT_PUBLIC_PAYMENT_API_URL ?? "http://localhost:8085";
let csrf: string | undefined;

async function getCsrfToken() {
  if (csrf) return csrf;
  const res = await fetch(`${base}/api/v1/payments/csrf`, { credentials: "include", cache: "no-store" });
  if (!res.ok) throw new Error("Unable to obtain payment CSRF token");
  csrf = (await res.json() as { token: string }).token;
  return csrf;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${base}${path}`, {
    ...init,
    credentials: "include",
    cache: "no-store",
    headers: {
      Accept: "application/json",
      ...init?.headers
    }
  });
  if (!res.ok) {
    throw new Error(`Payment API request failed (${res.status})`);
  }
  return res.json() as Promise<T>;
}

async function unsafeRequest<T>(path: string, method: string, body?: unknown, headers?: Record<string, string>): Promise<T> {
  return request<T>(path, {
    method,
    headers: {
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": await getCsrfToken(),
      ...headers
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
}

export const paymentClient = {
  authorize: (orderId: string, amount: string, currency: string, idempotencyKey: string, paymentToken: string) =>
    unsafeRequest<Payment>("/api/v1/payments/authorize", "POST", {
      orderId,
      amount: parseFloat(amount),
      currency,
      idempotencyKey,
      paymentToken
    }),
  getForOrder: (orderId: string) => request<Payment[]>(`/api/v1/payments/order/${orderId}`)
};
