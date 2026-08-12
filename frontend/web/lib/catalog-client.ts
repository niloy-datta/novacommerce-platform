export type Price = { amount: string; currency: string };
export type Brand = { id: string; name: string; slug: string; description?: string | null };
export type Category = { id: string; name: string; slug: string; description?: string | null; parentSlug?: string | null };
export type Variant = { id: string; sku: string; name: string; attributes: Record<string, string>; price: Price; active: boolean };
export type ProductSummary = { id: string; name: string; slug: string; shortDescription?: string | null; brand?: Brand | null; startingPrice?: Price | null };
export type Product = ProductSummary & { description?: string | null; status: string; categories: Category[]; variants: Variant[]; images: { id: string; url: string; altText?: string | null; sortOrder: number; variantId?: string | null }[]; version: number };
export type ProductPage = { items: ProductSummary[]; page: number; size: number; totalElements: number; totalPages: number };

const baseUrl = process.env.NEXT_PUBLIC_CATALOG_API_URL ?? "http://localhost:8082";
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, { ...init, headers: { Accept: "application/json", ...init?.headers }, cache: "no-store" });
  if (!response.ok) throw new Error(`Catalog request failed (${response.status})`);
  return response.json() as Promise<T>;
}

export function getProducts(params: URLSearchParams) { return request<ProductPage>(`/api/v1/products?${params.toString()}`); }
export function getProduct(slug: string) { return request<Product>(`/api/v1/products/${encodeURIComponent(slug)}`); }
export function getCategories() { return request<Category[]>("/api/v1/categories"); }
export function getBrands() { return request<Brand[]>("/api/v1/brands"); }

export async function getCatalogCsrf(): Promise<{ token: string; headerName: string }> {
  return request<{ token: string; headerName: string }>("/api/v1/catalog/csrf", { credentials: "include" });
}
export async function createCatalogProduct(payload: unknown, csrf: string) {
  return request<Product>("/api/v1/admin/catalog/products", { method: "POST", credentials: "include", headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": csrf }, body: JSON.stringify(payload) });
}
