"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { authClient, AuthApiError } from "@/lib/auth-client";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(""); setBusy(true);
    try { await authClient.login(email, password); router.replace("/account"); }
    catch (reason) { setError(reason instanceof AuthApiError ? reason.message : "Unable to sign in. Please try again."); }
    finally { setBusy(false); }
  }
  return <main className="auth-shell"><Link href="/" className="health-link">← NovaCommerce</Link><section className="auth-card" aria-labelledby="login-title"><p className="eyebrow">Identity</p><h1 id="login-title">Sign in</h1><p className="form-copy">Use your NovaCommerce account to access protected platform areas.</p><form onSubmit={submit}><label>Email<input type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label><label>Password<input type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required /></label>{error && <p className="form-error" role="alert">{error}</p>}<button type="submit" disabled={busy}>{busy ? "Signing in…" : "Sign in"}</button></form><p className="form-copy">New here? <Link href="/register" className="health-link">Create an account</Link></p></section></main>;
}
