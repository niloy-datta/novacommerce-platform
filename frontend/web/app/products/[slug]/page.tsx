import Link from "next/link";
import { getProduct, Product } from "@/lib/catalog-client";

export const dynamic = "force-dynamic";

export default async function ProductPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const product: Product | null = await getProduct(slug).catch(() => null);
  if (!product) {
    return <main className="shell diagnostic"><h1>Product unavailable</h1><p className="intro">This product is not available right now.</p><Link className="health-link" href="/products">Return to catalog</Link></main>;
  }
  return <main className="shell storefront"><Link className="health-link" href="/products">← Back to catalog</Link><article className="product-detail"><p className="eyebrow">{product.brand?.name ?? "Catalog product"}</p><h1>{product.name}</h1><p className="intro">{product.description ?? product.shortDescription ?? "Product description will be added by the catalog team."}</p><div className="detail-grid"><section><h2>Variants</h2>{product.variants.map((variant) => <div className="variant-row" key={variant.id}><span>{variant.name}</span><strong>{variant.price.amount} {variant.price.currency}</strong></div>)}</section><section><h2>Categories</h2><p className="form-copy">{product.categories.map((category) => category.name).join(" · ") || "Uncategorized"}</p></section></div></article></main>;
}
