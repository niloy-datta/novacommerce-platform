"use client";

import { useState } from "react";
import Link from "next/link";
import { orderClient } from "@/lib/order-client";

export default function AddToCart({ variantId, disabled }: { variantId: string; disabled: boolean }) {
  const [message, setMessage] = useState("");
  async function add() {
    setMessage("Adding…");
    try {
      await orderClient.add(variantId, 1);
      setMessage("Added to cart.");
    } catch {
      setMessage("Sign in to add this item.");
    }
  }
  return <span><button type="button" disabled={disabled} onClick={add}>Add to cart</button>{message && <small role="status">{message} {message === "Added to cart." && <Link href="/cart">View cart</Link>}</small>}</span>;
}
