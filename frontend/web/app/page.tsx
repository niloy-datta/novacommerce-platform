import Link from "next/link";

const plannedApplications = [
  { name: "Store", description: "Customer-facing commerce experience", status: "Planned" },
  { name: "Admin", description: "Operations and platform administration", status: "Planned" },
];

export default function HomePage() {
  return (
    <main className="shell">
      <header className="topbar" aria-label="NovaCommerce">
        <span className="brand">NovaCommerce</span>
        <nav><Link href="/login" className="health-link">Sign in</Link><span aria-hidden="true"> · </span><Link href="/health" className="health-link">System health</Link></nav>
      </header>
      <section className="hero" aria-labelledby="page-title">
        <p className="eyebrow">Architecture Foundation</p>
        <h1 id="page-title">Distributed Commerce &amp; Payment Platform</h1>
        <p className="intro">A production-style platform being shaped around reliable transactions, clear service ownership, and an intentionally small starting surface.</p>
        <div className="status" role="status"><span aria-hidden="true" />Project status: Architecture Foundation</div>
      </section>
      <section className="applications" aria-labelledby="applications-title">
        <div><p className="eyebrow">Applications</p><h2 id="applications-title">Product surfaces, planned deliberately.</h2></div>
        <div className="application-grid">
          {plannedApplications.map((application) => (
            <article className="application-card" key={application.name}>
              <p className="card-status">{application.status}</p>
              <h3>{application.name}</h3>
              <p>{application.description}</p>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
