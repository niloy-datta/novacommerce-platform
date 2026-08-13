"use client";
import { use, useEffect, useState } from "react";
import { orderClient, type Order } from "@/lib/order-client";

export default function OrderPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [order, setOrder] = useState<Order | null>(null);
  const [message, setMessage] = useState("");
  useEffect(() => { orderClient.order(id).then(setOrder).catch(() => setMessage("Order unavailable.")); }, [id]);
  async function cancel() { setMessage("Releasing inventory…"); try { setOrder(await orderClient.cancel(id)); setMessage("Order cancelled."); } catch { setMessage("Cancellation is not confirmed. Retry safely."); } }
  if (!order) return <main className="shell diagnostic"><h1>Loading order…</h1><p role="status">{message}</p></main>;
  return <main className="shell storefront"><p className="eyebrow">Order {order.id}</p><h1>{order.status.replaceAll("_", " ")}</h1>{order.items.map(item => <div className="variant-row" key={item.variantId}><span>{item.productName} · {item.variantName}<small>Quantity {item.quantity} at {item.unitPrice} {item.currency}</small></span><strong>{item.lineTotal} {item.currency}</strong></div>)}<h2>Total: {order.merchandiseTotal} {order.currency}</h2>{order.status === "AWAITING_PAYMENT" && <><p className="form-copy">Inventory is reserved. Payment is not implemented in Phase 4.</p><button type="button" onClick={cancel}>Cancel order</button></>}{message && <p role="status" className="form-copy">{message}</p>}</main>;
}
