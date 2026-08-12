"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { Brand, Category, ProductPage, getBrands, getCategories, getProducts } from "@/lib/catalog-client";

export default function ProductDiscovery() {
  const router = useRouter();
  const search = useSearchParams();
  const queryString = search.toString();
  const [result, setResult] = useState<{ query: string | null; data: ProductPage | null; error: string | null }>({ query: null, data: null, error: null });
  const [categories, setCategories] = useState<Category[]>([]);
  const [brands, setBrands] = useState<Brand[]>([]);
  const loading = result.query !== queryString;
  const data = loading ? null : result.data;
  const error = loading ? null : result.error;

  useEffect(() => {
    Promise.all([getCategories(), getBrands()])
      .then(([categoryItems, brandItems]) => { setCategories(categoryItems); setBrands(brandItems); })
      .catch(() => undefined);
  }, []);
  useEffect(() => {
    let cancelled = false;
    getProducts(new URLSearchParams(queryString))
      .then((productPage) => { if (!cancelled) setResult({ query: queryString, data: productPage, error: null }); })
      .catch(() => { if (!cancelled) setResult({ query: queryString, data: null, error: "Catalog is unavailable. Start catalog-service and try again." }); });
    return () => { cancelled = true; };
  }, [queryString]);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const params = new URLSearchParams();
    for (const key of ["q", "category", "brand", "minPrice", "maxPrice", "sort"]) {
      const value = String(form.get(key) ?? "").trim();
      if (value) params.set(key, value);
    }
    params.set("page", "0");
    params.set("size", search.get("size") ?? "12");
    router.push(`/products?${params.toString()}`);
  }
  function changePage(pageNumber: number) {
    const params = new URLSearchParams(search.toString());
    params.set("page", String(pageNumber));
    router.push(`/products?${params.toString()}`);
  }

  return <main className="shell storefront"><header className="topbar"><Link className="brand" href="/">NovaCommerce</Link><nav><Link className="health-link" href="/">Platform</Link><span aria-hidden="true"> · </span><Link className="health-link" href="/health">Health</Link></nav></header><section className="store-heading"><p className="eyebrow">Storefront foundation</p><h1>Product discovery</h1><p className="intro">A real catalog surface backed by the Catalog Service. Inventory and checkout arrive in later phases.</p></section><form className="filters" key={search.toString()} onSubmit={submit}><label>Search<input name="q" defaultValue={search.get("q") ?? ""} placeholder="Search products" /></label><label>Category<select name="category" defaultValue={search.get("category") ?? ""}><option value="">All categories</option>{categories.map((item) => <option key={item.id} value={item.slug}>{item.name}</option>)}</select></label><label>Brand<select name="brand" defaultValue={search.get("brand") ?? ""}><option value="">All brands</option>{brands.map((item) => <option key={item.id} value={item.slug}>{item.name}</option>)}</select></label><label>Minimum price<input name="minPrice" type="number" min="0" step="0.01" defaultValue={search.get("minPrice") ?? ""} /></label><label>Maximum price<input name="maxPrice" type="number" min="0" step="0.01" defaultValue={search.get("maxPrice") ?? ""} /></label><label>Sort<select name="sort" defaultValue={search.get("sort") ?? "newest"}><option value="newest">Newest</option><option value="relevance">Relevance</option><option value="price_asc">Price: low to high</option><option value="price_desc">Price: high to low</option><option value="name_asc">Name: A to Z</option><option value="name_desc">Name: Z to A</option></select></label><button type="submit">Apply filters</button></form><section aria-busy={loading}>{loading && <p role="status" className="form-copy">Loading catalog…</p>}{error && <p role="alert" className="form-error">{error}</p>}{!loading && !error && data?.items.length === 0 && <p className="form-copy">No products match these filters.</p>}<div className="product-grid">{data?.items.map((product) => <Link className="product-card" href={`/products/${product.slug}`} key={product.id}><span className="card-status">{product.brand?.name ?? "NovaCommerce catalog"}</span><h2>{product.name}</h2><p>{product.shortDescription ?? "Product details available in the catalog."}</p>{product.startingPrice && <strong>{product.startingPrice.amount} {product.startingPrice.currency}</strong>}</Link>)}</div></section>{data && data.totalPages > 1 && <nav className="pagination" aria-label="Catalog pages"><button type="button" disabled={data.page === 0} onClick={() => changePage(data.page - 1)}>Previous</button><span>Page {data.page + 1} of {data.totalPages}</span><button type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => changePage(data.page + 1)}>Next</button></nav>}</main>;
}
