"use server";
import { ApiOptions, ApiResponse, LOBBY_ERRORS } from "../types";

const BASE_URL = process.env.BACKEND_INTERNAL_URL || process.env.NEXT_PUBLIC_BACKEND_URL || "";

export async function apiFetch<T = unknown>(
  endpoint: string,
  options: ApiOptions = {}
): Promise<ApiResponse<T>> {
  try {
    const { body, headers, ...restOptions } = options;

    const res = await fetch(`${BASE_URL}${endpoint}`, {
      headers: {
        "Content-Type": "application/json",
        ...headers,
      },
      ...(body ? { body: JSON.stringify(body) } : {}),
      ...restOptions,
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      return {
        error: errorData.code || LOBBY_ERRORS.SERVER_ERROR,
        status: res.status,
      };
    }

    const data: T = await res.json().catch(() => ({}));
    return { data, status: res.status };
  } catch (err) {
    console.error(`API Fetch error on [${endpoint}]:`, err);
    return { error: LOBBY_ERRORS.CONNECTION_ERROR };
  }
}