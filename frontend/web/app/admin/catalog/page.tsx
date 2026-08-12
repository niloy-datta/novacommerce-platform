"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { authClient } from "@/lib/auth-client";
import { createCatalogProduct, getCatalogCsrf } from "@/lib/catalog-client";

export default function AdminCatalogPage() {
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [access, setAccess] = useState<"checking" | "allowed" | "denied">("checking");

  useEffect(() => {
    authClient.me()
      .then((user) => setAccess(user.roles.includes("ADMIN") ? "allowed" : "denied"))
      .catch(() => setAccess("denied"));
  }, []);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setMessage(null);
    try {
      const csrf = await getCatalogCsrf();
      await createCatalogProduct({ name, slug, status: "DRAFT" }, csrf.token);
      setMessage("Draft product created.");
      setName("");
      setSlug("");
    } catch {
      setMessage("Admin access or catalog-service is unavailable.");
    } finally {
      setSaving(false);
    }
  }

  return <main className="shell storefront"><header className="topbar"><Link className="brand" href="/">NovaCommerce</Link><Link className="health-link" href="/products">Store</Link></header><section className="store-heading"><p className="eyebrow">Admin surface</p><h1>Catalog operations</h1><p className="intro">Create a draft product for the Catalog Service. Authentication and activation rules remain server-enforced.</p></section>{access === "checking" && <p role="status" className="form-copy">Checking admin access…</p>}{access === "denied" && <p role="alert" className="form-error">Sign in with an ADMIN account to manage the catalog.</p>}{access === "allowed" && <form className="auth-card admin-form" onSubmit={submit}><label>Product name<input required value={name} onChange={(event) => setName(event.target.value)} /></label><label>Slug<input required pattern="[a-z0-9-]+" value={slug} onChange={(event) => setSlug(event.target.value)} /></label><button disabled={saving}>{saving ? "Creating…" : "Create draft"}</button>{message && <p role="status" className="form-copy">{message}</p>}</form>}</main>;
}
