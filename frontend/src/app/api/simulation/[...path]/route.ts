import { NextRequest, NextResponse } from "next/server";

export const dynamic = "force-dynamic";

const BROKER_URL = (process.env.BROKER_URL ?? "http://localhost:8080").replace(/\/$/, "");
const ADMIN_API_KEY = process.env.WARGAME_ADMIN_API_KEY ?? "admin-secret-token";

type RouteContext = { params: { path: string[] } };

async function proxyToBroker(request: NextRequest, { params }: RouteContext) {
  const path = params.path.map(encodeURIComponent).join("/");
  const target = `${BROKER_URL}/api/v1/simulation/${path}${request.nextUrl.search}`;
  const body = request.method === "GET" ? undefined : (await request.text()) || undefined;

  try {
    const response = await fetch(target, {
      method: request.method,
      headers: { "X-API-KEY": ADMIN_API_KEY, "Content-Type": "application/json" },
      body,
      cache: "no-store",
    });
    return new NextResponse(await response.text(), {
      status: response.status,
      headers: { "Content-Type": response.headers.get("Content-Type") ?? "application/json" },
    });
  } catch {
    return NextResponse.json({ message: `Брокер недоступний за адресою ${BROKER_URL}` }, { status: 502 });
  }
}

export { proxyToBroker as GET, proxyToBroker as POST };
