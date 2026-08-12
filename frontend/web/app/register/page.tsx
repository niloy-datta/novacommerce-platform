"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { authClient, AuthApiError } from "@/lib/auth-client";

export default function RegisterPage() {
  const router = useRouter();
  const [email, setEmail] = useState(""); const [password, setPassword] = useState(""); const [confirm, setConfirm] = useState("");
  const [error, setError] = useState(""); const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError("");
    if (password.length < 12) { setError("Use a password with at least 12 characters."); return; }
    if (password !== confirm) { setError("Passwords do not match."); return; }
    setBusy(true);
    try { await authClient.register(email, password); router.replace("/login"); }
    catch (reason) { setError(reason instanceof AuthApiError ? reason.message : "Unable to create the account."); }
    finally { setBusy(false); }
  }
  return <main className="auth-shell"><Link href="/" className="health-link">← NovaCommerce</Link><section className="auth-card" aria-labelledby="register-title"><p className="eyebrow">Identity</p><h1 id="register-title">Create account</h1><p className="form-copy">New accounts are created with customer access.</p><form onSubmit={submit}><label>Email<input type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label><label>Password<input type="password" autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} minLength={12} required /></label><label>Confirm password<input type="password" autoComplete="new-password" value={confirm} onChange={(event) => setConfirm(event.target.value)} minLength={12} required /></label>{error && <p className="form-error" role="alert">{error}</p>}<button type="submit" disabled={busy}>{busy ? "Creating account…" : "Create account"}</button></form><p className="form-copy">Already registered? <Link href="/login" className="health-link">Sign in</Link></p></section></main>;
}
