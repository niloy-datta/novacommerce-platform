"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { orderClient, type Order } from "@/lib/order-client";

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [message, setMessage] = useState("Loading orders…");
  useEffect(() => {
    orderClient.orders().then(page => {
      setOrders(page.items);
      setMessage(page.items.length ? "" : "No orders yet.");
    }).catch(() => setMessage("Sign in to view orders."));
  }, []);
  return <main className="shell storefront"><p className="eyebrow">Order history</p><h1>Your orders</h1><p className="form-copy" role="status">{message}</p><div className="product-grid">{orders.map(order => <Link className="product-card" href={`/orders/${order.id}`} key={order.id}><h2>{order.status.replaceAll("_", " ")}</h2><p>{order.items.length} line item(s)</p><strong>{order.merchandiseTotal} {order.currency}</strong></Link>)}</div></main>;
}
