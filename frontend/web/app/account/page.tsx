"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { authClient, AuthApiError, type AuthUser } from "@/lib/auth-client";

export default function AccountPage() {
  const router = useRouter(); const [user, setUser] = useState<AuthUser>(); const [error, setError] = useState(""); const [busy, setBusy] = useState(false);
  useEffect(() => { authClient.me().then(setUser).catch((reason) => { if (reason instanceof AuthApiError && reason.code === "AUTHENTICATION_REQUIRED") router.replace("/login"); else setError("Unable to load your account."); }); }, [router]);
  async function logout() { setBusy(true); try { await authClient.logout(); router.replace("/login"); } catch { setError("Unable to sign out. Please try again."); } finally { setBusy(false); } }
  if (error) return <main className="auth-shell"><p className="form-error" role="alert">{error}</p><Link href="/login" className="health-link">Sign in</Link></main>;
  if (!user) return <main className="auth-shell"><p className="form-copy" role="status">Loading account…</p></main>;
  return <main className="auth-shell"><Link href="/" className="health-link">← NovaCommerce</Link><section className="auth-card" aria-labelledby="account-title"><p className="eyebrow">Account</p><h1 id="account-title">Your identity</h1><dl className="account-details"><div><dt>Email</dt><dd>{user.email}</dd></div><div><dt>Roles</dt><dd>{user.roles.join(", ")}</dd></div></dl><button onClick={logout} disabled={busy}>{busy ? "Signing out…" : "Sign out"}</button></section></main>;
}
