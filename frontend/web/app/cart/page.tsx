"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { orderClient, type Cart } from "@/lib/order-client";

export default function CartPage() {
  const [cart, setCart] = useState<Cart | null>(null);
  const [message, setMessage] = useState("Loading cart…");
  useEffect(() => { orderClient.cart().then(value => { setCart(value); setMessage(""); }).catch(() => setMessage("Sign in to view your cart.")); }, []);
  async function update(variantId: string, quantity: number) { setCart(await orderClient.update(variantId, quantity)); }
  async function remove(variantId: string) { await orderClient.remove(variantId); setCart(await orderClient.cart()); }
  async function checkout() { if (!cart) return; setMessage("Reserving inventory…"); try { const result = await orderClient.checkout(cart.id, crypto.randomUUID()); setMessage(result.retryable ? `Order ${result.orderId} is pending inventory confirmation. Retry safely later.` : `Order ${result.orderId} created. Payment is not implemented yet.`); } catch { setMessage("Checkout could not be completed."); } }
  return <main className="shell storefront"><p className="eyebrow">Customer cart</p><h1>Your cart</h1>{message && <p className="form-copy" role="status">{message}</p>}{cart && <section className="detail-grid"><div>{cart.items.length === 0 ? <p>Your cart is empty.</p> : cart.items.map(item => <div className="variant-row" key={item.variantId}><span>{item.variantId}</span><span><label>Quantity <input aria-label={`Quantity for ${item.variantId}`} type="number" min="1" max="999" value={item.quantity} onChange={event => update(item.variantId, Number(event.target.value))} /></label><button type="button" onClick={() => remove(item.variantId)}>Remove</button></span></div>)}</div><div><p>Status: {cart.status}</p><button disabled={!cart.items.length || cart.status !== "ACTIVE"} onClick={checkout}>Checkout</button><p className="form-copy">Current prices are resolved from Catalog during checkout.</p></div></section>}<Link className="health-link" href="/orders">Order history</Link></main>;
}
