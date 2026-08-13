export type Availability = { variantId: string; availability: "AVAILABLE" | "OUT_OF_STOCK"; availableQuantity: number };
const baseUrl = process.env.NEXT_PUBLIC_INVENTORY_API_URL ?? "http://localhost:8083";
export async function getAvailability(variantId: string): Promise<Availability | null> {
  const response = await fetch(`${baseUrl}/api/v1/inventory/variants/${encodeURIComponent(variantId)}`, { cache: "no-store" });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Inventory request failed (${response.status})`);
  return response.json() as Promise<Availability>;
}
