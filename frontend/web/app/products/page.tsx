import { Suspense } from "react";
import ProductDiscovery from "./product-discovery";

export const metadata = { title: "Store | NovaCommerce" };
export default function ProductsPage() { return <Suspense fallback={<main className="shell"><p className="intro">Loading catalog…</p></main>}><ProductDiscovery /></Suspense>; }
