import Link from "next/link";

export default function HealthPage() {
  return (
    <main className="shell diagnostic">
      <p className="eyebrow">Diagnostic</p>
      <h1>Frontend available</h1>
      <p className="intro">The web application rendered successfully. Backend service health endpoints are independently available when their services are running.</p>
      <Link className="health-link" href="/">Return to NovaCommerce</Link>
    </main>
  );
}
